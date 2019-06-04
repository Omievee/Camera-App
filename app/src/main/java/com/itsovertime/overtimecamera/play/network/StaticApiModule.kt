package com.itsovertime.overtimecamera.play.network

import android.app.Application
import com.itsovertime.overtimecamera.play.BuildConfig
import com.itsovertime.overtimecamera.play.application.OTApplication
import com.itsovertime.overtimecamera.play.utils.Constants
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Singleton


@Module
class StaticApiModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        cache: Cache
    ): OkHttpClient.Builder {
        val httpClient = OkHttpClient.Builder()
        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY
            httpClient.addInterceptor(logging)
        }

        httpClient.connectTimeout(30, TimeUnit.SECONDS)
        httpClient.readTimeout(30, TimeUnit.SECONDS)
        httpClient.cache(cache)
        return httpClient
    }


    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        var m: Moshi = Moshi.Builder()
            .add(Date::class.java, com.squareup.moshi.adapters.Rfc3339DateJsonAdapter())
            .build()
        return m
    }


    @Provides
    @Singleton
    fun provideApi(client: OkHttpClient.Builder, moshi: Moshi): Api {
        return Retrofit.Builder()
            .baseUrl(Constants.videoUploadUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(client.build())
            .build()
            .create(Api::class.java)
    }

    @Provides
    @Singleton
    fun provideCache(application: OTApplication): Cache {
        return Cache(application.cacheDir, 10 * 1024 * 1024)
    }

}