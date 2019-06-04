package com.itsovertime.overtimecamera.play.di

import com.itsovertime.overtimecamera.play.network.Api
import com.itsovertime.overtimecamera.play.network.StaticApiModule
import com.itsovertime.overtimecamera.play.uploadsmanager.UploadsManager
import com.itsovertime.overtimecamera.play.uploadsmanager.UploadsManagerImpl
import com.itsovertime.overtimecamera.play.videomanager.VideosManager
import com.itsovertime.overtimecamera.play.videomanager.VideosManagerImpl
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(includes = [StaticApiModule::class])
class AppModule {

    @Provides
    @Singleton
    fun provideVideosManager(manager: UploadsManager): VideosManager {
        return VideosManagerImpl(manager)
    }

    @Provides
    @Singleton
    fun provideUploadManager(api: Api): UploadsManager {
        return UploadsManagerImpl(api)
    }
}
