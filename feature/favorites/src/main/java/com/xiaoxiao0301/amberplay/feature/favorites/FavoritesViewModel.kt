package com.xiaoxiao0301.amberplay.feature.favorites

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoxiao0301.amberplay.core.datastore.SettingsDataStore
import com.xiaoxiao0301.amberplay.core.media.PlayerController
import com.xiaoxiao0301.amberplay.core.media.PlayerService
import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.repository.FavoriteRepository
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
class FavoritesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val favoriteRepo:     FavoriteRepository,
    private val playerController: PlayerController,
    private val getSongUrl:       GetSongUrlUseCase,
    private val settingsDs:       SettingsDataStore,
) : ViewModel() {

    val favorites: StateFlow<List<Song>> = favoriteRepo.getFavorites()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun playSong(song: Song) {
        viewModelScope.launch {
            val br = settingsDs.settings.first().preferredBitrate
            getSongUrl(song, br).onSuccess { songUrl ->
                if (songUrl.url.isNotBlank()) {
                    startPlayerService()
                    playerController.playSong(song, songUrl.url)
                }
            }
        }
    }

    fun removeFavorite(song: Song) {
        viewModelScope.launch { favoriteRepo.removeFavorite(song.id) }
    }

    private fun startPlayerService() {
        context.startForegroundService(
            Intent(context, PlayerService::class.java))
    }
}
