package com.xiaoxiao0301.amberplay.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.xiaoxiao0301.amberplay.core.database.dao.QueueDao
import com.xiaoxiao0301.amberplay.core.database.dao.SongDao
import com.xiaoxiao0301.amberplay.core.database.entity.QueueItemEntity
import com.xiaoxiao0301.amberplay.core.database.entity.SongEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QueueDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var queueDao: QueueDao
    private lateinit var songDao: SongDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        queueDao = db.queueDao()
        songDao  = db.songDao()
    }

    @After
    fun teardown() { db.close() }

    private fun song(id: String) = SongEntity(
        id = id, trackId = id, source = "netease", name = "Song $id",
        artists = "[\"Artist\"]", album = "Album", picId = "", lyricId = "", createdAt = 1_000L,
    )

    private fun queueItem(pos: Int, songId: String) =
        QueueItemEntity(position = pos, songId = songId, insertedAt = 1_000L)

    @Test
    fun insertItemsAndGetQueue() = runTest {
        songDao.upsert(song("s1"))
        songDao.upsert(song("s2"))
        queueDao.insertItem(queueItem(0, "s1"))
        queueDao.insertItem(queueItem(1, "s2"))

        queueDao.getQueueSongs().test {
            val songs = awaitItem()
            assertEquals(2, songs.size)
            assertEquals("s1", songs[0].id)
            assertEquals("s2", songs[1].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun clearQueueRemovesAllItems() = runTest {
        songDao.upsert(song("s1"))
        queueDao.insertItem(queueItem(0, "s1"))
        queueDao.clearQueue()
        assertEquals(0, queueDao.getAllItems().size)
    }

    @Test
    fun insertAsNextShiftsPositions() = runTest {
        listOf("s1", "s2", "s3").forEachIndexed { i, id ->
            songDao.upsert(song(id))
            queueDao.insertItem(queueItem(i, id))
        }

        songDao.upsert(song("new"))
        // Insert after position 0 (after s1)
        queueDao.insertAsNext(afterPos = 0, item = queueItem(0, "new"))

        val items = queueDao.getAllItems()
        // Position 1 should now be "new"
        val newItem = items.first { it.position == 1 }
        assertEquals("new", newItem.songId)
        // s2 should have been pushed to position 2
        val s2Item = items.first { it.songId == "s2" }
        assertEquals(2, s2Item.position)
    }

    @Test
    fun moveItemChangesOrder() = runTest {
        listOf("s1", "s2", "s3").forEachIndexed { i, id ->
            songDao.upsert(song(id))
            queueDao.insertItem(queueItem(i, id))
        }

        // Move s1 (pos 0) to pos 2
        queueDao.move(fromPos = 0, toPos = 2)

        val items = queueDao.getAllItems().sortedBy { it.position }
        assertEquals("s2", items[0].songId)
        assertEquals("s3", items[1].songId)
        assertEquals("s1", items[2].songId)
    }
}
