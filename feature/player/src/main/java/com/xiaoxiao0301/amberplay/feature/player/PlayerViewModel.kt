package com.xiaoxiao0301.amberplay.feature.player

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoxiao0301.amberplay.core.datastore.SettingsDataStore
import com.xiaoxiao0301.amberplay.core.media.PlayMode
import com.xiaoxiao0301.amberplay.core.media.PlaybackState
import com.xiaoxiao0301.amberplay.core.media.PlayerController
import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.repository.HistoryRepository
import com.xiaoxiao0301.amberplay.domain.repository.QueueRepository
import com.xiaoxiao0301.amberplay.domain.usecase.GetSongUrlUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val playerController: PlayerController,
    private val getSongUrl:   GetSongUrlUseCase,
    private val queueRepo:    QueueRepository,
    private val settingsDs:   SettingsDataStore,
    private val historyRepo:  HistoryRepository,
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playerController.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlaybackState())

    val queue = queueRepo.getQueue()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun playSong(song: Song) {
        viewModelScope.launch {
            val settings = settingsDs.settings.first()
            playerController.crossfadeMs = settings.crossfadeMs

            getSongUrl(song, settings.preferredBitrate)
                .onSuccess { songUrl ->
                    if (songUrl.url.isNotBlank()) {
                        startPlayerService()
                        playerController.playSong(song, songUrl.url)
                        queueRepo.addToEnd(song)
                        // 记录播放历史 & 更新播放统计
                        historyRepo.addPlayRecord(song, 0L)
                        historyRepo.incrementPlayStat(song)
                    }
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
        val intent = Intent(context,
            Class.forName("com.xiaoxiao0301.amberplay.core.media.PlayerService"))
        context.startForegroundService(intent)
    }
}
