package com.xiaoxiao0301.amberplay.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.xiaoxiao0301.amberplay.core.database.dao.FavoriteDao
import com.xiaoxiao0301.amberplay.core.database.dao.HistoryDao
import com.xiaoxiao0301.amberplay.core.database.dao.PlaylistDao
import com.xiaoxiao0301.amberplay.core.database.dao.QueueDao
import com.xiaoxiao0301.amberplay.core.database.dao.SongDao
import com.xiaoxiao0301.amberplay.core.database.dao.StatsDao
import com.xiaoxiao0301.amberplay.core.database.entity.FavoriteEntity
import com.xiaoxiao0301.amberplay.core.database.entity.PlayHistoryEntity
import com.xiaoxiao0301.amberplay.core.database.entity.PlayStatEntity
import com.xiaoxiao0301.amberplay.core.database.entity.PlaylistEntity
import com.xiaoxiao0301.amberplay.core.database.entity.PlaylistSongCrossRef
import com.xiaoxiao0301.amberplay.core.database.entity.QueueItemEntity
import com.xiaoxiao0301.amberplay.core.database.entity.SearchHistoryEntity
import com.xiaoxiao0301.amberplay.core.database.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        FavoriteEntity::class,
        PlayHistoryEntity::class,
        PlayStatEntity::class,
        SearchHistoryEntity::class,
        QueueItemEntity::class,
    ],
    version      = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun queueDao(): QueueDao
    abstract fun statsDao(): StatsDao
}
