package com.xiaoxiao0301.amberplay.core.database.di

import android.content.Context
import androidx.room.Room
import com.xiaoxiao0301.amberplay.core.database.AppDatabase
import com.xiaoxiao0301.amberplay.core.database.dao.FavoriteDao
import com.xiaoxiao0301.amberplay.core.database.dao.HistoryDao
import com.xiaoxiao0301.amberplay.core.database.dao.PlaylistDao
import com.xiaoxiao0301.amberplay.core.database.dao.QueueDao
import com.xiaoxiao0301.amberplay.core.database.dao.SongDao
import com.xiaoxiao0301.amberplay.core.database.dao.StatsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "amber.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun provideSongDao(db: AppDatabase): SongDao           = db.songDao()
    @Provides fun providePlaylistDao(db: AppDatabase): PlaylistDao   = db.playlistDao()
    @Provides fun provideFavoriteDao(db: AppDatabase): FavoriteDao   = db.favoriteDao()
    @Provides fun provideHistoryDao(db: AppDatabase): HistoryDao     = db.historyDao()
    @Provides fun provideQueueDao(db: AppDatabase): QueueDao         = db.queueDao()
    @Provides fun provideStatsDao(db: AppDatabase): StatsDao         = db.statsDao()
}
