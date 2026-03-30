package com.xiaoxiao0301.amberplay.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")        val id: String,        // "{source}:{trackId}"
    @ColumnInfo(name = "track_id")  val trackId: String,
    @ColumnInfo(name = "source")    val source: String,
    @ColumnInfo(name = "name")      val name: String,
    @ColumnInfo(name = "artists")   val artists: String,   // JSON array string
    @ColumnInfo(name = "album")     val album: String,
    @ColumnInfo(name = "pic_id")    val picId: String,
    @ColumnInfo(name = "lyric_id")  val lyricId: String,
    @ColumnInfo(name = "duration_ms") val durationMs: Long = 0L,
    @ColumnInfo(name = "created_at")  val createdAt: Long,
)
