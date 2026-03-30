package com.xiaoxiao0301.amberplay.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val historyRepo: HistoryRepository,
) : ViewModel() {

    /** 最近播放歌曲（去重，最多 20 首） */
    val recentSongs: StateFlow<List<Song>> = historyRepo.getPlayHistory(50)
        .map { records -> records.map { it.song }.distinctBy { it.id }.take(20) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** 搜索历史关键词 */
    val searchHistory: StateFlow<List<String>> = historyRepo.getSearchHistory()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}
