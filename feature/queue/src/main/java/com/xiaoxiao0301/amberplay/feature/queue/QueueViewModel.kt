package com.xiaoxiao0301.amberplay.feature.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoxiao0301.amberplay.core.media.PlaybackState
import com.xiaoxiao0301.amberplay.core.media.PlayerController
import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.repository.QueueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val queueRepo:       QueueRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playerController.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlaybackState())

    val queue: StateFlow<List<Song>> = queueRepo.getQueue()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** 从队列中指定位置播放 */
    fun playAt(position: Int) {
        playerController.skipToIndex(position)
    }

    fun remove(position: Int) {
        viewModelScope.launch { queueRepo.remove(position) }
    }

    fun move(fromPos: Int, toPos: Int) {
        viewModelScope.launch { queueRepo.move(fromPos, toPos) }
    }

    fun clear() {
        viewModelScope.launch { queueRepo.clear() }
    }
}
