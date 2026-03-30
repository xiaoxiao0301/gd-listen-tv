package com.xiaoxiao0301.amberplay.domain.repository

import com.xiaoxiao0301.amberplay.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface QueueRepository {
    fun getQueue(): Flow<List<Song>>
    suspend fun addToEnd(song: Song)
    suspend fun addAsNext(currentPosition: Int, song: Song)
    suspend fun remove(position: Int)
    suspend fun move(fromPos: Int, toPos: Int)
    suspend fun clear()
    suspend fun getQueueSnapshot(): List<Song>
}
