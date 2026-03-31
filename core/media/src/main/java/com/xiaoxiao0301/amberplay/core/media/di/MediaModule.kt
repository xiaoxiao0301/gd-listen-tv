package com.xiaoxiao0301.amberplay.core.media.di

import com.xiaoxiao0301.amberplay.core.media.IPlayerController
import com.xiaoxiao0301.amberplay.core.media.PlayerController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaModule {

    @Binds
    @Singleton
    abstract fun bindPlayerController(impl: PlayerController): IPlayerController
}
