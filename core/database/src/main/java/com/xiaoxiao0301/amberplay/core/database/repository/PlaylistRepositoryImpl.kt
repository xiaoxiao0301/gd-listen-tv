package com.xiaoxiao0301.amberplay.core.database.repository

import android.content.Context
import com.xiaoxiao0301.amberplay.core.database.dao.PlaylistDao
import com.xiaoxiao0301.amberplay.core.database.dao.SongDao
import com.xiaoxiao0301.amberplay.core.database.entity.PlaylistEntity
import com.xiaoxiao0301.amberplay.core.database.entity.PlaylistSongCrossRef
import com.xiaoxiao0301.amberplay.core.database.mapper.toDomain
import com.xiaoxiao0301.amberplay.core.database.mapper.toEntity
import com.xiaoxiao0301.amberplay.core.database.mapper.upsertPreserving
import com.xiaoxiao0301.amberplay.domain.model.Playlist
import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.repository.PlaylistRepository
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// ─── JSON DTOs for playlist export/import ────────────────────────────────────

private data class SongJson(
    @Json(name = "id")        val id: String       = "",
    @Json(name = "source")    val source: String   = "",
    @Json(name = "track_id")  val trackId: String  = "",
    @Json(name = "name")      val name: String     = "",
    @Json(name = "artists")   val artists: List<String> = emptyList(),
    @Json(name = "album")     val album: String    = "",
    @Json(name = "pic_id")    val picId: String    = "",
    @Json(name = "lyric_id")  val lyricId: String  = "",
)

private data class PlaylistJson(
    @Json(name = "name")        val name: String            = "",
    @Json(name = "description") val description: String     = "",
    @Json(name = "songs")       val songs: List<SongJson>   = emptyList(),
)

private data class ExportRoot(
    @Json(name = "version")     val version: Int                  = 1,
    @Json(name = "exported_at") val exportedAt: Long               = 0L,
    @Json(name = "playlists")   val playlists: List<PlaylistJson>  = emptyList(),
)

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val songDao: SongDao,
    @ApplicationContext private val context: Context,
) : PlaylistRepository {

    private val exportAdapter = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
        .adapter(ExportRoot::class.java)
        .indent("  ")

    override fun getAllPlaylists(): Flow<List<Playlist>> =
        playlistDao.getAllPlaylistsWithCount().map { list ->
            list.map { withCount ->
                Playlist(
                    id          = withCount.playlist.id,
                    name        = withCount.playlist.name,
                    description = withCount.playlist.description,
                    coverSongId = withCount.playlist.coverSongId,
                    songCount   = withCount.songCount,
                    createdAt   = withCount.playlist.createdAt,
                    updatedAt   = withCount.playlist.updatedAt,
                )
            }
        }

    override suspend fun getPlaylist(id: Int): Playlist? {
        val entity = playlistDao.getPlaylistById(id) ?: return null
        val count  = playlistDao.getSongCount(id)
        return Playlist(
            id          = entity.id,
            name        = entity.name,
            description = entity.description,
            coverSongId = entity.coverSongId,
            songCount   = count,
            createdAt   = entity.createdAt,
            updatedAt   = entity.updatedAt,
        )
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
            songDao.upsertPreserving(song)
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

    // ─── Export ───────────────────────────────────────────────────────────────

    override suspend fun exportPlaylists(): String = withContext(Dispatchers.IO) {
        val playlists = getAllPlaylists().first()
        val root = ExportRoot(
            version     = 1,
            exportedAt  = System.currentTimeMillis(),
            playlists   = playlists.map { playlist ->
                val songs = getPlaylistSongs(playlist.id).first()
                PlaylistJson(
                    name        = playlist.name,
                    description = playlist.description,
                    songs       = songs.map { song ->
                        SongJson(
                            id       = song.id,
                            source   = song.source,
                            trackId  = song.trackId,
                            name     = song.name,
                            artists  = song.artists,
                            album    = song.album,
                            picId    = song.picId,
                            lyricId  = song.lyricId,
                        )
                    },
                )
            },
        )
        exportAdapter.toJson(root)
    }

    // ─── Import ───────────────────────────────────────────────────────────────

    override suspend fun importPlaylists(jsonContent: String): Unit = withContext(Dispatchers.IO) {
        val root = exportAdapter.fromJson(jsonContent) ?: error("Invalid playlist JSON")
        val now  = System.currentTimeMillis()

        root.playlists.forEach { pJson ->
            val entity = PlaylistEntity(
                name = pJson.name, description = pJson.description,
                createdAt = now, updatedAt = now,
            )
            val playlistId = playlistDao.insertPlaylist(entity).toInt()

            pJson.songs.forEachIndexed { index, sJson ->
                val id   = sJson.id.ifBlank { "${sJson.source}:${sJson.trackId}" }
                val song = Song(
                    id       = id,
                    trackId  = sJson.trackId,
                    source   = sJson.source,
                    name     = sJson.name,
                    artists  = sJson.artists,
                    album    = sJson.album,
                    picId    = sJson.picId,
                    lyricId  = sJson.lyricId,
                )
                songDao.upsertPreserving(song)
                playlistDao.insertCrossRef(
                    PlaylistSongCrossRef(
                        playlistId = playlistId,
                        songId     = id,
                        position   = index,
                        addedAt    = now,
                    )
                )
            }
        }
    }
}
