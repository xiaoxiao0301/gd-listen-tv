package com.xiaoxiao0301.amberplay.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xiaoxiao0301.amberplay.core.database.entity.PlayHistoryEntity
import com.xiaoxiao0301.amberplay.core.database.entity.SearchHistoryEntity
import com.xiaoxiao0301.amberplay.core.database.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    // ─── 播放历史 ───────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayRecord(record: PlayHistoryEntity)

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN play_history ph ON s.id = ph.song_id
        ORDER BY ph.played_at DESC
        LIMIT :limit
    """)
    fun getRecentPlayedSongs(limit: Int): Flow<List<SongEntity>>

    @Query("SELECT * FROM play_history ORDER BY played_at DESC LIMIT :limit")
    fun getPlayHistory(limit: Int): Flow<List<PlayHistoryEntity>>

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
