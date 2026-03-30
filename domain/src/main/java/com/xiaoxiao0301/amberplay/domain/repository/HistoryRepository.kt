package com.xiaoxiao0301.amberplay.domain.repository

import com.xiaoxiao0301.amberplay.domain.model.PlayRecord
import com.xiaoxiao0301.amberplay.domain.model.PlayStat
import com.xiaoxiao0301.amberplay.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getPlayHistory(limit: Int = 50): Flow<List<PlayRecord>>
    suspend fun addPlayRecord(song: Song, durationPlayedMs: Long)
    fun getSearchHistory(): Flow<List<String>>
    suspend fun addSearchHistory(keyword: String)
    suspend fun clearSearchHistory()
    fun getTopPlayStats(limit: Int = 10): Flow<List<PlayStat>>
    suspend fun incrementPlayStat(song: Song)
    fun getTotalPlayCount(): Flow<Long>
    fun getTotalPlayDurationMs(): Flow<Long>
}
