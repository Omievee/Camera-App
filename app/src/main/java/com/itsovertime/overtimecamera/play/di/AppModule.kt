package com.itsovertime.overtimecamera.play.di

import android.app.Application
import com.itsovertime.overtimecamera.play.network.Api
import com.itsovertime.overtimecamera.play.network.ApiModule
import com.itsovertime.overtimecamera.play.videomanager.VideosManager
import com.itsovertime.overtimecamera.play.videomanager.VideosManagerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(includes = [ApiModule::class])
class AppModule {

    @Provides
    @Singleton
    fun provideVideosManager(api: Api): VideosManager {
        return VideosManagerImpl(api = api)
    }
}
