package com.xiaoxiao0301.amberplay.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "play_stats")
data class PlayStatEntity(
    @PrimaryKey
    @ColumnInfo(name = "song_id")     val songId: String,
    @ColumnInfo(name = "play_count")  val playCount: Int  = 0,
    @ColumnInfo(name = "total_ms")    val totalMs: Long   = 0L,
    @ColumnInfo(name = "last_played") val lastPlayed: Long,
)
