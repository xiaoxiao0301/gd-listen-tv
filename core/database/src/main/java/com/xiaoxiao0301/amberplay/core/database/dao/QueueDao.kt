package com.xiaoxiao0301.amberplay.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.xiaoxiao0301.amberplay.core.database.entity.QueueItemEntity
import com.xiaoxiao0301.amberplay.core.database.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QueueDao {

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN queue_items qi ON s.id = qi.song_id
        ORDER BY qi.position ASC
    """)
    fun getQueueSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM queue_items ORDER BY position ASC")
    suspend fun getAllItems(): List<QueueItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: QueueItemEntity)

    @Query("SELECT MAX(position) FROM queue_items")
    suspend fun getMaxPosition(): Int?

    @Query("DELETE FROM queue_items WHERE position = :position")
    suspend fun removeAtPosition(position: Int)

    @Query("DELETE FROM queue_items")
    suspend fun clearQueue()

    /** 在 pos 之后的所有位置 +1（为插队腾出位置）
     *  SQLite 不支持 UPDATE ... ORDER BY，用两步偏移法避免主键冲突：
     *  1. 先把目标行位移到不会冲突的大偏移区间
     *  2. 再把偏移区间的行位移回正常值 +1
     */
    @Query("UPDATE queue_items SET position = position + 1000000 WHERE position > :afterPos")
    suspend fun shiftAfterStep1(afterPos: Int)

    @Query("UPDATE queue_items SET position = position - 999999 WHERE position > 1000000")
    suspend fun shiftAfterStep2()

    @Transaction
    suspend fun shiftAfter(afterPos: Int) {
        shiftAfterStep1(afterPos)
        shiftAfterStep2()
    }

    @Transaction
    suspend fun insertAsNext(afterPos: Int, item: QueueItemEntity) {
        shiftAfter(afterPos)
        insertItem(item.copy(position = afterPos + 1))
    }

    @Transaction
    suspend fun move(fromPos: Int, toPos: Int) {
        if (fromPos == toPos) return
        if (fromPos < toPos) {
            shiftRangeDown(fromPos + 1, toPos)
        } else {
            shiftRangeUp(toPos, fromPos - 1)
        }
        updatePosition(fromPos, toPos)
    }

    @Query("UPDATE queue_items SET position = position - 1 WHERE position BETWEEN :from AND :to")
    suspend fun shiftRangeDown(from: Int, to: Int)

    @Query("UPDATE queue_items SET position = position + 1 WHERE position BETWEEN :from AND :to")
    suspend fun shiftRangeUp(from: Int, to: Int)

    @Query("UPDATE queue_items SET position = :to WHERE position = :from")
    suspend fun updatePosition(from: Int, to: Int)
}
