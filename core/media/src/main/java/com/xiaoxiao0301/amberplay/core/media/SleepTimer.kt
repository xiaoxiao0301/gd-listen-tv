package com.xiaoxiao0301.amberplay.core.media

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SleepTimer {
    private var job: Job? = null

    fun set(minutes: Int, scope: CoroutineScope, onTimer: () -> Unit) {
        job?.cancel()
        if (minutes <= 0) return
        job = scope.launch {
            delay(minutes * 60_000L)
            onTimer()
        }
    }

    fun cancel() { job?.cancel(); job = null }

    val isActive: Boolean get() = job?.isActive == true
}
