package com.itsovertime.overtimecamera.play.network

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.itsovertime.overtimecamera.play.BuildConfig
import com.itsovertime.overtimecamera.play.application.OTApplication
import com.itsovertime.overtimecamera.play.utils.Constants
import com.squareup.moshi.*
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import okhttp3.Cache
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlin.math.max


@Module
class StaticApiModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        cache: Cache
    ): OkHttpClient.Builder {
        return OkHttpClient.Builder().apply {
            addInterceptor(AuthenticatedNetworkInterceptor())
            if (BuildConfig.DEBUG) {
                val logging = HttpLoggingInterceptor()
                logging.level = HttpLoggingInterceptor.Level.BODY
                this.addInterceptor(logging)
            }
            connectTimeout(1, TimeUnit.MINUTES)
            writeTimeout(1, TimeUnit.MINUTES)
            readTimeout(1, TimeUnit.MINUTES)
            dispatcher(Dispatcher().apply {
                maxRequests = 3
            })
            cache(cache)
        }
    }

    @Provides
    @Singleton
    @FromJson
    fun provideMoshi(): Moshi {
        return Moshi.Builder().apply {
            add(KotlinJsonAdapterFactory())
            add(Date::class.java, Rfc3339DateJsonAdapter())
            add(object {
                @ToJson
                fun toJson(uuid: UUID) = uuid.toString()

                @FromJson
                fun fromJson(s: String) = UUID.fromString(s)
            })
        }.build()

    }


    @Provides
    @Singleton
    fun provideApi(client: OkHttpClient.Builder, moshi: Moshi): Api {
        return Retrofit.Builder()
            .baseUrl(Constants.mainUploadURL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createAsync())
            .client(client.build())
            .build()
            .create(Api::class.java)
    }

    @Provides
    @Singleton
    fun provideCache(application: OTApplication): Cache {
        return Cache(application.cacheDir, 20 * 1024 * 1024)
    }

    @Provides
    @Singleton
    fun provideAuthenticatedRequestInterceptor(): AuthenticatedNetworkInterceptor {
        return AuthenticatedNetworkInterceptor()
    }

}

