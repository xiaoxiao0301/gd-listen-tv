package com.xiaoxiao0301.amberplay.core.media

import kotlinx.coroutines.flow.StateFlow

/**
 * 播放器控制器的最小接口，供 feature 层 ViewModel 注入，
 * 避免直接依赖 [PlayerController] 具体类（Clean Architecture A-02）。
 *
 * [PlayerViewModel] 需要更多实现细节，仍直接注入 [PlayerController]。
 */
interface IPlayerController {
    val state: StateFlow<PlaybackState>
    fun seekTo(positionMs: Long)
    fun skipToIndex(index: Int)
    fun skipToNext()
    fun skipToPrevious()
}
