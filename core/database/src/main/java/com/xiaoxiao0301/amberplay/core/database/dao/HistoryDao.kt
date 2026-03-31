package com.xiaoxiao0301.amberplay.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xiaoxiao0301.amberplay.core.database.entity.PlayHistoryEntity
import com.xiaoxiao0301.amberplay.core.database.entity.PlayHistoryWithSong
import com.xiaoxiao0301.amberplay.core.database.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    // ─── 播放历史 ───────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayRecord(record: PlayHistoryEntity)

    @Query("SELECT * FROM play_history ORDER BY played_at DESC LIMIT :limit")
    fun getPlayHistory(limit: Int): Flow<List<PlayHistoryEntity>>

    @Query("""
        SELECT h.*,
               s.id         AS s_id,
               s.track_id   AS s_track_id,
               s.source     AS s_source,
               s.name       AS s_name,
               s.artists    AS s_artists,
               s.album      AS s_album,
               s.pic_id     AS s_pic_id,
               s.lyric_id   AS s_lyric_id,
               s.duration_ms AS s_duration_ms,
               s.created_at AS s_created_at
        FROM play_history h
        INNER JOIN songs s ON h.song_id = s.id
        ORDER BY h.played_at DESC LIMIT :limit
    """)
    fun getPlayHistoryWithSongs(limit: Int): Flow<List<PlayHistoryWithSong>>

    // ─── 搜索历史 ───────────────────────────────────────────────

    @Query("SELECT keyword FROM search_history ORDER BY searched_at DESC LIMIT 30")
    fun getSearchHistory(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(entry: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE keyword = :keyword")
    suspend fun deleteSearchKeyword(keyword: String)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()
}
