package com.xiaoxiao0301.amberplay.core.network.di

import com.xiaoxiao0301.amberplay.core.network.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.xiaoxiao0301.amberplay.core.network.api.MusicApiService
import com.xiaoxiao0301.amberplay.core.network.ratelimit.RateLimiter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val BASE_URL get() = BuildConfig.MUSIC_API_BASE_URL

    /**
     * SEC-03: Certificate pinning for music-api.gdstudio.xyz.
     * Leaf pin  : sha256/TazM8eBi89wJfvs7+3h1JlEf3z9TTExU2j9XCJwHReA= (current leaf cert)
     * Backup pin: sha256/kIdp6NNEd8wsugYyyIYFsi1ylMCED3hZbSR8ZFsa/A4=  (Google Trust Services WE1 intermediate)
     *
     * To refresh pins after cert rotation run:
     *   openssl s_client -connect music-api.gdstudio.xyz:443 -servername music-api.gdstudio.xyz 2>/dev/null \
     *     | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der 2>/dev/null \
     *     | openssl dgst -sha256 -binary | base64
     */
    private val certificatePinner = CertificatePinner.Builder()
        .add("music-api.gdstudio.xyz", "sha256/TazM8eBi89wJfvs7+3h1JlEf3z9TTExU2j9XCJwHReA=")
        .add("music-api.gdstudio.xyz", "sha256/kIdp6NNEd8wsugYyyIYFsi1ylMCED3hZbSR8ZFsa/A4=")
        .build()

    @Provides @Singleton
    fun provideOkHttpClient(rateLimiter: RateLimiter): OkHttpClient =
        OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .addInterceptor { chain ->
                // 同步获取令牌，不需要 runBlocking，不阻塞协程线程池
                val waitMs = rateLimiter.acquireSync()
                if (waitMs > 0) Thread.sleep(waitMs)
                chain.proceed(chain.request())
            }
            .addNetworkInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                            else HttpLoggingInterceptor.Level.NONE
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
