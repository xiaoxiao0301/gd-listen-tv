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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val songDao: SongDao,
    @ApplicationContext private val context: Context,
) : PlaylistRepository {

    override fun getAllPlaylists(): Flow<List<Playlist>> =
        playlistDao.getAllPlaylists().map { list ->
            list.map { entity ->
                Playlist(
                    id          = entity.id,
                    name        = entity.name,
                    description = entity.description,
                    coverSongId = entity.coverSongId,
                    songCount   = playlistDao.getSongCount(entity.id),
                    createdAt   = entity.createdAt,
                    updatedAt   = entity.updatedAt,
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
        val root = JSONObject().apply {
            put("version", 1)
            put("exported_at", System.currentTimeMillis())
            val playlistsArr = JSONArray()
            playlists.forEach { playlist ->
                val songs = getPlaylistSongs(playlist.id).first()
                val songsArr = JSONArray()
                songs.forEach { song ->
                    songsArr.put(JSONObject().apply {
                        put("id",       song.id)
                        put("source",   song.source)
                        put("track_id", song.trackId)
                        put("name",     song.name)
                        put("artists",  JSONArray(song.artists))
                        put("album",    song.album)
                        put("pic_id",   song.picId)
                        put("lyric_id", song.lyricId)
                    })
                }
                playlistsArr.put(JSONObject().apply {
                    put("name",        playlist.name)
                    put("description", playlist.description)
                    put("songs",       songsArr)
                })
            }
            put("playlists", playlistsArr)
        }
        root.toString(2)
    }

    // ─── Import ───────────────────────────────────────────────────────────────

    override suspend fun importPlaylists(jsonContent: String): Unit = withContext(Dispatchers.IO) {
        val json   = JSONObject(jsonContent)
        val arr    = json.getJSONArray("playlists")
        val now    = System.currentTimeMillis()

        for (i in 0 until arr.length()) {
            val pObj   = arr.getJSONObject(i)
            val name   = pObj.getString("name")
            val desc   = pObj.optString("description", "")
            val entity = PlaylistEntity(name = name, description = desc,
                createdAt = now, updatedAt = now)
            val playlistId = playlistDao.insertPlaylist(entity).toInt()

            val songsArr = pObj.getJSONArray("songs")
            for (j in 0 until songsArr.length()) {
                val sObj    = songsArr.getJSONObject(j)
                val source  = sObj.getString("source")
                val trackId = sObj.getString("track_id")
                val id      = sObj.optString("id", "$source:$trackId")
                val artistsRaw = sObj.getJSONArray("artists")
                val artists = List(artistsRaw.length()) { artistsRaw.getString(it) }

                val song = com.xiaoxiao0301.amberplay.domain.model.Song(
                    id       = id,
                    trackId  = trackId,
                    source   = source,
                    name     = sObj.getString("name"),
                    artists  = artists,
                    album    = sObj.optString("album", ""),
                    picId    = sObj.optString("pic_id", ""),
                    lyricId  = sObj.optString("lyric_id", ""),
                )
                songDao.upsertPreserving(song)
                playlistDao.insertCrossRef(
                    PlaylistSongCrossRef(
                        playlistId = playlistId,
                        songId     = id,
                        position   = j,
                        addedAt    = now,
                    )
                )
            }
        }
    }
}
