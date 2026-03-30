package com.xiaoxiao0301.amberplay.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoxiao0301.amberplay.core.database.dao.StatsDao
import com.xiaoxiao0301.amberplay.domain.model.PlayStat
import com.xiaoxiao0301.amberplay.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val historyRepo: HistoryRepository,
    private val statsDao:    StatsDao,
) : ViewModel() {

    /** Top 10 最常播放歌曲及其统计 */
    val topSongs: StateFlow<List<PlayStat>> = historyRepo.getTopPlayStats(10)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** 累计播放次数 */
    val totalPlayCount: StateFlow<Long> = statsDao.getTotalPlayCount()
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    /** 累计播放时长（ms） */
    val totalPlayDurationMs: StateFlow<Long> = statsDao.getTotalPlayDurationMs()
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)
}
