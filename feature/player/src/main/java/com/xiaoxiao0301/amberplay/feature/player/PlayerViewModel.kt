package com.xiaoxiao0301.amberplay.feature.player

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoxiao0301.amberplay.core.cache.AudioCache
import com.xiaoxiao0301.amberplay.core.common.network.NetworkMonitor
import com.xiaoxiao0301.amberplay.core.datastore.SettingsDataStore
import com.xiaoxiao0301.amberplay.core.media.IFullPlayerController
import com.xiaoxiao0301.amberplay.core.media.PlayMode
import com.xiaoxiao0301.amberplay.core.media.PlaybackState
import com.xiaoxiao0301.amberplay.core.media.PlayerService
import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.repository.HistoryRepository
import com.xiaoxiao0301.amberplay.domain.repository.QueueRepository
import com.xiaoxiao0301.amberplay.domain.usecase.GetSongUrlUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val playerController: IFullPlayerController,
    private val getSongUrl:     GetSongUrlUseCase,
    private val queueRepo:      QueueRepository,
    private val settingsDs:     SettingsDataStore,
    private val historyRepo:    HistoryRepository,
    private val audioCache:     AudioCache,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playerController.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlaybackState())

    val queue = queueRepo.getQueue()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _playerError = MutableSharedFlow<String>(extraBufferCapacity = 4)
    /** UI 层收听此流以显示 Snackbar 错误提示 */
    val playerError: SharedFlow<String> = _playerError.asSharedFlow()

    // Tracks the delayed 30-second stat-incrementing job for the current song
    private var statsJob: Job? = null

    init {
        // 注册回调：让 QueueViewModel.playAt() 和歌曲自然播放结束均能触发正确的队列导航
        playerController.onSkipToIndex   = { idx -> viewModelScope.launch { navigateToIndex(idx) } }
        playerController.onPlaybackEnded = { viewModelScope.launch { navigateQueue(+1) } }
    }

    fun playSong(song: Song) {
        viewModelScope.launch {
            // 新歌曲添到队列末尾，再读回快照确定新索引
            queueRepo.addToEnd(song)
            val q   = queueRepo.getQueueSnapshot()
            val idx = q.indexOfLast { it.id == song.id }.coerceAtLeast(0)
            fetchAndPlay(song, idx, q.size)
        }
    }

    // ── 队列导航（上/下首、跳转到指定位置） ──────────────────────────────────────

    /** 下一首：直接触发队列导航，不经过 ExoPlayer 多媒体列表 */
    fun skipNext()     = viewModelScope.launch { navigateQueue(+1) }

    /** 上一首：若已播放 > 3 秒则重播当前歌曲；否则跳转到上一首 */
    fun skipPrevious() {
        if (playbackState.value.positionMs > 3_000L) seekTo(0L)
        else viewModelScope.launch { navigateQueue(-1) }
    }

    /**
     * 根据 [delta]（+1 下一首 / -1 上一首）和当前播放模式计算目标索引并播放。
     * 也是歌曲自然结束后的跳转入口（onPlaybackEnded 回调）。
     */
    private suspend fun navigateQueue(delta: Int) {
        val q       = queueRepo.getQueueSnapshot()
        if (q.isEmpty()) return
        val current = playbackState.value.currentIndex
        val mode    = playbackState.value.playMode

        // SEQUENTIAL: 到边界时不循环
        if (mode == PlayMode.SEQUENTIAL) {
            if (delta > 0 && current >= q.size - 1) return  // 已是最后一首，停止
            navigateToIndex((current + delta).coerceIn(0, q.size - 1))
            return
        }
        val next = when (mode) {
            PlayMode.REPEAT_ALL  -> (current + delta).mod(q.size)
            PlayMode.SHUFFLE     -> if (q.size == 1) 0
                                    else { var r: Int; do { r = q.indices.random() } while (r == current); r }
            PlayMode.REPEAT_ONE  -> current  // 应由 ExoPlayer REPEAT_MODE_ONE 处理，正常不会达到此分支
            PlayMode.SEQUENTIAL  -> current
        }
        navigateToIndex(next)
    }

    /**
     * 跳转到队列中的指定索引。
     * 由 [playerController.onSkipToIndex] 回调触发（QueueViewModel.playAt() 的入口）。
     */
    private suspend fun navigateToIndex(index: Int) {
        val q = queueRepo.getQueueSnapshot()
        if (q.isEmpty()) return
        val bounded = index.coerceIn(0, q.size - 1)
        fetchAndPlay(q[bounded], bounded, q.size)
    }

    /**
     * 共用的 URL 获取 + 播放流程：缓存优先 → 网络获取 URL → 开始播放。
     * 已经确定的 [queueIndex]/[queueSize] 直接传入，不再重复读取队列。
     */
    private suspend fun fetchAndPlay(song: Song, queueIndex: Int, queueSize: Int) {
        val settings = settingsDs.settings.first()
        playerController.crossfadeMs = settings.crossfadeMs
        val br = settings.preferredBitrate

        // 1. 检查本地缓存
        val cachedFile = withContext(Dispatchers.IO) {
            audioCache.getFile(song.source, song.trackId, br)
        }
        if (cachedFile != null) {
            startAndPlay(song, "file://${cachedFile.absolutePath}", queueIndex, queueSize)
            return
        }

        // 2. 检查网络
        val isOnline = networkMonitor.isOnline.first()
        if (!isOnline) {
            _playerError.emit("离线模式：该歌曲尚未缓存，无法播放")
            return
        }

        // 3. 在线：获取 URL 并播放；后台异步缓存
        getSongUrl(song, br)
            .onSuccess { songUrl ->
                if (songUrl.url.isBlank()) {
                    _playerError.emit("获取播放链接失败，请稍后重试")
                    return@onSuccess
                }
                startAndPlay(song, songUrl.url, queueIndex, queueSize)
                viewModelScope.launch(Dispatchers.IO) {
                    audioCache.downloadAndCache(song.source, song.trackId, br, songUrl.url)
                }
            }
            .onFailure { _playerError.emit("播放失败：${it.message}") }
    }

    // 记录当前歌曲开始播放的时刻，用于切歌时计算实际已播时长
    private var playStartMs: Long = 0L
    private var playingForRecord: Song? = null

    private suspend fun startAndPlay(song: Song, url: String, queueIndex: Int, queueSize: Int) {
        // 在开始新歌曲前，先把上一首的实际已播时长写入历史表
        val prev = playingForRecord
        if (prev != null && playStartMs > 0L) {
            val elapsed = System.currentTimeMillis() - playStartMs
            historyRepo.addPlayRecord(prev, elapsed.coerceAtLeast(0L))
        }
        playingForRecord = song
        playStartMs = System.currentTimeMillis()

        startPlayerService()
        playerController.updateQueueContext(queueIndex, queueSize)
        playerController.playSong(song, url)
        // 连续播放 30 秒后才计入播放次数统计
        statsJob?.cancel()
        statsJob = viewModelScope.launch {
            delay(30_000L)
            if (playbackState.value.currentSong?.id == song.id) {
                historyRepo.incrementPlayStat(song)
            }
        }
    }

    fun playOrPause()    = playerController.playOrPause()
    fun seekTo(ms: Long) = playerController.seekTo(ms)

    fun cyclePlayMode() {
        val current = playbackState.value.playMode
        val next    = when (current) {
            PlayMode.SEQUENTIAL -> PlayMode.REPEAT_ALL
            PlayMode.REPEAT_ALL -> PlayMode.REPEAT_ONE
            PlayMode.REPEAT_ONE -> PlayMode.SHUFFLE
            PlayMode.SHUFFLE    -> PlayMode.SEQUENTIAL
        }
        playerController.setPlayMode(next)
    }

    /** 播放速度循环切换（0.5 → 0.75 → 1.0 → 1.25 → 1.5 → 2.0 → 0.5） */
    fun cycleSpeed() {
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        val current = playbackState.value.speed
        val next = speeds[(speeds.indexOfFirst { it == current }.takeIf { it >= 0 }
            ?.let { (it + 1) % speeds.size } ?: 1)]
        playerController.setSpeed(next)
        viewModelScope.launch { settingsDs.setPlaybackSpeed(next) }
    }

    /** 设置睡眠定时（0 = 取消） */
    fun setSleepTimer(minutes: Int) {
        if (minutes <= 0) {
            playerController.cancelSleepTimer()
        } else {
            playerController.setSleepTimer(minutes)
        }
        viewModelScope.launch { settingsDs.setSleepTimerMin(minutes) }
    }

    private fun startPlayerService() {
        val intent = Intent(context, PlayerService::class.java)
        context.startForegroundService(intent)
    }
}
