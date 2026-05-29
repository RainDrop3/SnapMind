package com.example.snapmind.di

import com.example.snapmind.BuildConfig
import com.example.snapmind.data.remote.gemini.GeminiApiService
import com.example.snapmind.data.remote.vision.VisionApiService
import com.example.snapmind.data.remote.youtube.YoutubeApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class VisionRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GeminiRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class YoutubeRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader("x-goog-api-key")
        }
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @VisionRetrofit
    fun provideVisionRetrofit(client: OkHttpClient): Retrofit =
        retrofit("https://vision.googleapis.com/", client)

    @Provides
    @Singleton
    @GeminiRetrofit
    fun provideGeminiRetrofit(client: OkHttpClient): Retrofit =
        retrofit("https://generativelanguage.googleapis.com/", client)

    @Provides
    @Singleton
    @YoutubeRetrofit
    fun provideYoutubeRetrofit(client: OkHttpClient): Retrofit =
        retrofit("https://www.googleapis.com/", client)

    @Provides
    @Singleton
    fun provideVisionApiService(
        @VisionRetrofit retrofit: Retrofit,
    ): VisionApiService = retrofit.create(VisionApiService::class.java)

    @Provides
    @Singleton
    fun provideGeminiApiService(
        @GeminiRetrofit retrofit: Retrofit,
    ): GeminiApiService = retrofit.create(GeminiApiService::class.java)

    @Provides
    @Singleton
    fun provideYoutubeApiService(
        @YoutubeRetrofit retrofit: Retrofit,
    ): YoutubeApiService = retrofit.create(YoutubeApiService::class.java)

    private fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
}
