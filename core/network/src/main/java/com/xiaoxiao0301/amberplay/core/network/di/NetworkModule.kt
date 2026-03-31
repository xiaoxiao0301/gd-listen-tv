package com.xiaoxiao0301.amberplay.core.network.di

import com.xiaoxiao0301.amberplay.core.network.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.xiaoxiao0301.amberplay.core.network.api.MusicApiService
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
     *
     * We pin the **intermediate CA** (not the leaf certificate) so that no app update is needed
     * when music-api.gdstudio.xyz rotates its leaf cert.
     *
     * The CA pin is stored in BuildConfig.CERT_PIN_CA (build.gradle.kts) and changes only when
     * the CA itself is replaced — typically on a multi-year timescale.
     *
     * To verify the current CA pin:
     *   openssl s_client -connect music-api.gdstudio.xyz:443 -servername music-api.gdstudio.xyz \
     *     -showcerts 2>/dev/null | awk '/-----BEGIN CERTIFICATE-----/{n++} n==2{print}' | \
     *     openssl x509 -pubkey -noout | openssl pkey -pubin -outform der 2>/dev/null | \
     *     openssl dgst -sha256 -binary | base64
     *
     * Future self-hosted deployment: replace CERT_PIN_CA with the pin of your own private root CA.
     * Rotate certs freely without ever updating this value.
     */
    private val certificatePinner = CertificatePinner.Builder()
        .add("music-api.gdstudio.xyz", BuildConfig.CERT_PIN_CA)
        .build()

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
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
