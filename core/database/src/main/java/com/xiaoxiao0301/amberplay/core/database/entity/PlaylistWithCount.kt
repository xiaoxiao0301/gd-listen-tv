package com.xiaoxiao0301.amberplay.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class PlaylistWithCount(
    @Embedded                              val playlist: PlaylistEntity,
    @ColumnInfo(name = "song_count") val songCount: Int,
)
