package com.itsovertime.overtimecamera.play.di

import android.app.Application
import com.itsovertime.overtimecamera.play.videomanager.VideosManager
import com.itsovertime.overtimecamera.play.videomanager.VideosManagerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule {

    @Provides
    @Singleton
    fun provideoVideosManager(): VideosManager {
        return VideosManagerImpl()
    }
}
