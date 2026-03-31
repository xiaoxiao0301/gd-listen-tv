package com.xiaoxiao0301.amberplay.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xiaoxiao0301.amberplay.core.database.entity.PlayStatEntity
import com.xiaoxiao0301.amberplay.core.database.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStat(stat: PlayStatEntity)

    @Query("SELECT * FROM play_stats WHERE song_id = :songId")
    suspend fun getStatBySongId(songId: String): PlayStatEntity?

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN play_stats ps ON s.id = ps.song_id
        ORDER BY ps.play_count DESC
        LIMIT :limit
    """)
    fun getTopPlayedSongs(limit: Int): Flow<List<SongEntity>>

    @Query("SELECT * FROM play_stats ORDER BY play_count DESC LIMIT :limit")
    fun getTopStats(limit: Int): Flow<List<PlayStatEntity>>

    @Query("SELECT COALESCE(SUM(play_count), 0) FROM play_stats")
    fun getTotalPlayCount(): Flow<Long>

    @Query("SELECT COALESCE(SUM(total_ms), 0) FROM play_stats")
    fun getTotalPlayDurationMs(): Flow<Long>
}
