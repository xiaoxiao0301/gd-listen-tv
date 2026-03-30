package com.xiaoxiao0301.amberplay.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.xiaoxiao0301.amberplay.core.database.dao.FavoriteDao
import com.xiaoxiao0301.amberplay.core.database.dao.SongDao
import com.xiaoxiao0301.amberplay.core.database.entity.FavoriteEntity
import com.xiaoxiao0301.amberplay.core.database.entity.SongEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoriteDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var favoriteDao: FavoriteDao
    private lateinit var songDao: SongDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        favoriteDao = db.favoriteDao()
        songDao     = db.songDao()
    }

    @After
    fun teardown() { db.close() }

    private fun song(id: String) = SongEntity(
        id = id, trackId = id, source = "netease", name = "Song $id",
        artists = "[\"Artist\"]", album = "Album", picId = "", lyricId = "", createdAt = 1_000L,
    )

    @Test
    fun addAndQueryFavorite() = runTest {
        songDao.upsert(song("s1"))
        favoriteDao.addFavorite(FavoriteEntity("s1", addedAt = 1_000L))

        favoriteDao.getFavoriteSongs().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("s1", list.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun removeFavorite() = runTest {
        songDao.upsert(song("s1"))
        favoriteDao.addFavorite(FavoriteEntity("s1", addedAt = 1_000L))
        favoriteDao.removeFavorite("s1")

        favoriteDao.getFavoriteSongs().test {
            assertEquals(0, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun batchRemoveFavorites() = runTest {
        listOf("s1", "s2", "s3").forEach { id ->
            songDao.upsert(song(id))
            favoriteDao.addFavorite(FavoriteEntity(id, addedAt = 1_000L))
        }

        favoriteDao.batchRemoveFavorites(listOf("s1", "s3"))

        favoriteDao.getFavoriteSongs().test {
            val remaining = awaitItem()
            assertEquals(1, remaining.size)
            assertEquals("s2", remaining.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun isFavoriteFlowReturnsCorrectCount() = runTest {
        songDao.upsert(song("s1"))
        favoriteDao.isFavoriteFlow("s1").test {
            assertEquals(0, awaitItem())
            favoriteDao.addFavorite(FavoriteEntity("s1", addedAt = 1_000L))
            assertEquals(1, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
