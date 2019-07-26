package com.itsovertime.overtimecamera.play.network

import com.itsovertime.overtimecamera.play.BuildConfig
import com.itsovertime.overtimecamera.play.application.OTApplication
import com.itsovertime.overtimecamera.play.utils.Constants
import com.squareup.moshi.*
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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
        return OkHttpClient.Builder().apply {
            addInterceptor(AuthenticatedNetworkInterceptor())
            if (BuildConfig.DEBUG) {
                val logging = HttpLoggingInterceptor()
                logging.level = HttpLoggingInterceptor.Level.BODY
                this.addInterceptor(logging)
            }
            connectTimeout(30, TimeUnit.SECONDS)
            readTimeout(30, TimeUnit.SECONDS)
            cache(cache)
        }
    }


    @Provides
    @Singleton
    @FromJson
    fun provideMoshi(): Moshi {
        return Moshi.Builder().apply {
            add(Date::class.java, Rfc3339DateJsonAdapter())
            add(KotlinJsonAdapterFactory())
            add(object {
                @ToJson
                fun toJson(uuid: UUID) = uuid.toString()
                @FromJson
                fun fromJson(s: String) = UUID.fromString(s)
            })
        }
            .build()

    }


    @Provides
    @Singleton
    fun provideApi(client: OkHttpClient.Builder, moshi: Moshi): Api {
        return Retrofit.Builder()
            .baseUrl(Constants.mainUploadURL)
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

    @Provides
    @Singleton
    fun provideAuthenticatedRequestInterceptor(): AuthenticatedNetworkInterceptor {
        return AuthenticatedNetworkInterceptor()
    }

}

//
//class UUIDAdapter : JsonAdapter<UUID>() {
//
//    @Synchronized
//    override fun fromJson(reader: JsonReader): UUID? {
//        val uuid = reader.nextString()
//        return uuid
//    }
//
//
//    override fun toJson(writer: JsonWriter, value: UUID?) {
//        writer.value()
//    }
//
//}