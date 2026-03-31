package com.xiaoxiao0301.amberplay.feature.queue

import app.cash.turbine.test
import com.xiaoxiao0301.amberplay.core.media.IPlayerController
import com.xiaoxiao0301.amberplay.core.media.PlaybackState
import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.repository.QueueRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QueueViewModelTest {

    private val testDispatcher   = UnconfinedTestDispatcher()
    private val queueRepo        = mockk<QueueRepository>(relaxed = true)
    private val playerController = mockk<IPlayerController>(relaxed = true)

    private val playbackFlow = MutableStateFlow(PlaybackState(currentIndex = 2))

    private fun song(n: Int) = Song(
        id = "s$n", trackId = "$n", source = "netease", name = "Song $n",
        artists = listOf("A"), album = "B", picId = "", lyricId = "",
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { playerController.state } returns playbackFlow
        every { queueRepo.getQueue() } returns flowOf(listOf(song(1), song(2), song(3)))
    }

    @AfterEach
    fun teardown() { Dispatchers.resetMain() }

    private fun createViewModel() = QueueViewModel(queueRepo, playerController)

    // ─── playAt ────────────────────────────────────────────────────

    @Test
    fun `playAt delegates skipToIndex to playerController`() {
        val vm = createViewModel()
        vm.playAt(1)
        verify { playerController.skipToIndex(1) }
    }

    @Test
    fun `playAt position 0 works correctly`() {
        val vm = createViewModel()
        vm.playAt(0)
        verify { playerController.skipToIndex(0) }
    }

    // ─── remove ────────────────────────────────────────────────────

    @Test
    fun `remove calls queueRepo remove with correct position`() = runTest {
        val vm = createViewModel()
        vm.remove(1)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { queueRepo.remove(1) }
    }

    // ─── move ──────────────────────────────────────────────────────

    @Test
    fun `move calls queueRepo with from and to positions`() = runTest {
        val vm = createViewModel()
        vm.move(0, 2)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { queueRepo.move(0, 2) }
    }

    // ─── clear ─────────────────────────────────────────────────────

    @Test
    fun `clear calls queueRepo clear`() = runTest {
        val vm = createViewModel()
        vm.clear()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { queueRepo.clear() }
    }

    // ─── queue StateFlow ───────────────────────────────────────────

    @Test
    fun `queue exposes songs from repository`() = runTest {
        val vm = createViewModel()
        vm.queue.test {
            val songs = awaitItem()
            assertEquals(3, songs.size)
            assertEquals("s1", songs[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── playbackState ─────────────────────────────────────────────

    @Test
    fun `playbackState reflects playerController state`() = runTest {
        val vm = createViewModel()
        vm.playbackState.test {
            val state = awaitItem()
            assertEquals(2, state.currentIndex)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
