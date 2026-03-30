package com.xiaoxiao0301.amberplay.domain.repository

import com.xiaoxiao0301.amberplay.domain.model.Playlist
import com.xiaoxiao0301.amberplay.domain.model.Song
import kotlinx.coroutines.flow.Flow
import java.io.File

interface PlaylistRepository {
    fun getAllPlaylists(): Flow<List<Playlist>>
    fun getPlaylistSongs(playlistId: Int): Flow<List<Song>>
    suspend fun createPlaylist(name: String, description: String = ""): Playlist
    suspend fun deletePlaylist(id: Int)
    suspend fun addSongsToPlaylist(playlistId: Int, songs: List<Song>)
    suspend fun removeSongFromPlaylist(playlistId: Int, songId: String)
    suspend fun reorderSong(playlistId: Int, fromPos: Int, toPos: Int)
    suspend fun exportPlaylists(): File
    suspend fun importPlaylists(file: File)
}
