package com.xiaoxiao0301.amberplay.core.database.repository

import com.xiaoxiao0301.amberplay.core.database.dao.QueueDao
import com.xiaoxiao0301.amberplay.core.database.dao.SongDao
import com.xiaoxiao0301.amberplay.core.database.entity.QueueItemEntity
import com.xiaoxiao0301.amberplay.core.database.mapper.toDomain
import com.xiaoxiao0301.amberplay.core.database.mapper.toEntity
import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.repository.QueueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QueueRepositoryImpl @Inject constructor(
    private val queueDao: QueueDao,
    private val songDao: SongDao,
) : QueueRepository {

    override fun getQueue(): Flow<List<Song>> =
        queueDao.getQueueSongs().map { entities -> entities.map { it.toDomain() } }

    override suspend fun addToEnd(song: Song) {
        songDao.upsert(song.toEntity())
        val nextPos = (queueDao.getMaxPosition() ?: -1) + 1
        queueDao.insertItem(
            QueueItemEntity(position = nextPos, songId = song.id, insertedAt = System.currentTimeMillis())
        )
    }

    override suspend fun addAsNext(currentPosition: Int, song: Song) {
        songDao.upsert(song.toEntity())
        queueDao.insertAsNext(
            afterPos = currentPosition,
            item     = QueueItemEntity(
                position   = currentPosition + 1,
                songId     = song.id,
                insertedAt = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun remove(position: Int) {
        queueDao.removeAtPosition(position)
    }

    override suspend fun move(fromPos: Int, toPos: Int) {
        queueDao.move(fromPos, toPos)
    }

    override suspend fun clear() {
        queueDao.clearQueue()
    }

    override suspend fun getQueueSnapshot(): List<Song> =
        queueDao.getAllItems()
            .mapNotNull { item -> songDao.getById(item.songId)?.toDomain() }
}
