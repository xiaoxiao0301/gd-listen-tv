package com.xiaoxiao0301.amberplay.core.database.repository

import com.xiaoxiao0301.amberplay.core.database.dao.HistoryDao
import com.xiaoxiao0301.amberplay.core.database.dao.SongDao
import com.xiaoxiao0301.amberplay.core.database.entity.PlayHistoryEntity
import com.xiaoxiao0301.amberplay.core.database.entity.SearchHistoryEntity
import com.xiaoxiao0301.amberplay.core.database.mapper.toDomain
import com.xiaoxiao0301.amberplay.core.database.mapper.toEntity
import com.xiaoxiao0301.amberplay.domain.model.PlayRecord
import com.xiaoxiao0301.amberplay.domain.model.PlayStat
import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao,
    private val songDao: SongDao,
) : HistoryRepository {

    override fun getPlayHistory(limit: Int): Flow<List<PlayRecord>> =
        historyDao.getPlayHistory(limit).map { records ->
            records.mapNotNull { record ->
                val songEntity = songDao.getById(record.songId) ?: return@mapNotNull null
                PlayRecord(
                    id               = record.id,
                    song             = songEntity.toDomain(),
                    playedAt         = record.playedAt,
                    durationPlayedMs = record.durationPlayedMs,
                )
            }
        }

    override suspend fun addPlayRecord(song: Song, durationPlayedMs: Long) {
        songDao.upsert(song.toEntity())
        historyDao.insertPlayRecord(
            PlayHistoryEntity(
                songId           = song.id,
                playedAt         = System.currentTimeMillis(),
                durationPlayedMs = durationPlayedMs,
            )
        )
    }

    override fun getSearchHistory(): Flow<List<String>> =
        historyDao.getSearchHistory()

    override suspend fun addSearchHistory(keyword: String) {
        if (keyword.isBlank()) return
        // 先删旧记录（如存在），再插入，实现去重并更新时间
        historyDao.deleteSearchKeyword(keyword.trim())
        historyDao.insertSearchHistory(
            SearchHistoryEntity(keyword = keyword.trim(), searchedAt = System.currentTimeMillis())
        )
    }

    override suspend fun clearSearchHistory() {
        historyDao.clearSearchHistory()
    }

    override fun getTopPlayStats(limit: Int): Flow<List<PlayStat>> = emptyFlow()

    override suspend fun incrementPlayStat(song: Song) { /* Sprint 3 中实现 */ }
}
