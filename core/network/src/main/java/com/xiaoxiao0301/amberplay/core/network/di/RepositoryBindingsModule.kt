package com.xiaoxiao0301.amberplay.core.network.di

import com.xiaoxiao0301.amberplay.core.network.repository.MusicRepositoryImpl
import com.xiaoxiao0301.amberplay.domain.repository.MusicRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindingsModule {

    @Binds @Singleton
    abstract fun bindMusicRepository(impl: MusicRepositoryImpl): MusicRepository
}
