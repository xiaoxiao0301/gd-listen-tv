package com.xiaoxiao0301.amberplay.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xiaoxiao0301.amberplay.core.database.entity.FavoriteEntity
import com.xiaoxiao0301.amberplay.core.database.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN favorites f ON s.id = f.song_id
        ORDER BY f.added_at DESC
    """)
    fun getFavoriteSongs(): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE song_id = :songId")
    suspend fun removeFavorite(songId: String)

    @Query("SELECT COUNT(*) FROM favorites WHERE song_id = :songId")
    fun isFavoriteFlow(songId: String): Flow<Int>
}
