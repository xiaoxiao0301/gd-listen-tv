package com.xiaoxiao0301.amberplay.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.xiaoxiao0301.amberplay.core.database.dao.PlaylistDao
import com.xiaoxiao0301.amberplay.core.database.dao.SongDao
import com.xiaoxiao0301.amberplay.core.database.entity.PlaylistEntity
import com.xiaoxiao0301.amberplay.core.database.entity.PlaylistSongCrossRef
import com.xiaoxiao0301.amberplay.core.database.entity.SongEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var playlistDao: PlaylistDao
    private lateinit var songDao: SongDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        playlistDao = db.playlistDao()
        songDao     = db.songDao()
    }

    @After
    fun teardown() { db.close() }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private fun playlist(name: String, now: Long = 1_000L) =
        PlaylistEntity(name = name, createdAt = now, updatedAt = now)

    private fun song(id: String) = SongEntity(
        id = id, trackId = id, source = "netease", name = "Song $id",
        artists = "[\"Artist\"]", album = "Album", picId = "", lyricId = "", createdAt = 1_000L,
    )

    // ─── tests ───────────────────────────────────────────────────────────────

    @Test
    fun insertAndRetrievePlaylist() = runTest {
        val id = playlistDao.insertPlaylist(playlist("My Playlist")).toInt()
        val found = playlistDao.getPlaylistById(id)
        assertEquals("My Playlist", found?.name)
    }

    @Test
    fun deletePlaylistRemovesIt() = runTest {
        val id = playlistDao.insertPlaylist(playlist("Temp")).toInt()
        playlistDao.deletePlaylist(id)
        assertNull(playlistDao.getPlaylistById(id))
    }

    @Test
    fun getAllPlaylistsEmitsUpdates() = runTest {
        playlistDao.getAllPlaylists().test {
            val initial = awaitItem()
            assertEquals(0, initial.size)

            playlistDao.insertPlaylist(playlist("First"))
            val afterInsert = awaitItem()
            assertEquals(1, afterInsert.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addAndRemoveSongFromPlaylist() = runTest {
        val pid = playlistDao.insertPlaylist(playlist("Pop")).toInt()
        songDao.upsert(song("netease:001"))

        val crossRef = PlaylistSongCrossRef(playlistId = pid, songId = "netease:001", position = 0)
        playlistDao.insertCrossRef(crossRef)

        assertEquals(1, playlistDao.getSongCount(pid))

        playlistDao.removeSongFromPlaylist(pid, "netease:001")
        assertEquals(0, playlistDao.getSongCount(pid))
    }

    @Test
    fun reorderSongChangesPositions() = runTest {
        val pid = playlistDao.insertPlaylist(playlist("Rock")).toInt()
        listOf("s1", "s2", "s3").forEachIndexed { i, id ->
            songDao.upsert(song(id))
            playlistDao.insertCrossRef(PlaylistSongCrossRef(playlistId = pid, songId = id, position = i))
        }

        // Move s1 (pos 0) to pos 2
        playlistDao.reorderSong(pid, fromPos = 0, toPos = 2)

        playlistDao.getPlaylistSongs(pid).test {
            val songs = awaitItem()
            // After reorder, position 2 should be "s1"
            assertEquals("s1", songs[2].id)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
