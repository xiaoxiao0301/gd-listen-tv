package com.xiaoxiao0301.amberplay.core.database.repository

import com.xiaoxiao0301.amberplay.core.database.dao.HistoryDao
import com.xiaoxiao0301.amberplay.core.database.dao.SongDao
import com.xiaoxiao0301.amberplay.core.database.dao.StatsDao
import com.xiaoxiao0301.amberplay.core.database.entity.PlayHistoryEntity
import com.xiaoxiao0301.amberplay.core.database.entity.SearchHistoryEntity
import com.xiaoxiao0301.amberplay.core.database.mapper.toDomain
import com.xiaoxiao0301.amberplay.core.database.mapper.toEntity
import com.xiaoxiao0301.amberplay.core.database.mapper.upsertPreserving
import com.xiaoxiao0301.amberplay.domain.model.PlayRecord
import com.xiaoxiao0301.amberplay.domain.model.PlayStat
import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao,
    private val songDao:    SongDao,
    private val statsDao:   StatsDao,
) : HistoryRepository {

    override fun getPlayHistory(limit: Int): Flow<List<PlayRecord>> =
        historyDao.getPlayHistoryWithSongs(limit).map { rows ->
            rows.map { row ->
                PlayRecord(
                    id               = row.history.id,
                    song             = row.song.toDomain(),
                    playedAt         = row.history.playedAt,
                    durationPlayedMs = row.history.durationPlayedMs,
                )
            }
        }

    override suspend fun addPlayRecord(song: Song, durationPlayedMs: Long) {
        songDao.upsertPreserving(song)
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

    override fun getTopPlayStats(limit: Int): Flow<List<PlayStat>> =
        statsDao.getTopStatsWithSongs(limit).map { rows ->
            rows.map { row ->
                PlayStat(
                    song       = row.song.toDomain(),
                    playCount  = row.stat.playCount,
                    totalMs    = row.stat.totalMs,
                    lastPlayed = row.stat.lastPlayed,
                )
            }
        }

    override suspend fun incrementPlayStat(song: Song) {
        songDao.upsertPreserving(song)
        statsDao.incrementStat(
            songId     = song.id,
            durationMs = song.durationMs,
            now        = System.currentTimeMillis(),
        )
    }

    override fun getTotalPlayCount(): Flow<Long> =
        statsDao.getTotalPlayCount()

    override fun getTotalPlayDurationMs(): Flow<Long> =
        statsDao.getTotalPlayDurationMs()
}
