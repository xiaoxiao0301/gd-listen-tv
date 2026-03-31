package com.xiaoxiao0301.amberplay.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xiaoxiao0301.amberplay.core.database.entity.PlayStatEntity
import com.xiaoxiao0301.amberplay.core.database.entity.PlayStatWithSong
import com.xiaoxiao0301.amberplay.core.database.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStat(stat: PlayStatEntity)

    /**
     * 原子递增播放次数，消除并发 read-modify-write 竞态。
     * 首次插入 play_count=1；后续冲突时 play_count+1、total_ms 累加、last_played 更新。
     */
    @Query("""
        INSERT INTO play_stats (song_id, play_count, total_ms, last_played)
        VALUES (:songId, 1, :durationMs, :now)
        ON CONFLICT(song_id) DO UPDATE
        SET play_count  = play_count + 1,
            total_ms    = total_ms + excluded.total_ms,
            last_played = excluded.last_played
    """)
    suspend fun incrementStat(songId: String, durationMs: Long, now: Long)

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

    @Query("""
        SELECT ps.*,
               s.id          AS s_id,
               s.track_id    AS s_track_id,
               s.source      AS s_source,
               s.name        AS s_name,
               s.artists     AS s_artists,
               s.album       AS s_album,
               s.pic_id      AS s_pic_id,
               s.lyric_id    AS s_lyric_id,
               s.duration_ms AS s_duration_ms,
               s.created_at  AS s_created_at
        FROM play_stats ps
        INNER JOIN songs s ON ps.song_id = s.id
        ORDER BY ps.play_count DESC LIMIT :limit
    """)
    fun getTopStatsWithSongs(limit: Int): Flow<List<PlayStatWithSong>>

    @Query("SELECT COALESCE(SUM(play_count), 0) FROM play_stats")
    fun getTotalPlayCount(): Flow<Long>

    @Query("SELECT COALESCE(SUM(total_ms), 0) FROM play_stats")
    fun getTotalPlayDurationMs(): Flow<Long>
}
