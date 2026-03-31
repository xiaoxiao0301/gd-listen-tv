package com.xiaoxiao0301.amberplay.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoxiao0301.amberplay.domain.model.PlayStat
import com.xiaoxiao0301.amberplay.domain.usecase.GetTopStatsUseCase
import com.xiaoxiao0301.amberplay.domain.usecase.GetTotalPlayCountUseCase
import com.xiaoxiao0301.amberplay.domain.usecase.GetTotalPlayDurationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val getTopStats:        GetTopStatsUseCase,
    private val getTotalPlayCount:  GetTotalPlayCountUseCase,
    private val getTotalPlayDuration: GetTotalPlayDurationUseCase,
) : ViewModel() {

    /** Top 10 最常播放歌曲及其统计 */
    val topSongs: StateFlow<List<PlayStat>> = getTopStats(10)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** 累计播放次数 */
    val totalPlayCount: StateFlow<Long> = getTotalPlayCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    /** 累计播放时长（ms） */
    val totalPlayDurationMs: StateFlow<Long> = getTotalPlayDuration()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)
}
