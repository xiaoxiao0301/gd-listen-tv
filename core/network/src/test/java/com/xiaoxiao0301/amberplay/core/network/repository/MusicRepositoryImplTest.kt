package com.xiaoxiao0301.amberplay.core.network.repository

import com.xiaoxiao0301.amberplay.core.datastore.AppSettings
import com.xiaoxiao0301.amberplay.core.datastore.SettingsDataStore
import com.xiaoxiao0301.amberplay.core.network.api.MusicApiService
import com.xiaoxiao0301.amberplay.core.network.dto.SongUrlDto
import com.xiaoxiao0301.amberplay.domain.model.Song
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MusicRepositoryImplTest {

    private val api          = mockk<MusicApiService>()
    private val settingsDs   = mockk<SettingsDataStore>()
    private lateinit var repo: MusicRepositoryImpl

    private val defaultSettings = AppSettings(
        defaultSource     = "netease",
        multiSourceSearch = false,
        enabledSources    = setOf("netease", "kuwo", "joox"),
        preferredBitrate  = 320,
    )

    private val testSong = Song(
        id       = "netease:111",
        trackId  = "111",
        source   = "netease",
        name     = "Test",
        artists  = listOf("A"),
        album    = "B",
        picId    = "p",
        lyricId  = "l",
    )

    @BeforeEach
    fun setUp() {
        every { settingsDs.settings } returns flowOf(defaultSettings)
        repo = MusicRepositoryImpl(api, settingsDs)
    }

    // ─── getSongUrl fallback rotation ──────────────────────────────

    @Test
    fun `getSongUrl returns url on first source success`() = runTest {
        coEvery { api.getSongUrl(source = "netease", id = "111", br = 320) } returns
                SongUrlDto(url = "https://cdn.netease.com/song.mp3", br = 320, size = 12000)

        val result = repo.getSongUrl(testSong, 320)

        assertTrue(result.isSuccess)
        assertEquals("https://cdn.netease.com/song.mp3", result.getOrNull()!!.url)
        coVerify(exactly = 1) { api.getSongUrl(any(), any(), any(), any()) }
    }

    @Test
    fun `getSongUrl falls back to second source when first returns blank url`() = runTest {
        coEvery { api.getSongUrl(source = "netease", id = "111", br = 320) } returns
                SongUrlDto(url = "", br = 320, size = 0)
        coEvery { api.getSongUrl(source = "kuwo", id = "111", br = 320) } returns
                SongUrlDto(url = "https://cdn.kuwo.com/song.mp3", br = 320, size = 12000)

        val result = repo.getSongUrl(testSong, 320)

        assertTrue(result.isSuccess)
        assertEquals("https://cdn.kuwo.com/song.mp3", result.getOrNull()!!.url)
    }

    @Test
    fun `getSongUrl falls back to third source when first two throw`() = runTest {
        coEvery { api.getSongUrl(source = "netease", id = "111", br = 320) } throws RuntimeException("network error")
        coEvery { api.getSongUrl(source = "kuwo",   id = "111", br = 320) } throws RuntimeException("timeout")
        coEvery { api.getSongUrl(source = "joox",   id = "111", br = 320) } returns
                SongUrlDto(url = "https://cdn.joox.com/song.mp3", br = 320, size = 5000)

        val result = repo.getSongUrl(testSong, 320)

        assertTrue(result.isSuccess)
        assertEquals("https://cdn.joox.com/song.mp3", result.getOrNull()!!.url)
    }

    @Test
    fun `getSongUrl returns failure when all sources throw`() = runTest {
        coEvery { api.getSongUrl(source = "netease", id = "111", br = 320) } throws RuntimeException("err1")
        coEvery { api.getSongUrl(source = "kuwo",   id = "111", br = 320) } throws RuntimeException("err2")
        coEvery { api.getSongUrl(source = "joox",   id = "111", br = 320) } throws RuntimeException("err3")

        val result = repo.getSongUrl(testSong, 320)

        assertTrue(result.isFailure)
    }

    @Test
    fun `getSongUrl tries at most 3 sources`() = runTest {
        // 4 sources enabled but max 3 attempts
        every { settingsDs.settings } returns flowOf(
            defaultSettings.copy(enabledSources = setOf("netease", "kuwo", "joox", "bilibili"))
        )
        coEvery { api.getSongUrl(any(), any(), any(), any()) } returns SongUrlDto("", 0, 0)

        repo.getSongUrl(testSong, 320)

        // Should be called at most 3 times (fallbacks.take(3))
        coVerify(atMost = 3) { api.getSongUrl(any(), any(), any(), any()) }
    }
}
