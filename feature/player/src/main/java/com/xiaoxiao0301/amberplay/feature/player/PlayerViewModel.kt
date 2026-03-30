package com.xiaoxiao0301.amberplay.feature.player

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoxiao0301.amberplay.core.media.PlayMode
import com.xiaoxiao0301.amberplay.core.media.PlaybackState
import com.xiaoxiao0301.amberplay.core.media.PlayerController
import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.repository.QueueRepository
import com.xiaoxiao0301.amberplay.domain.usecase.GetSongUrlUseCase
import com.xiaoxiao0301.amberplay.core.datastore.SettingsDataStore
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
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playerController.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlaybackState())

    val queue = queueRepo.getQueue()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun playSong(song: Song) {
        viewModelScope.launch {
            val preferredBr = settingsDs.settings.first().preferredBitrate

            getSongUrl(song, preferredBr)
                .onSuccess { songUrl ->
                    if (songUrl.url.isNotBlank()) {
                        startPlayerService()
                        playerController.playSong(song, songUrl.url)
                        queueRepo.addToEnd(song)
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

    private fun startPlayerService() {
        val intent = Intent(context,
            Class.forName("com.xiaoxiao0301.amberplay.core.media.PlayerService"))
        context.startForegroundService(intent)
    }
}
