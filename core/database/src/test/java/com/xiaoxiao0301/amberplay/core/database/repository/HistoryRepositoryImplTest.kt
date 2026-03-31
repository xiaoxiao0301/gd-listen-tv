package com.xiaoxiao0301.amberplay.core.database.repository

import app.cash.turbine.test
import com.xiaoxiao0301.amberplay.core.database.dao.HistoryDao
import com.xiaoxiao0301.amberplay.core.database.dao.SongDao
import com.xiaoxiao0301.amberplay.core.database.dao.StatsDao
import com.xiaoxiao0301.amberplay.core.database.entity.PlayStatEntity
import com.xiaoxiao0301.amberplay.core.database.entity.SongEntity
import com.xiaoxiao0301.amberplay.domain.model.Song
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HistoryRepositoryImplTest {

    private val historyDao = mockk<HistoryDao>(relaxed = true)
    private val songDao    = mockk<SongDao>(relaxed = true)
    private val statsDao   = mockk<StatsDao>(relaxed = true)

    private lateinit var repo: HistoryRepositoryImpl

    private val testSong = Song(
        id         = "netease:12345",
        trackId    = "12345",
        source     = "netease",
        name       = "Test Song",
        artists    = listOf("Artist A"),
        album      = "Album X",
        picId      = "pic1",
        lyricId    = "lyric1",
        durationMs = 180_000L,
    )

    private fun songEntity(id: String = testSong.id) = SongEntity(
        id         = id,
        trackId    = "12345",
        source     = "netease",
        name       = "Test Song",
        artists    = "[\"Artist A\"]",
        album      = "Album X",
        picId      = "pic1",
        lyricId    = "lyric1",
        durationMs = 180_000L,
        createdAt  = 1_000_000L,
    )

    @BeforeEach
    fun setUp() {
        coEvery { songDao.getById(any()) } returns null
        repo = HistoryRepositoryImpl(historyDao, songDao, statsDao)
    }

    // ─── incrementPlayStat ──────────────────────────────────────────

    @Test
    fun `incrementPlayStat accumulates totalMs for first play`() = runTest {
        coEvery { statsDao.getStatBySongId(testSong.id) } returns null

        val captured = slot<PlayStatEntity>()
        coEvery { statsDao.upsertStat(capture(captured)) } returns Unit

        repo.incrementPlayStat(testSong)

        assertEquals(1, captured.captured.playCount)
        assertEquals(180_000L, captured.captured.totalMs)
    }

    @Test
    fun `incrementPlayStat accumulates totalMs on subsequent plays`() = runTest {
        val existing = PlayStatEntity(
            songId     = testSong.id,
            playCount  = 3,
            totalMs    = 540_000L,     // 3 × 180 s
            lastPlayed = System.currentTimeMillis(),
        )
        coEvery { statsDao.getStatBySongId(testSong.id) } returns existing

        val captured = slot<PlayStatEntity>()
        coEvery { statsDao.upsertStat(capture(captured)) } returns Unit

        repo.incrementPlayStat(testSong)

        assertEquals(4, captured.captured.playCount)
        assertEquals(720_000L, captured.captured.totalMs)   // 4 × 180 s
    }

    @Test
    fun `incrementPlayStat does not reset totalMs to zero`() = runTest {
        // Before BUG-03 fix, totalMs was always set to 0; this test guards against regression
        val existing = PlayStatEntity(
            songId     = testSong.id,
            playCount  = 1,
            totalMs    = 180_000L,
            lastPlayed = 1_000_000L,
        )
        coEvery { statsDao.getStatBySongId(testSong.id) } returns existing

        val captured = slot<PlayStatEntity>()
        coEvery { statsDao.upsertStat(capture(captured)) } returns Unit

        repo.incrementPlayStat(testSong)

        assert(captured.captured.totalMs > 0L) { "totalMs must be > 0 after a play" }
        assertEquals(360_000L, captured.captured.totalMs)
    }

    // ─── addPlayRecord ──────────────────────────────────────────────

    @Test
    fun `addPlayRecord persists given durationPlayedMs`() = runTest {
        repo.addPlayRecord(testSong, durationPlayedMs = 95_000L)

        coVerify {
            historyDao.insertPlayRecord(match { it.durationPlayedMs == 95_000L })
        }
    }

    @Test
    fun `addPlayRecord uses current time as playedAt`() = runTest {
        val before = System.currentTimeMillis()
        repo.addPlayRecord(testSong, durationPlayedMs = 0L)
        val after = System.currentTimeMillis()

        coVerify {
            historyDao.insertPlayRecord(match { it.playedAt in before..after })
        }
    }

    // ─── getTopPlayStats ────────────────────────────────────────────

    @Test
    fun `getTopPlayStats maps entities correctly`() = runTest {
        val statEntity = PlayStatEntity(
            songId     = testSong.id,
            playCount  = 7,
            totalMs    = 1_260_000L,
            lastPlayed = 9_999_999L,
        )
        every { statsDao.getTopStats(10) } returns flowOf(listOf(statEntity))
        coEvery { songDao.getById(testSong.id) } returns songEntity()

        repo.getTopPlayStats(10).test {
            val stats = awaitItem()
            assertEquals(1, stats.size)
            assertEquals(7, stats[0].playCount)
            assertEquals(1_260_000L, stats[0].totalMs)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
