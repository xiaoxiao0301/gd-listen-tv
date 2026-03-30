package com.xiaoxiao0301.amberplay.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AlbumUiState {
    object Idle    : AlbumUiState()
    object Loading : AlbumUiState()
    data class Ready(val songs: List<Song>) : AlbumUiState()
    data class Error(val message: String) : AlbumUiState()
}

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val musicRepo: MusicRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlbumUiState>(AlbumUiState.Idle)
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    private var initialized = false

    fun init(source: String, albumId: String) {
        if (initialized) return
        initialized = true
        load(source, albumId)
    }

    private fun load(source: String, albumId: String) {
        _uiState.value = AlbumUiState.Loading
        viewModelScope.launch {
            musicRepo.getAlbumTracks(albumId, source)
                .onSuccess { songs ->
                    _uiState.value = if (songs.isEmpty()) AlbumUiState.Error("专辑无曲目")
                    else AlbumUiState.Ready(songs)
                }
                .onFailure { e ->
                    _uiState.value = AlbumUiState.Error(e.message ?: "加载失败")
                }
        }
    }
}
