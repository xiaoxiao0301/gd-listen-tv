package com.xiaoxiao0301.amberplay.core.database.repository

import com.xiaoxiao0301.amberplay.core.database.dao.PlaylistDao
import com.xiaoxiao0301.amberplay.core.database.dao.SongDao
import com.xiaoxiao0301.amberplay.core.database.entity.PlaylistEntity
import com.xiaoxiao0301.amberplay.core.database.entity.PlaylistSongCrossRef
import com.xiaoxiao0301.amberplay.core.database.entity.PlaylistWithCount
import com.xiaoxiao0301.amberplay.core.database.entity.SongEntity
import com.xiaoxiao0301.amberplay.domain.model.Playlist
import com.xiaoxiao0301.amberplay.domain.model.Song
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PlaylistRepositoryImplTest {

    private val playlistDao = mockk<PlaylistDao>(relaxed = true)
    private val songDao     = mockk<SongDao>(relaxed = true)
    private val context     = mockk<android.content.Context>(relaxed = true)

    private lateinit var repo: PlaylistRepositoryImpl

    private fun playlist(id: Int, name: String) = Playlist(
        id = id, name = name, description = "desc", songCount = 2,
        createdAt = 1000L, updatedAt = 2000L,
    )

    private fun playlistWithCount(id: Int, name: String, count: Int = 2) = PlaylistWithCount(
        playlist = PlaylistEntity(
            id = id, name = name, description = "desc",
            createdAt = 1000L, updatedAt = 2000L,
        ),
        songCount = count,
    )

    private fun song(id: String, name: String = "Song $id") = Song(
        id = id, trackId = id, source = "netease", name = name,
        artists = listOf("Artist A", "Artist B"),
        album = "Test Album", picId = "pic1", lyricId = "lyr1",
    )

    @BeforeEach
    fun setup() {
        repo = PlaylistRepositoryImpl(playlistDao, songDao, context)
    }

    // ─── Export tests ──────────────────────────────────────────────────────────

    @Test
    fun `exportPlaylists serialises all playlists and songs to JSON`() = runTest {
        val songList = listOf(song("s1", "Alpha"), song("s2", "Beta"))
        every { playlistDao.getAllPlaylistsWithCount() } returns flowOf(
            listOf(playlistWithCount(1, "Favourites"))
        )
        every { playlistDao.getPlaylistSongs(1) } returns flowOf(
            songList.map { s ->
                com.xiaoxiao0301.amberplay.core.database.entity.SongEntity(
                    id = s.id, trackId = s.trackId, source = s.source,
                    name = s.name, artists = "[\"Artist A\",\"Artist B\"]",
                    album = s.album, picId = s.picId, lyricId = s.lyricId,
                    durationMs = 0L, createdAt = 0L,
                )
            }
        )

        val json = repo.exportPlaylists()

        assertTrue(json.contains("\"version\""))
        assertTrue(json.contains("Favourites"))
        assertTrue(json.contains("Alpha"))
        assertTrue(json.contains("Beta"))
        assertTrue(json.contains("Artist A"))
    }

    @Test
    fun `exportPlaylists with empty playlist produces valid JSON`() = runTest {
        every { playlistDao.getAllPlaylistsWithCount() } returns flowOf(
            listOf(playlistWithCount(1, "Empty list", count = 0))
        )
        every { playlistDao.getPlaylistSongs(1) } returns flowOf(emptyList())

        val json = repo.exportPlaylists()
        assertTrue(json.contains("\"songs\""))
        assertTrue(json.contains("Empty list"))
    }

    // ─── Import tests ──────────────────────────────────────────────────────────

    private val sampleJson = """
        {
          "version": 1,
          "exported_at": 1710000000000,
          "playlists": [
            {
              "name": "Road Trip",
              "description": "Summer vibes",
              "songs": [
                {
                  "id": "netease:001",
                  "source": "netease",
                  "track_id": "001",
                  "name": "Highway",
                  "artists": ["Band X"],
                  "album": "Roadside",
                  "pic_id": "p1",
                  "lyric_id": "l1"
                },
                {
                  "id": "netease:002",
                  "source": "netease",
                  "track_id": "002",
                  "name": "Sunset Drive",
                  "artists": ["Band Y", "Feat Z"],
                  "album": "Roadside",
                  "pic_id": "p2",
                  "lyric_id": "l2"
                }
              ]
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `importPlaylists creates playlist and inserts all songs`() = runTest {
        coEvery { playlistDao.insertPlaylist(any()) } returns 42L
        // upsertPreserving calls getById(id) then upsert(entity) — both are already relaxed

        repo.importPlaylists(sampleJson)

        coVerify(exactly = 1) { playlistDao.insertPlaylist(match { it.name == "Road Trip" }) }
        // upsert() called once per song (upsertPreserving calls getById then upsert)
        coVerify(exactly = 2) { songDao.upsert(any()) }
        coVerify(exactly = 2) { playlistDao.insertCrossRef(any()) }
    }

    @Test
    fun `importPlaylists preserves song order via position index`() = runTest {
        coEvery { playlistDao.insertPlaylist(any()) } returns 10L
        val crossRefSlot = mutableListOf<PlaylistSongCrossRef>()
        coEvery { playlistDao.insertCrossRef(capture(crossRefSlot)) } returns Unit

        repo.importPlaylists(sampleJson)

        assertEquals(0, crossRefSlot[0].position)
        assertEquals(1, crossRefSlot[1].position)
    }

    @Test
    fun `importPlaylists assigns fallback id when id field is blank`() = runTest {
        val jsonNoId = """
            {
              "version": 1,
              "exported_at": 0,
              "playlists": [{
                "name": "P",
                "description": "",
                "songs": [{
                  "id": "",
                  "source": "kugou",
                  "track_id": "xyz",
                  "name": "Test",
                  "artists": [],
                  "album": "",
                  "pic_id": "",
                  "lyric_id": ""
                }]
              }]
            }
        """.trimIndent()
        coEvery { playlistDao.insertPlaylist(any()) } returns 1L
        // upsertPreserving calls getById (already relaxed, returns null) then upsert
        val entitySlot = slot<SongEntity>()
        coEvery { songDao.upsert(capture(entitySlot)) } returns Unit
        coEvery { playlistDao.insertCrossRef(any()) } returns Unit

        repo.importPlaylists(jsonNoId)

        assertEquals("kugou:xyz", entitySlot.captured.id)
    }

    @Test
    fun `importPlaylists multi-artist list is preserved`() = runTest {
        coEvery { playlistDao.insertPlaylist(any()) } returns 1L
        val entitySlots = mutableListOf<SongEntity>()
        coEvery { songDao.upsert(capture(entitySlots)) } returns Unit
        coEvery { playlistDao.insertCrossRef(any()) } returns Unit

        repo.importPlaylists(sampleJson)

        // SongEntity stores artists as JSON array string — parse them back for assertion
        // SongEntity stores artists as JSON array string — verify it contains the expected names
        val highwayArtistsJson = entitySlots.first { it.id == "netease:001" }.artists
        val sunsetArtistsJson  = entitySlots.first { it.id == "netease:002" }.artists
        assertTrue(highwayArtistsJson.contains("Band X"))
        assertTrue(sunsetArtistsJson.contains("Band Y"))
        assertTrue(sunsetArtistsJson.contains("Feat Z"))
    }
}
