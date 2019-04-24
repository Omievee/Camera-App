package com.overtime.camera.network

import android.app.Application
import com.overtime.camera.BuildConfig
import com.overtime.camera.utils.Constants
import com.squareup.moshi.Moshi
import com.squareup.moshi.Rfc3339DateJsonAdapter
import dagger.Module
import dagger.Provides
import okhttp3.Cache
import okhttp3.CookieJar
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Singleton


@Module
class ApiModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        cookieJar: CookieJar,
        loggingInterceptor: HttpLoggingInterceptor,
        authenticatedRequestInterceptor: Interceptor,
        cache: Cache
    ): OkHttpClient.Builder {
        val httpClient = OkHttpClient.Builder()
        httpClient.connectTimeout(40, TimeUnit.SECONDS)
        httpClient.readTimeout(40, TimeUnit.SECONDS)
        httpClient.cookieJar(cookieJar)
        httpClient.addInterceptor(authenticatedRequestInterceptor)
        httpClient.addInterceptor(loggingInterceptor)
        return httpClient
    }


    @Provides
    @Singleton
    fun provideHttpLogging(): HttpLoggingInterceptor {
        val logging = HttpLoggingInterceptor()
        logging.level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        return logging
    }


    @Provides
    @Singleton
    fun provideMoshi(): Moshi.Builder {
        lateinit var moshi2: Moshi
        return Moshi.Builder()
            .add(Date::class.java, Rfc3339DateJsonAdapter())
    }

    @Provides
    @Singleton
    fun provideCache(application: Application): Cache {
        return Cache(application.cacheDir, 10 * 1024 * 1024)
    }

    @Provides
    @Singleton
    fun provideApi(client: OkHttpClient.Builder, moshi: Moshi): Api {
        return Retrofit.Builder()
            .baseUrl(Constants.OT_BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(client.build())
            .build()
            .create(Api::class.java)
    }


}