package com.xiaoxiao0301.amberplay.domain.usecase

import com.xiaoxiao0301.amberplay.domain.model.PlayStat
import com.xiaoxiao0301.amberplay.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** 查询播放次数前 N 名歌曲的统计信息 */
class GetTopStatsUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {
    operator fun invoke(limit: Int = 10): Flow<List<PlayStat>> =
        historyRepository.getTopPlayStats(limit)
}

/** 查询累计总播放次数 */
class GetTotalPlayCountUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {
    operator fun invoke(): Flow<Long> =
        historyRepository.getTotalPlayCount()
}

/** 查询累计总播放时长（毫秒） */
class GetTotalPlayDurationUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {
    operator fun invoke(): Flow<Long> =
        historyRepository.getTotalPlayDurationMs()
}

