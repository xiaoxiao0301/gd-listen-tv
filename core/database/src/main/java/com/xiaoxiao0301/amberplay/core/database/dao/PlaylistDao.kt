package com.xiaoxiao0301.amberplay.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.xiaoxiao0301.amberplay.core.database.entity.PlaylistEntity
import com.xiaoxiao0301.amberplay.core.database.entity.PlaylistSongCrossRef
import com.xiaoxiao0301.amberplay.core.database.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY updated_at DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Int): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Int)

    @Query("UPDATE playlists SET name=:name, updated_at=:now WHERE id=:id")
    suspend fun renamePlaylist(id: Int, name: String, now: Long)

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN playlist_songs ps ON s.id = ps.song_id
        WHERE ps.playlist_id = :playlistId
        ORDER BY ps.position ASC
    """)
    fun getPlaylistSongs(playlistId: Int): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_songs WHERE playlist_id=:playlistId AND song_id=:songId")
    suspend fun removeSongFromPlaylist(playlistId: Int, songId: String)

    @Query("SELECT MAX(position) FROM playlist_songs WHERE playlist_id=:playlistId")
    suspend fun getMaxPosition(playlistId: Int): Int?

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlist_id=:playlistId")
    suspend fun getSongCount(playlistId: Int): Int

    @Transaction
    suspend fun reorderSong(playlistId: Int, fromPos: Int, toPos: Int) {
        if (fromPos == toPos) return
        if (fromPos < toPos) {
            shiftDown(playlistId, fromPos + 1, toPos)
        } else {
            shiftUp(playlistId, toPos, fromPos - 1)
        }
        setPosition(playlistId, fromPos, toPos)
    }

    @Query("UPDATE playlist_songs SET position = position - 1 WHERE playlist_id=:pid AND position BETWEEN :from AND :to")
    suspend fun shiftDown(pid: Int, from: Int, to: Int)

    @Query("UPDATE playlist_songs SET position = position + 1 WHERE playlist_id=:pid AND position BETWEEN :from AND :to")
    suspend fun shiftUp(pid: Int, from: Int, to: Int)

    @Query("UPDATE playlist_songs SET position=:to WHERE playlist_id=:pid AND position=:from")
    suspend fun setPosition(pid: Int, from: Int, to: Int)
}
