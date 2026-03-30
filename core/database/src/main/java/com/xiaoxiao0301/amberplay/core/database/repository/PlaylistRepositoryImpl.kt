package com.xiaoxiao0301.amberplay.core.database.repository

import com.xiaoxiao0301.amberplay.core.database.dao.PlaylistDao
import com.xiaoxiao0301.amberplay.core.database.dao.SongDao
import com.xiaoxiao0301.amberplay.core.database.entity.PlaylistEntity
import com.xiaoxiao0301.amberplay.core.database.entity.PlaylistSongCrossRef
import com.xiaoxiao0301.amberplay.core.database.mapper.toDomain
import com.xiaoxiao0301.amberplay.core.database.mapper.toEntity
import com.xiaoxiao0301.amberplay.domain.model.Playlist
import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val songDao: SongDao,
) : PlaylistRepository {

    override fun getAllPlaylists(): Flow<List<Playlist>> =
        playlistDao.getAllPlaylists().map { list ->
            list.map { entity ->
                Playlist(
                    id          = entity.id,
                    name        = entity.name,
                    description = entity.description,
                    coverSongId = entity.coverSongId,
                    createdAt   = entity.createdAt,
                    updatedAt   = entity.updatedAt,
                )
            }
        }

    override fun getPlaylistSongs(playlistId: Int): Flow<List<Song>> =
        playlistDao.getPlaylistSongs(playlistId).map { list -> list.map { it.toDomain() } }

    override suspend fun createPlaylist(name: String, description: String): Playlist {
        val now = System.currentTimeMillis()
        val entity = PlaylistEntity(name = name, description = description, createdAt = now, updatedAt = now)
        val id = playlistDao.insertPlaylist(entity).toInt()
        return Playlist(id = id, name = name, description = description, createdAt = now, updatedAt = now)
    }

    override suspend fun deletePlaylist(id: Int) {
        playlistDao.deletePlaylist(id)
    }

    override suspend fun addSongsToPlaylist(playlistId: Int, songs: List<Song>) {
        val maxPos = playlistDao.getMaxPosition(playlistId) ?: -1
        songs.forEachIndexed { index, song ->
            songDao.upsert(song.toEntity())
            playlistDao.insertCrossRef(
                PlaylistSongCrossRef(
                    playlistId = playlistId,
                    songId     = song.id,
                    position   = maxPos + 1 + index,
                    addedAt    = System.currentTimeMillis(),
                )
            )
        }
    }

    override suspend fun removeSongFromPlaylist(playlistId: Int, songId: String) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    override suspend fun reorderSong(playlistId: Int, fromPos: Int, toPos: Int) {
        playlistDao.reorderSong(playlistId, fromPos, toPos)
    }

    override suspend fun exportPlaylists(): File =
        throw NotImplementedError("Export not yet implemented — coming in Sprint 3")

    override suspend fun importPlaylists(file: File) =
        throw NotImplementedError("Import not yet implemented — coming in Sprint 3")
}
