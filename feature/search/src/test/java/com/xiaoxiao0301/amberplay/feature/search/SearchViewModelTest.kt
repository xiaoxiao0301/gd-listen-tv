package com.xiaoxiao0301.amberplay.feature.search

import app.cash.turbine.test
import com.xiaoxiao0301.amberplay.core.media.PlayerController
import com.xiaoxiao0301.amberplay.core.network.ratelimit.RateLimiter
import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.repository.FavoriteRepository
import com.xiaoxiao0301.amberplay.domain.repository.HistoryRepository
import com.xiaoxiao0301.amberplay.domain.repository.PlaylistRepository
import com.xiaoxiao0301.amberplay.domain.repository.QueueRepository
import com.xiaoxiao0301.amberplay.domain.usecase.SearchMusicUseCase
import com.xiaoxiao0301.amberplay.domain.usecase.ToggleFavoriteUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val searchUseCase    = mockk<SearchMusicUseCase>()
    private val historyRepo      = mockk<HistoryRepository>(relaxed = true)
    private val favoriteRepo     = mockk<FavoriteRepository>()
    private val toggleFavorite   = mockk<ToggleFavoriteUseCase>(relaxed = true)
    private val rateLimiter      = mockk<RateLimiter>(relaxed = true)
    private val playlistRepo     = mockk<PlaylistRepository>()
    private val queueRepo        = mockk<QueueRepository>(relaxed = true)
    private val playerController = mockk<PlayerController>(relaxed = true)

    private lateinit var viewModel: SearchViewModel

    private fun song(id: String) = Song(
        id = id, trackId = id, source = "netease", name = "Song $id",
        artists = listOf("Artist"), album = "Album", picId = "", lyricId = "",
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { rateLimiter.events } returns MutableSharedFlow()
        every { historyRepo.getSearchHistory() } returns flowOf(emptyList())
        every { favoriteRepo.getFavorites() } returns flowOf(emptyList())
        every { playlistRepo.getAllPlaylists() } returns flowOf(emptyList())

        viewModel = SearchViewModel(
            searchUseCase   = searchUseCase,
            historyRepo     = historyRepo,
            favoriteRepo    = favoriteRepo,
            toggleFavorite  = toggleFavorite,
            rateLimiter     = rateLimiter,
            playlistRepo    = playlistRepo,
            queueRepo       = queueRepo,
            playerController = playerController,
        )
    }

    @AfterEach
    fun teardown() { Dispatchers.resetMain() }

    @Test
    fun `initial state is Idle`() = runTest {
        viewModel.uiState.test {
            assertInstanceOf(SearchUiState.Idle::class.java, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search transitions through Loading to Results`() = runTest {
        val songs = listOf(song("s1"), song("s2"))
        coEvery { searchUseCase(any(), any(), any()) } returns Result.success(songs)

        viewModel.uiState.test {
            assertEquals(SearchUiState.Idle, awaitItem())       // initial
            viewModel.search("test")
            assertEquals(SearchUiState.Loading, awaitItem())    // loading kicked off
            val result = awaitItem()
            assertInstanceOf(SearchUiState.Results::class.java, result)
            assertEquals(2, (result as SearchUiState.Results).songs.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search returns Empty when no results`() = runTest {
        coEvery { searchUseCase(any(), any(), any()) } returns Result.success(emptyList())

        viewModel.uiState.test {
            awaitItem() // Idle
            viewModel.search("nothing")
            awaitItem() // Loading
            assertEquals(SearchUiState.Empty, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search returns Error on failure`() = runTest {
        coEvery { searchUseCase(any(), any(), any()) } returns Result.failure(Exception("네트워크 오류"))

        viewModel.uiState.test {
            awaitItem() // Idle
            viewModel.search("error")
            awaitItem() // Loading
            val state = awaitItem()
            assertInstanceOf(SearchUiState.Error::class.java, state)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `blank search does not change state`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Idle
            viewModel.search("   ")
            // No new event should arrive
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearHistory delegates to historyRepo`() = runTest {
        viewModel.clearHistory()
        coVerify { historyRepo.clearSearchHistory() }
    }
}
