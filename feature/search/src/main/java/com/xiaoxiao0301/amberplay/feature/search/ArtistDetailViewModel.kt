package com.xiaoxiao0301.amberplay.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.usecase.SearchMusicUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ArtistUiState {
    object Idle    : ArtistUiState()
    object Loading : ArtistUiState()
    data class Ready(val songs: List<Song>) : ArtistUiState()
    data class Error(val message: String) : ArtistUiState()
}

/**
 * ViewModel for browsing songs by a specific artist.
 * Uses the existing [SearchMusicUseCase] with the artist name as the keyword,
 * fetching up to 3 pages to give a broader catalogue view.
 */
@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    private val searchMusic: SearchMusicUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ArtistUiState>(ArtistUiState.Idle)
    val uiState: StateFlow<ArtistUiState> = _uiState.asStateFlow()

    private var initialized = false

    fun init(source: String, artistName: String) {
        if (initialized) return
        initialized = true
        load(source, artistName)
    }

    private fun load(source: String, artistName: String) {
        _uiState.value = ArtistUiState.Loading
        viewModelScope.launch {
            // Fetch first 2 pages to provide a richer result set
            val allSongs = mutableListOf<Song>()
            for (page in 1..2) {
                searchMusic(artistName, source, page)
                    .onSuccess { songs -> allSongs.addAll(songs) }
                    .onFailure {
                        if (allSongs.isEmpty()) {
                            _uiState.value = ArtistUiState.Error(it.message ?: "加载失败")
                            return@launch
                        }
                    }
                if (allSongs.isEmpty() && page == 1) {
                    // First page had no results
                    _uiState.value = ArtistUiState.Error("未找到 $artistName 的歌曲")
                    return@launch
                }
            }
            _uiState.value = ArtistUiState.Ready(allSongs.distinctBy { it.id })
        }
    }
}
