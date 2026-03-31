package com.xiaoxiao0301.amberplay.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoxiao0301.amberplay.core.network.ratelimit.RateLimitEvent
import com.xiaoxiao0301.amberplay.core.network.ratelimit.RateLimiter
import com.xiaoxiao0301.amberplay.core.media.IPlayerController
import com.xiaoxiao0301.amberplay.domain.model.Playlist
import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.repository.FavoriteRepository
import com.xiaoxiao0301.amberplay.domain.repository.HistoryRepository
import com.xiaoxiao0301.amberplay.domain.repository.PlaylistRepository
import com.xiaoxiao0301.amberplay.domain.repository.QueueRepository
import com.xiaoxiao0301.amberplay.domain.usecase.SearchMusicUseCase
import com.xiaoxiao0301.amberplay.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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
    private val searchUseCase:   SearchMusicUseCase,
    private val historyRepo:     HistoryRepository,
    private val favoriteRepo:    FavoriteRepository,
    private val toggleFavorite:  ToggleFavoriteUseCase,
    private val rateLimiter:     RateLimiter,
    private val playlistRepo:    PlaylistRepository,
    private val queueRepo:       QueueRepository,
    private val playerController: IPlayerController,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _rateLimitWarning = MutableSharedFlow<Long>(extraBufferCapacity = 4)
    val rateLimitWarning: SharedFlow<Long> = _rateLimitWarning.asSharedFlow()

    val searchHistory = historyRepo.getSearchHistory()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** 已收藏歌曲的 ID 集合，用于 UI 中显示 ❤ 状态 */
    val favoriteIds: StateFlow<Set<String>> = favoriteRepo.getFavorites()
        .map { songs -> songs.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** 所有歌单（用于"加入歌单"弹窗） */
    val playlists: StateFlow<List<Playlist>> = playlistRepo.getAllPlaylists()
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

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            val isFav = song.id in favoriteIds.value
            toggleFavorite(song, isFav)
        }
    }

    /** 将歌曲加入指定歌单 */
    fun addSongToPlaylist(song: Song, playlistId: Int) {
        viewModelScope.launch {
            playlistRepo.addSongsToPlaylist(playlistId, listOf(song))
        }
    }

    /** 将歌曲插入播放队列的下一首 */
    fun playNext(song: Song) {
        val currentIndex = playerController.state.value.currentIndex
        viewModelScope.launch { queueRepo.addAsNext(currentIndex, song) }
    }

    /** 批量收藏 */
    fun addBatchToFavorites(songs: List<Song>) {
        viewModelScope.launch {
            songs.filter { it.id !in favoriteIds.value }
                 .forEach { song -> toggleFavorite(song, false) }
        }
    }

    /** 批量加入歌单 */
    fun addBatchToPlaylist(songs: List<Song>, playlistId: Int) {
        viewModelScope.launch { playlistRepo.addSongsToPlaylist(playlistId, songs) }
    }
}
