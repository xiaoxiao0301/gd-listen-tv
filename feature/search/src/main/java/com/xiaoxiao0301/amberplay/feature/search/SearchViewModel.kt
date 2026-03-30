package com.xiaoxiao0301.amberplay.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoxiao0301.amberplay.core.network.ratelimit.RateLimitEvent
import com.xiaoxiao0301.amberplay.core.network.ratelimit.RateLimiter
import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.repository.HistoryRepository
import com.xiaoxiao0301.amberplay.domain.usecase.SearchMusicUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SearchUiState {
    object Idle    : SearchUiState()
    object Loading : SearchUiState()
    object Empty   : SearchUiState()
    data class Results(val songs: List<Song>, val page: Int, val hasMore: Boolean) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchUseCase:  SearchMusicUseCase,
    private val historyRepo:    HistoryRepository,
    private val rateLimiter:    RateLimiter,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _rateLimitWarning = MutableSharedFlow<Long>(extraBufferCapacity = 4)
    val rateLimitWarning: SharedFlow<Long> = _rateLimitWarning.asSharedFlow()

    val searchHistory = historyRepo.getSearchHistory()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var lastKeyword = ""
    private var currentPage = 1

    init {
        // 转发令牌桶等候事件
        viewModelScope.launch {
            rateLimiter.events.collect { event ->
                if (event is RateLimitEvent.Waiting) {
                    _rateLimitWarning.emit(event.waitSeconds)
                }
            }
        }
    }

    fun search(keyword: String, source: String? = null) {
        if (keyword.isBlank()) return
        lastKeyword  = keyword
        currentPage  = 1
        _uiState.value = SearchUiState.Loading
        viewModelScope.launch {
            historyRepo.addSearchHistory(keyword)
            searchUseCase(keyword, source, 1)
                .onSuccess { songs ->
                    _uiState.value = if (songs.isEmpty()) SearchUiState.Empty
                    else SearchUiState.Results(songs, 1, songs.size >= 20)
                }
                .onFailure { e ->
                    _uiState.value = SearchUiState.Error(e.message ?: "搜索失败")
                }
        }
    }

    fun loadNextPage() {
        val current = _uiState.value as? SearchUiState.Results ?: return
        if (!current.hasMore) return
        val nextPage = current.page + 1
        viewModelScope.launch {
            searchUseCase(lastKeyword, null, nextPage)
                .onSuccess { newSongs ->
                    _uiState.value = SearchUiState.Results(
                        songs   = current.songs + newSongs,
                        page    = nextPage,
                        hasMore = newSongs.size >= 20,
                    )
                }
        }
    }

    fun clearHistory() {
        viewModelScope.launch { historyRepo.clearSearchHistory() }
    }
}
