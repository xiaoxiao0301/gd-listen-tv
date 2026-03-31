package com.xiaoxiao0301.amberplay.core.database.entity

import androidx.room.Embedded

data class PlayStatWithSong(
    @Embedded                val stat: PlayStatEntity,
    @Embedded(prefix = "s_") val song: SongEntity,
)
