package com.xiaoxiao0301.amberplay.core.database.di

import com.xiaoxiao0301.amberplay.core.database.repository.FavoriteRepositoryImpl
import com.xiaoxiao0301.amberplay.core.database.repository.HistoryRepositoryImpl
import com.xiaoxiao0301.amberplay.core.database.repository.PlaylistRepositoryImpl
import com.xiaoxiao0301.amberplay.core.database.repository.QueueRepositoryImpl
import com.xiaoxiao0301.amberplay.domain.repository.FavoriteRepository
import com.xiaoxiao0301.amberplay.domain.repository.HistoryRepository
import com.xiaoxiao0301.amberplay.domain.repository.PlaylistRepository
import com.xiaoxiao0301.amberplay.domain.repository.QueueRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseBindingsModule {

    @Binds @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

    @Binds @Singleton
    abstract fun bindQueueRepository(impl: QueueRepositoryImpl): QueueRepository

    @Binds @Singleton
    abstract fun bindFavoriteRepository(impl: FavoriteRepositoryImpl): FavoriteRepository

    @Binds @Singleton
    abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository
}
