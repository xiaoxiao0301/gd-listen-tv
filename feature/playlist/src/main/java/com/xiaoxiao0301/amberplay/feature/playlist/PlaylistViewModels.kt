package com.xiaoxiao0301.amberplay.feature.playlist

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoxiao0301.amberplay.core.datastore.SettingsDataStore
import com.xiaoxiao0301.amberplay.core.media.PlayerController
import com.xiaoxiao0301.amberplay.domain.model.Playlist
import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.repository.PlaylistRepository
import com.xiaoxiao0301.amberplay.domain.usecase.GetSongUrlUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── 歌单列表 ViewModel ───────────────────────────────────────────────────────

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistRepo: PlaylistRepository,
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = playlistRepo.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    fun openCreateDialog()  { _showCreateDialog.value = true  }
    fun closeCreateDialog() { _showCreateDialog.value = false }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            playlistRepo.createPlaylist(name.trim())
            _showCreateDialog.value = false
        }
    }

    fun deletePlaylist(id: Int) {
        viewModelScope.launch { playlistRepo.deletePlaylist(id) }
    }
}

// ─── 歌单详情 ViewModel ───────────────────────────────────────────────────────

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playlistRepo:     PlaylistRepository,
    private val playerController: PlayerController,
    private val getSongUrl:       GetSongUrlUseCase,
    private val settingsDs:       SettingsDataStore,
) : ViewModel() {

    private val _playlistId = MutableStateFlow(0)

    fun init(playlistId: Int) {
        if (_playlistId.value == playlistId) return
        _playlistId.value = playlistId
    }

    val songs: StateFlow<List<Song>> get() {
        val id = _playlistId.value
        return if (id == 0) MutableStateFlow(emptyList())
        else playlistRepo.getPlaylistSongs(id)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    }

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

    fun removeSong(songId: String) {
        val pid = _playlistId.value
        if (pid == 0) return
        viewModelScope.launch { playlistRepo.removeSongFromPlaylist(pid, songId) }
    }

    private fun startPlayerService() {
        context.startForegroundService(
            Intent(context, Class.forName("com.xiaoxiao0301.amberplay.core.media.PlayerService"))
        )
    }
}
