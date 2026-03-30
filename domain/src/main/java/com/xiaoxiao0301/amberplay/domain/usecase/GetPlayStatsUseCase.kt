package com.xiaoxiao0301.amberplay.domain.usecase

import com.xiaoxiao0301.amberplay.domain.model.PlayStat
import com.xiaoxiao0301.amberplay.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPlayStatsUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {
    fun topStats(limit: Int = 10): Flow<List<PlayStat>> =
        historyRepository.getTopPlayStats(limit)

    fun totalPlayCount(): Flow<Long> =
        historyRepository.getTotalPlayCount()

    fun totalPlayDurationMs(): Flow<Long> =
        historyRepository.getTotalPlayDurationMs()
}
