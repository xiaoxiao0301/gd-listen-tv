package com.xiaoxiao0301.amberplay.core.network.ratelimit

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

sealed class RateLimitEvent {
    data class Waiting(val waitSeconds: Long) : RateLimitEvent()
}

/**
 * 令牌桶限速器，对应 API 约束：5 分钟内不超过 50 次请求
 *
 * [acquire] 挂起函数，在 MusicRepositoryImpl 中每次 API 调用前调用；
 * 通过 [events] SharedFlow 向 ViewModel 广播等候事件以展示 Snackbar 提示。
 */
@Singleton
class RateLimiter @Inject constructor() {

    private val capacity        = 50
    private val refillIntervalMs = 5 * 60 * 1000L   // 300 000 ms

    private var tokens       = capacity.toDouble()
    private var lastRefill   = System.currentTimeMillis()
    private val mutex        = Mutex()

    private val _events = MutableSharedFlow<RateLimitEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<RateLimitEvent> = _events.asSharedFlow()

    private val _remainingTokens = MutableStateFlow(capacity)
    /** 剩余令牌数（在 mutex 内更新，线程安全） */
    val remainingTokens: StateFlow<Int> = _remainingTokens.asStateFlow()

    /** 消耗一个 token；不足时 suspend 直到补充 */
    suspend fun acquire() {
        while (true) {
            val waitMs: Long
            mutex.withLock {
                refill()
                if (tokens >= 1.0) {
                    tokens -= 1.0
                    _remainingTokens.value = tokens.toInt()
                    return  // 拿到 token，直接返回
                }
                val fillRate  = capacity.toDouble() / refillIntervalMs
                waitMs        = ((1.0 - tokens) / fillRate).toLong() + 100L
            }
            _events.emit(RateLimitEvent.Waiting(waitMs / 1000 + 1))
            delay(waitMs)
        }
    }

    private fun refill() {
        val now     = System.currentTimeMillis()
        val elapsed = now - lastRefill
        tokens      = minOf(capacity.toDouble(), tokens + elapsed.toDouble() * capacity / refillIntervalMs)
        lastRefill  = now
    }
}
