package com.xiaoxiao0301.amberplay.feature.player

import android.content.Context
import app.cash.turbine.test
import com.xiaoxiao0301.amberplay.core.cache.AudioCache
import com.xiaoxiao0301.amberplay.core.common.network.NetworkMonitor
import com.xiaoxiao0301.amberplay.core.datastore.AppSettings
import com.xiaoxiao0301.amberplay.core.datastore.SettingsDataStore
import com.xiaoxiao0301.amberplay.core.media.IFullPlayerController
import com.xiaoxiao0301.amberplay.core.media.PlayMode
import com.xiaoxiao0301.amberplay.core.media.PlaybackState
import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.model.SongUrl
import com.xiaoxiao0301.amberplay.domain.repository.HistoryRepository
import com.xiaoxiao0301.amberplay.domain.repository.QueueRepository
import com.xiaoxiao0301.amberplay.domain.usecase.GetSongUrlUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val context          = mockk<Context>(relaxed = true)
    private val playerController = mockk<IFullPlayerController>(relaxed = true)
    private val getSongUrl       = mockk<GetSongUrlUseCase>()
    private val queueRepo        = mockk<QueueRepository>(relaxed = true)
    private val settingsDs       = mockk<SettingsDataStore>()
    private val historyRepo      = mockk<HistoryRepository>(relaxed = true)
    private val audioCache       = mockk<AudioCache>(relaxed = true)
    private val networkMonitor   = mockk<NetworkMonitor>()

    private lateinit var viewModel: PlayerViewModel

    private val defaultSettings = AppSettings(preferredBitrate = 320, crossfadeMs = 0)

    private fun song(id: String = "s1") = Song(
        id = id, trackId = id, source = "netease", name = "Song $id",
        artists = listOf("Artist"), album = "Album", picId = "", lyricId = "",
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { playerController.state } returns MutableStateFlow(PlaybackState())
        every { queueRepo.getQueue() } returns flowOf(emptyList())
        every { settingsDs.settings } returns flowOf(defaultSettings)
    }

    @AfterEach
    fun teardown() { Dispatchers.resetMain() }

    private fun createViewModel() = PlayerViewModel(
        context          = context,
        playerController = playerController,
        getSongUrl       = getSongUrl,
        queueRepo        = queueRepo,
        settingsDs       = settingsDs,
        historyRepo      = historyRepo,
        audioCache       = audioCache,
        networkMonitor   = networkMonitor,
    )

    @Test
    fun `playSong uses cached file when available`() = runTest {
        val cachedFile = mockk<File>(relaxed = true)
        every { cachedFile.absolutePath } returns "/cache/s1.mp3"
        every { audioCache.getFile(any(), any(), any()) } returns cachedFile

        // Signal that playSong() was called on the controller
        val playSignal = CompletableDeferred<String>()
        every { playerController.playSong(any(), any()) } answers { playSignal.complete(secondArg()) }

        viewModel = createViewModel()
        viewModel.playSong(song())

        // Wait until the IO coroutine completes and playSong is invoked
        val playedUrl = playSignal.await()
        assertTrue(playedUrl.startsWith("file://"))
        coVerify(exactly = 0) { getSongUrl(any(), any()) }
    }

    @Test
    fun `playSong emits error when offline and no cache`() = runTest {
        every { audioCache.getFile(any(), any(), any()) } returns null
        every { networkMonitor.isOnline } returns flowOf(false)

        viewModel = createViewModel()
        viewModel.playerError.test {
            viewModel.playSong(song())
            val error = awaitItem()
            assertEquals("离线模式：该歌曲尚未缓存，无法播放", error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `playSong fetches url and plays when online and no cache`() = runTest {
        every { audioCache.getFile(any(), any(), any()) } returns null
        every { networkMonitor.isOnline } returns flowOf(true)
        val songUrl = SongUrl(url = "https://example.com/song.mp3", bitrate = 320, sizeKb = 5000)
        coEvery { getSongUrl(any(), any()) } returns Result.success(songUrl)

        val playSignal = CompletableDeferred<String>()
        every { playerController.playSong(any(), any()) } answers { playSignal.complete(secondArg()) }

        viewModel = createViewModel()
        viewModel.playSong(song())

        val playedUrl = playSignal.await()
        assertEquals("https://example.com/song.mp3", playedUrl)
    }

    @Test
    fun `playSong emits error when url is blank`() = runTest {
        every { audioCache.getFile(any(), any(), any()) } returns null
        every { networkMonitor.isOnline } returns flowOf(true)
        val emptySongUrl = SongUrl(url = "", bitrate = 0, sizeKb = 0)
        coEvery { getSongUrl(any(), any()) } returns Result.success(emptySongUrl)

        viewModel = createViewModel()
        viewModel.playerError.test {
            viewModel.playSong(song())
            val error = awaitItem()
            assertEquals("获取播放链接失败，请稍后重试", error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `playSong emits error on getSongUrl failure`() = runTest {
        every { audioCache.getFile(any(), any(), any()) } returns null
        every { networkMonitor.isOnline } returns flowOf(true)
        coEvery { getSongUrl(any(), any()) } returns Result.failure(Exception("timeout"))

        viewModel = createViewModel()
        viewModel.playerError.test {
            viewModel.playSong(song())
            val error = awaitItem()
            assert(error.contains("播放失败"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Queue navigation tests ────────────────────────────────────────────────

    private fun setupForNavigation(
        queue: List<Song>,
        currentIndex: Int,
        mode: PlayMode = PlayMode.REPEAT_ALL,
    ): CompletableDeferred<Song> {
        val playSignal = CompletableDeferred<Song>()
        every { audioCache.getFile(any(), any(), any()) } returns null
        every { networkMonitor.isOnline } returns flowOf(true)
        coEvery { getSongUrl(any(), any()) } answers { Result.success(SongUrl(url = "https://cdn/song.mp3", bitrate = 320, sizeKb = 0)) }
        coEvery { queueRepo.getQueueSnapshot() } returns queue
        every { playerController.state } returns MutableStateFlow(
            PlaybackState(currentIndex = currentIndex, queueSize = queue.size, playMode = mode)
        )
        every { playerController.playSong(any(), any()) } answers {
            playSignal.complete(firstArg())
        }
        return playSignal
    }

    @Test
    fun `navigateQueue +1 plays next song in REPEAT_ALL mode`() = runTest {
        val queue = listOf(song("s0"), song("s1"), song("s2"))
        val playSignal = setupForNavigation(queue, currentIndex = 0)
        viewModel = createViewModel()
        viewModel.skipNext()
        val playedSong = playSignal.await()
        assertEquals("s1", playedSong.id)
    }

    @Test
    fun `navigateQueue +1 wraps around in REPEAT_ALL mode`() = runTest {
        val queue = listOf(song("s0"), song("s1"), song("s2"))
        val playSignal = setupForNavigation(queue, currentIndex = 2, mode = PlayMode.REPEAT_ALL)
        viewModel = createViewModel()
        viewModel.skipNext()
        val playedSong = playSignal.await()
        assertEquals("s0", playedSong.id)
    }

    @Test
    fun `navigateQueue +1 stops at last song in SEQUENTIAL mode`() = runTest {
        val queue = listOf(song("s0"), song("s1"))
        every { audioCache.getFile(any(), any(), any()) } returns null
        every { networkMonitor.isOnline } returns flowOf(true)
        coEvery { queueRepo.getQueueSnapshot() } returns queue
        every { playerController.state } returns MutableStateFlow(
            PlaybackState(currentIndex = 1, queueSize = 2, playMode = PlayMode.SEQUENTIAL)
        )

        viewModel = createViewModel()
        viewModel.skipNext() // should be a no-op
        // playSong must NOT be called since we're at the last track in SEQUENTIAL
        coVerify(exactly = 0) { playerController.playSong(any(), any()) }
    }

    @Test
    fun `onSkipToIndex callback navigates to correct index`() = runTest {
        val queue = listOf(song("s0"), song("s1"), song("s2"))
        val callbackSlot = io.mockk.slot<(Int) -> Unit>()
        every { playerController.onSkipToIndex = capture(callbackSlot) } just runs
        every { playerController.onPlaybackEnded = any() } just runs
        every { audioCache.getFile(any(), any(), any()) } returns null
        every { networkMonitor.isOnline } returns flowOf(true)
        coEvery { getSongUrl(any(), any()) } returns Result.success(SongUrl(url = "https://cdn/s2.mp3", bitrate = 320, sizeKb = 0))
        coEvery { queueRepo.getQueueSnapshot() } returns queue
        every { playerController.state } returns MutableStateFlow(
            PlaybackState(currentIndex = 0, queueSize = 3, playMode = PlayMode.REPEAT_ALL)
        )
        val playSignal = CompletableDeferred<Song>()
        every { playerController.playSong(any(), any()) } answers { playSignal.complete(firstArg()) }

        viewModel = createViewModel()
        // The callback gets registered in init{}
        callbackSlot.captured.invoke(2)
        val playedSong = playSignal.await()
        assertEquals("s2", playedSong.id)
    }
}
