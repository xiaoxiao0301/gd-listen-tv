package com.xiaoxiao0301.amberplay.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.xiaoxiao0301.amberplay.core.database.AppDatabase
import com.xiaoxiao0301.amberplay.core.database.DatabaseKeyManager
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
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase {
        val passphrase = DatabaseKeyManager.getOrCreatePassphrase(ctx)

        // Factory-reset recovery: if the Android Keystore key was destroyed (e.g. factory
        // reset, lock-screen key eviction), DatabaseKeyManager generated a brand-new
        // passphrase and set this flag.  The existing database file is encrypted with the
        // old, now-irrecoverable passphrase — attempting to open it would crash.
        // We delete it here so Room can create a fresh, empty database with the new key.
        // All user data is unavoidably lost after a factory reset; this ensures the app
        // at least starts cleanly rather than crashing on every subsequent launch.
        if (DatabaseKeyManager.lastWasRecoveredFromKeystoreFailure) {
            ctx.getDatabasePath("amber.db").delete()
        }

        val factory = SupportFactory(passphrase)
        passphrase.fill(0) // zero out passphrase bytes immediately after handing to SupportFactory
        return Room.databaseBuilder(ctx, AppDatabase::class.java, "amber.db")
            .openHelperFactory(factory)
            // Add explicit migrations here when bumping the DB version.
            // Example: .addMigrations(MIGRATION_1_2)
            // NEVER use fallbackToDestructiveMigration in production —
            // it silently drops all user data on schema change.
            .build()
    }

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
