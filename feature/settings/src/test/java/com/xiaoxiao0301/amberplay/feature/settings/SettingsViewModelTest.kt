package com.xiaoxiao0301.amberplay.feature.settings

import com.xiaoxiao0301.amberplay.core.cache.AudioCache
import com.xiaoxiao0301.amberplay.core.datastore.AppSettings
import com.xiaoxiao0301.amberplay.core.datastore.SettingsDataStore
import com.xiaoxiao0301.amberplay.core.network.ratelimit.RateLimiter
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val settingsDs  = mockk<SettingsDataStore>(relaxed = true)
    private val audioCache  = mockk<AudioCache>(relaxed = true)
    private val rateLimiter = mockk<RateLimiter>(relaxed = true)

    private lateinit var viewModel: SettingsViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { settingsDs.settings } returns flowOf(AppSettings())
        every { audioCache.usedBytesFlow } returns MutableStateFlow(0L)
        every { rateLimiter.remainingTokens } returns MutableStateFlow(10)

        viewModel = SettingsViewModel(
            settingsDs  = settingsDs,
            audioCache  = audioCache,
            rateLimiter = rateLimiter,
        )
    }

    @AfterEach
    fun teardown() { Dispatchers.resetMain() }

    @Test
    fun `setCacheLimitMb updates DataStore and calls audioCache updateLimit`() = runTest {
        viewModel.setCacheLimitMb(512)
        coVerify { settingsDs.setCacheLimitMb(512) }
        verify { audioCache.updateLimit(512) }
    }

    @Test
    fun `clearCache delegates to audioCache`() = runTest {
        viewModel.clearCache()
        verify { audioCache.clear() }
    }

    @Test
    fun `cacheUsedMb exposes audioCache usedBytesFlow in MB`() = runTest {
        val usedBytesFlow = MutableStateFlow(10L * 1024 * 1024) // 10MB
        every { audioCache.usedBytesFlow } returns usedBytesFlow
        viewModel = SettingsViewModel(settingsDs, audioCache, rateLimiter)
        assertEquals(10L, viewModel.cacheUsedMb.first())
    }

    @Test
    fun `setBitrate delegates to settingsDs`() = runTest {
        viewModel.setBitrate(320)
        coVerify { settingsDs.setBitrate(320) }
    }

    @Test
    fun `setOfflineMode delegates to settingsDs`() = runTest {
        viewModel.setOfflineMode(true)
        coVerify { settingsDs.setOfflineMode(true) }
    }
}
