package com.xiaoxiao0301.amberplay.feature.lyrics

import app.cash.turbine.test
import com.xiaoxiao0301.amberplay.core.datastore.AppSettings
import com.xiaoxiao0301.amberplay.core.datastore.LyricMode
import com.xiaoxiao0301.amberplay.core.datastore.SettingsDataStore
import com.xiaoxiao0301.amberplay.core.media.IPlayerController
import com.xiaoxiao0301.amberplay.core.media.PlaybackState
import com.xiaoxiao0301.amberplay.domain.model.Lyric
import com.xiaoxiao0301.amberplay.domain.model.LyricLine
import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.usecase.GetLyricUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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
class LyricsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val playerController = mockk<IPlayerController>(relaxed = true)
    private val getLyric         = mockk<GetLyricUseCase>()
    private val settingsDs       = mockk<SettingsDataStore>()

    private val playbackFlow = MutableStateFlow(PlaybackState())

    private val testSong = Song(
        id = "netease:1", trackId = "1", source = "netease", name = "Test",
        artists = listOf("A"), album = "B", picId = "", lyricId = "l1",
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { playerController.state } returns playbackFlow
        every { settingsDs.settings } returns flowOf(AppSettings(lyricMode = LyricMode.ORIGINAL))
    }

    @AfterEach
    fun teardown() { Dispatchers.resetMain() }

    private fun createViewModel() = LyricsViewModel(playerController, getLyric, settingsDs)

    private fun lines(vararg tsMs: Long) = tsMs.mapIndexed { i, ts ->
        LyricLine(timestampMs = ts, text = "Line $i")
    }

    // ─── currentLineIndex ──────────────────────────────────────────

    @Test
    fun `currentLineIndex is 0 when lyrics not yet loaded`() = runTest {
        coEvery { getLyric(any()) } returns Result.success(Lyric(lines(0L, 5000L, 10000L)))
        val vm = createViewModel()

        // No song playing yet → index stays 0
        assertEquals(0, vm.currentLineIndex.value)
    }

    @Test
    fun `currentLineIndex tracks position after lyrics load`() = runTest {
        val lyricsLines = lines(0L, 5_000L, 10_000L, 15_000L)
        coEvery { getLyric(any()) } returns Result.success(Lyric(lyricsLines))

        val vm = createViewModel()

        // Trigger lyrics load by setting a song in playback
        playbackFlow.value = PlaybackState(currentSong = testSong, positionMs = 0L)
        // Give coroutines time to process
        testDispatcher.scheduler.advanceUntilIdle()

        // At 0 ms → first line (index 0)
        playbackFlow.value = playbackFlow.value.copy(positionMs = 0L)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, vm.currentLineIndex.value)

        // At 6s → second line (index 1: ts=5000 ≤ 6000)
        playbackFlow.value = playbackFlow.value.copy(positionMs = 6_000L)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, vm.currentLineIndex.value)

        // At 12s → third line (index 2: ts=10000 ≤ 12000)
        playbackFlow.value = playbackFlow.value.copy(positionMs = 12_000L)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, vm.currentLineIndex.value)
    }

    @Test
    fun `currentLineIndex updates reactively when lyrics become ready`() = runTest {
        // Start with no lyrics
        coEvery { getLyric(any()) } returns Result.success(Lyric(emptyList()))
        val vm = createViewModel()

        playbackFlow.value = PlaybackState(currentSong = testSong, positionMs = 8_000L)
        testDispatcher.scheduler.advanceUntilIdle()

        // Index stays 0 for empty lyrics
        assertEquals(0, vm.currentLineIndex.value)
    }

    // ─── seekToLine ────────────────────────────────────────────────

    @Test
    fun `seekToLine delegates to playerController`() {
        coEvery { getLyric(any()) } returns Result.success(Lyric(emptyList()))
        val vm = createViewModel()

        vm.seekToLine(7_500L)

        io.mockk.verify { playerController.seekTo(7_500L) }
    }

    // ─── NoLyric fallback ──────────────────────────────────────────

    @Test
    fun `uiState becomes NoLyric when getLyric returns empty lines`() = runTest {
        coEvery { getLyric(any()) } returns Result.success(Lyric(emptyList()))
        val vm = createViewModel()

        playbackFlow.value = PlaybackState(currentSong = testSong, positionMs = 0L)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assert(state is LyricsUiState.NoLyric || state is LyricsUiState.Loading)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
