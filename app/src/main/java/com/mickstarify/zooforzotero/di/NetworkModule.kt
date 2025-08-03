package com.mickstarify.zooforzotero.di

import com.mickstarify.zooforzotero.BuildConfig
import com.mickstarify.zooforzotero.ZoteroAPI.BASE_URL
import com.mickstarify.zooforzotero.ZoteroAPI.ZoteroAPIService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UnauthenticatedApiService

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @UnauthenticatedApiService
    fun provideUnauthenticatedZoteroApiService(): ZoteroAPIService {
        val httpClient = OkHttpClient.Builder().apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
            }
            // Add version header but NO API key for validation calls
            addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Zotero-API-Version", "3")
                    .build()
                chain.proceed(request)
            }
        }.build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ZoteroAPIService::class.java)
    }
}