package com.xiaoxiao0301.amberplay.core.database.entity

import androidx.room.Embedded

data class PlayHistoryWithSong(
    @Embedded                val history: PlayHistoryEntity,
    @Embedded(prefix = "s_") val song: SongEntity,
)
