package com.xiaoxiao0301.amberplay.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
            // Add explicit migrations here when bumping the DB version.
            // Example: .addMigrations(MIGRATION_1_2)
            // NEVER use fallbackToDestructiveMigration in production —
            // it silently drops all user data on schema change.
            .build()

    // Template for future migrations:
    // val MIGRATION_1_2 = object : Migration(1, 2) {
    //     override fun migrate(db: SupportSQLiteDatabase) {
    //         // db.execSQL("ALTER TABLE songs ADD COLUMN duration_ms INTEGER NOT NULL DEFAULT 0")
    //     }
    // }

    @Provides fun provideSongDao(db: AppDatabase): SongDao           = db.songDao()
    @Provides fun providePlaylistDao(db: AppDatabase): PlaylistDao   = db.playlistDao()
    @Provides fun provideFavoriteDao(db: AppDatabase): FavoriteDao   = db.favoriteDao()
    @Provides fun provideHistoryDao(db: AppDatabase): HistoryDao     = db.historyDao()
    @Provides fun provideQueueDao(db: AppDatabase): QueueDao         = db.queueDao()
    @Provides fun provideStatsDao(db: AppDatabase): StatsDao         = db.statsDao()
}
