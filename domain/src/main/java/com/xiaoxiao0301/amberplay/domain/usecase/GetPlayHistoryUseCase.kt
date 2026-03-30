package com.xiaoxiao0301.amberplay.domain.usecase

import com.xiaoxiao0301.amberplay.domain.model.PlayRecord
import com.xiaoxiao0301.amberplay.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPlayHistoryUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {
    operator fun invoke(limit: Int = 50): Flow<List<PlayRecord>> =
        historyRepository.getPlayHistory(limit)
}
