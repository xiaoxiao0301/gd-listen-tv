package com.xiaoxiao0301.amberplay.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "queue_items")
data class QueueItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "position")    val position: Int,
    @ColumnInfo(name = "song_id")     val songId: String,
    @ColumnInfo(name = "inserted_at") val insertedAt: Long,
)
