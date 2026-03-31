package com.xiaoxiao0301.amberplay.core.media

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.xiaoxiao0301.amberplay.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
) : IPlayerController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val player: ExoPlayer by lazy {
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .build()
            .also { attachListener(it) }
    }

    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    /** Crossfade duration set by the ViewModel from DataStore. 0 = disabled. */
    var crossfadeMs: Int = 0

    /**
     * 队列导航回调，由 PlayerViewModel.init 注册。
     * PlayerController 本身不持有队列状态，所有上/下/跳转逻辑委托给 PlayerViewModel。
     */
    var onSkipToIndex:   ((Int) -> Unit)? = null
    var onPlaybackEnded: (() -> Unit)?    = null

    private val sleepTimer = SleepTimer()
    private var positionJob: Job? = null
    private var crossfadeJob: Job? = null

    // ─── 公开控制方法 ────────────────────────────────────────────

    fun playSong(song: Song, url: String) {
        // SEC-01: Only allow http/https/file schemes to prevent ExoPlayer reading arbitrary URIs
        require(url.startsWith("https://") || url.startsWith("http://") || url.startsWith("file://")) {
            "Invalid playback URL scheme: ${url.substringBefore("://")}"
        }
        crossfadeJob?.cancel()
        val fadeDuration = crossfadeMs
        if (fadeDuration > 0 && player.isPlaying) {
            crossfadeJob = scope.launch {
                // Fade out current
                val steps = 20
                val stepDelayMs = (fadeDuration / 2 / steps).toLong().coerceAtLeast(10L)
                for (i in steps downTo 0) {
                    player.volume = i.toFloat() / steps
                    delay(stepDelayMs)
                }
                // Switch track
                player.setMediaItem(MediaItem.fromUri(url))
                player.prepare()
                player.play()
                _state.value = _state.value.copy(currentSong = song, isPlaying = true)
                startPositionPolling()
                // Fade in
                for (i in 0..steps) {
                    player.volume = i.toFloat() / steps
                    delay(stepDelayMs)
                }
                player.volume = 1.0f
            }
        } else {
            player.volume = 1.0f
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
            player.play()
            _state.value = _state.value.copy(currentSong = song, isPlaying = true)
            startPositionPolling()
        }
    }

    fun playOrPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    override fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _state.value = _state.value.copy(positionMs = positionMs)
    }

    /**
     * 这三个方法不再直接操作 ExoPlayer 多媒体列表（内部始终只有 1 条目）。
     * 上/下/跳转逻辑由 PlayerViewModel 通过回调中心化处理。
     */
    override fun skipToNext()            { onSkipToIndex?.invoke(state.value.currentIndex + 1) }
    override fun skipToPrevious()        { onSkipToIndex?.invoke(state.value.currentIndex - 1) }
    override fun skipToIndex(index: Int) { onSkipToIndex?.invoke(index) }

    /** 由 PlayerViewModel 在 playSong() / navigateToIndex() 时调用，同步队列位置到 PlaybackState */
    fun updateQueueContext(index: Int, size: Int) {
        _state.value = _state.value.copy(currentIndex = index, queueSize = size)
    }

    fun setSpeed(speed: Float) {
        player.setPlaybackSpeed(speed.coerceIn(0.5f, 2.0f))
        _state.value = _state.value.copy(speed = speed)
    }

    fun setPlayMode(mode: PlayMode) {
        when (mode) {
            PlayMode.SEQUENTIAL -> {
                player.repeatMode = Player.REPEAT_MODE_OFF
                player.shuffleModeEnabled = false
            }
            PlayMode.REPEAT_ONE -> {
                player.repeatMode = Player.REPEAT_MODE_ONE
                player.shuffleModeEnabled = false
            }
            PlayMode.REPEAT_ALL -> {
                player.repeatMode = Player.REPEAT_MODE_ALL
                player.shuffleModeEnabled = false
            }
            PlayMode.SHUFFLE -> {
                player.repeatMode = Player.REPEAT_MODE_ALL
                player.shuffleModeEnabled = true
            }
        }
        _state.value = _state.value.copy(playMode = mode)
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimer.set(minutes, scope) { player.pause() }
    }

    fun cancelSleepTimer() = sleepTimer.cancel()

    fun release() {
        positionJob?.cancel()
        crossfadeJob?.cancel()
        sleepTimer.cancel()
        player.release()
    }

    // ─── 内部 ────────────────────────────────────────────────────

    private fun attachListener(exo: ExoPlayer) {
        exo.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
                if (isPlaying) startPositionPolling() else positionJob?.cancel()
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY ->
                        _state.value = _state.value.copy(durationMs = exo.duration.coerceAtLeast(0L))
                    Player.STATE_ENDED ->
                        // REPEAT_ONE 由 ExoPlayer 的 REPEAT_MODE_ONE 自动处理，不需要回调
                        if (_state.value.playMode != PlayMode.REPEAT_ONE) onPlaybackEnded?.invoke()
                }
            }
        })
    }

    /** 每 1000 ms 轮询一次播放进度，仅在进度实际变化时更新 StateFlow，减少无效重组 */
    private fun startPositionPolling() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (true) {
                val newPos = player.currentPosition
                if (newPos != _state.value.positionMs) {
                    _state.value = _state.value.copy(positionMs = newPos)
                }
                delay(1_000L)
            }
        }
    }
}
