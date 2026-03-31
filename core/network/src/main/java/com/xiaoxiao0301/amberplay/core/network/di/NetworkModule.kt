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
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val BASE_URL get() = BuildConfig.MUSIC_API_BASE_URL

    /**
     * SEC-03/SEC-04: Certificate pinning for music-api.gdstudio.xyz.
     * Pins are sourced from BuildConfig (CERT_PIN_LEAF / CERT_PIN_BACKUP_CA) so they can be
     * updated in one place (build.gradle.kts) without touching runtime code.
     *
     * CERT_PIN_LEAF_EXPIRY contains the approximate leaf-cert expiry date.  At runtime the app
     * logs a warning when fewer than 30 days remain, reminding the developer to rotate the pin.
     *
     * To refresh pins after cert rotation run:
     *   openssl s_client -connect music-api.gdstudio.xyz:443 -servername music-api.gdstudio.xyz 2>/dev/null \
     *     | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der 2>/dev/null \
     *     | openssl dgst -sha256 -binary | base64
     */
    private val certificatePinner = CertificatePinner.Builder()
        .add("music-api.gdstudio.xyz", BuildConfig.CERT_PIN_LEAF)
        .add("music-api.gdstudio.xyz", BuildConfig.CERT_PIN_BACKUP_CA)
        .build()

    /** Log a warning if the leaf certificate is close to expiry (SEC-04). */
    private fun checkCertPinExpiry() {
        runCatching {
            val sdf      = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val expiry   = sdf.parse(BuildConfig.CERT_PIN_LEAF_EXPIRY) ?: return
            val daysLeft = ((expiry.time - Date().time) / 86_400_000L).toInt()
            if (daysLeft <= 30) {
                Timber.w("SEC-04: Certificate pin leaf expires in $daysLeft day(s)! Update CERT_PIN_LEAF in build.gradle.kts.")
            }
        }
    }

    @Provides @Singleton
    fun provideOkHttpClient(rateLimiter: RateLimiter): OkHttpClient {
        checkCertPinExpiry()
        return OkHttpClient.Builder()
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
    }

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
