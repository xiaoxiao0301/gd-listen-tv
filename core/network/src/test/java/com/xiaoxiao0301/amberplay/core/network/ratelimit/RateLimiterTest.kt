package com.xiaoxiao0301.amberplay.core.network.ratelimit

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RateLimiterTest {

    @Test
    fun `acquire succeeds immediately when tokens are available`() = runTest {
        val limiter = RateLimiter()
        // Should not throw or time-out — token is available
        limiter.acquire()
    }

    @Test
    fun `can acquire up to full capacity without waiting`() = runTest {
        val limiter = RateLimiter()
        repeat(50) { limiter.acquire() }
        assertEquals(0, limiter.remainingTokens.value)
    }

    @Test
    fun `remaining tokens decrease by 1 per acquire`() = runTest {
        val limiter = RateLimiter()
        val before = limiter.remainingTokens.value
        limiter.acquire()
        assertEquals(before - 1, limiter.remainingTokens.value)
    }

    @Test
    fun `emits Waiting event when tokens are exhausted`() = runTest {
        val limiter = RateLimiter()
        // Drain the bucket
        repeat(50) { limiter.acquire() }

        limiter.events.test {
            // Launch a coroutine that will block waiting for a token and emit an event
            val job = launch { limiter.acquire() }
            val event = awaitItem()
            assertTrue(event is RateLimitEvent.Waiting)
            val waitSecs = (event as RateLimitEvent.Waiting).waitSeconds
            assertTrue(waitSecs > 0, "Expected positive wait seconds, got $waitSecs")
            job.cancel()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
