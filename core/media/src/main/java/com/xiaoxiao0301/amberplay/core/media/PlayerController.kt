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
) {
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
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val sleepTimer = SleepTimer()
    private var positionJob: Job? = null

    // ─── 公开控制方法 ────────────────────────────────────────────

    fun playSong(song: Song, url: String) {
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.play()
        _state.value = _state.value.copy(currentSong = song, isPlaying = true)
        startPositionPolling()
    }

    fun playOrPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _state.value = _state.value.copy(positionMs = positionMs)
    }

    fun skipToNext() { player.seekToNextMediaItem() }
    fun skipToPrevious() { player.seekToPreviousMediaItem() }

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
                if (playbackState == Player.STATE_READY) {
                    _state.value = _state.value.copy(durationMs = exo.duration.coerceAtLeast(0L))
                }
            }
        })
    }

    /** 每 500 ms 轮询一次播放进度 */
    private fun startPositionPolling() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (true) {
                _state.value = _state.value.copy(positionMs = player.currentPosition)
                delay(500L)
            }
        }
    }
}
