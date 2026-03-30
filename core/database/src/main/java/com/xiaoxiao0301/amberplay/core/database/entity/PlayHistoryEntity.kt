package com.xiaoxiao0301.amberplay.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "play_history",
    indices = [
        Index("song_id"),
        Index("played_at"),
    ]
)
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")                  val id: Long = 0L,
    @ColumnInfo(name = "song_id")             val songId: String,
    @ColumnInfo(name = "played_at")           val playedAt: Long,
    @ColumnInfo(name = "duration_played_ms")  val durationPlayedMs: Long = 0L,
)
