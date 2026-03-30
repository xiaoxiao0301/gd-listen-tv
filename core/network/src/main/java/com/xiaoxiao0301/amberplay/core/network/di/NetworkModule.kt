package com.xiaoxiao0301.amberplay.core.network.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.xiaoxiao0301.amberplay.core.network.api.MusicApiService
import com.xiaoxiao0301.amberplay.core.network.ratelimit.RateLimiter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://music-api.gdstudio.xyz/"

    @Provides @Singleton
    fun provideOkHttpClient(rateLimiter: RateLimiter): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                // 在 OkHttp IO 线程上阻塞等待令牌
                runBlocking { rateLimiter.acquire() }
                chain.proceed(chain.request())
            }
            .addNetworkInterceptor(
                HttpLoggingInterceptor().apply {
                    // 仅在 DEBUG 构建时启用详细日志；Release 构建中 ProGuard 会移除
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

    @Provides @Singleton
    fun provideMoshi(): Moshi =
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

    @Provides @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides @Singleton
    fun provideMusicApiService(retrofit: Retrofit): MusicApiService =
        retrofit.create(MusicApiService::class.java)
}
