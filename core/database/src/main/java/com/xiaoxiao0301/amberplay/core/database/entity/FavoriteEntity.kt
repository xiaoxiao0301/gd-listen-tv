package com.xiaoxiao0301.amberplay.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorites",
    foreignKeys = [
        ForeignKey(
            entity        = SongEntity::class,
            parentColumns = ["id"],
            childColumns  = ["song_id"],
            onDelete      = ForeignKey.CASCADE,
        )
    ]
)
data class FavoriteEntity(
    @PrimaryKey
    @ColumnInfo(name = "song_id")  val songId: String,
    @ColumnInfo(name = "added_at") val addedAt: Long,
)
