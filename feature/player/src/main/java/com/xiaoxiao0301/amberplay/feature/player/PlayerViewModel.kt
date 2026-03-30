package com.xiaoxiao0301.amberplay.feature.player

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoxiao0301.amberplay.core.cache.AudioCache
import com.xiaoxiao0301.amberplay.core.common.network.NetworkMonitor
import com.xiaoxiao0301.amberplay.core.datastore.SettingsDataStore
import com.xiaoxiao0301.amberplay.core.media.PlayMode
import com.xiaoxiao0301.amberplay.core.media.PlaybackState
import com.xiaoxiao0301.amberplay.core.media.PlayerController
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
    val playerController: PlayerController,
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

    fun playSong(song: Song) {
        viewModelScope.launch {
            val settings = settingsDs.settings.first()
            playerController.crossfadeMs = settings.crossfadeMs
            val br = settings.preferredBitrate

            // 1. 检查本地缓存
            val cachedFile = withContext(Dispatchers.IO) {
                audioCache.getFile(song.source, song.trackId, br)
            }
            if (cachedFile != null) {
                startAndPlay(song, "file://${cachedFile.absolutePath}")
                return@launch
            }

            // 2. 检查网络
            val isOnline = networkMonitor.isOnline.first()
            if (!isOnline) {
                _playerError.emit("离线模式：该歌曲尚未缓存，无法播放")
                return@launch
            }

            // 3. 在线：获取 URL 并播放；后台异步缓存
            getSongUrl(song, br)
                .onSuccess { songUrl ->
                    if (songUrl.url.isBlank()) {
                        _playerError.emit("获取播放链接失败，请稍后重试")
                        return@onSuccess
                    }
                    startAndPlay(song, songUrl.url)
                    // 后台下载缓存，不阻塞播放
                    launch(Dispatchers.IO) {
                        audioCache.downloadAndCache(song.source, song.trackId, br, songUrl.url)
                    }
                }
                .onFailure {
                    _playerError.emit("播放失败：${it.message}")
                }
        }
    }

    private suspend fun startAndPlay(song: Song, url: String) {
        startPlayerService()
        playerController.playSong(song, url)
        queueRepo.addToEnd(song)
        historyRepo.addPlayRecord(song, 0L)
        // Only register as a "play" in the stats counter after 30 s of continuous playback
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
    fun skipNext()       = playerController.skipToNext()
    fun skipPrevious()   = playerController.skipToPrevious()

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
