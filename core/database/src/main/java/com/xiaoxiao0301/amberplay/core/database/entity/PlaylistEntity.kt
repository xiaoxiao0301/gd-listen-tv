package com.xiaoxiao0301.amberplay.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")           val id: Int = 0,
    @ColumnInfo(name = "name")         val name: String,
    @ColumnInfo(name = "description")  val description: String = "",
    @ColumnInfo(name = "cover_song_id")val coverSongId: String? = null,
    @ColumnInfo(name = "created_at")   val createdAt: Long,
    @ColumnInfo(name = "updated_at")   val updatedAt: Long,
)
