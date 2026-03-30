package com.xiaoxiao0301.amberplay.domain.repository

import com.xiaoxiao0301.amberplay.domain.model.Playlist
import com.xiaoxiao0301.amberplay.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun getAllPlaylists(): Flow<List<Playlist>>
    suspend fun getPlaylist(id: Int): Playlist?
    fun getPlaylistSongs(playlistId: Int): Flow<List<Song>>
    suspend fun createPlaylist(name: String, description: String = ""): Playlist
    suspend fun deletePlaylist(id: Int)
    suspend fun addSongsToPlaylist(playlistId: Int, songs: List<Song>)
    suspend fun removeSongFromPlaylist(playlistId: Int, songId: String)
    suspend fun reorderSong(playlistId: Int, fromPos: Int, toPos: Int)
    suspend fun exportPlaylists(): String
    /** 导入歌单。[jsonContent] 为已读取的 JSON 字符串，文件读取应在调用方完成 */
    suspend fun importPlaylists(jsonContent: String)
}
