package com.overtime.camera.di

import android.app.Application
import android.content.Context
import com.overtime.camera.db.AppDatabase
import com.overtime.camera.videomanager.VideosManager
import com.overtime.camera.videomanager.VideosManagerImpl
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

//@Module(includes = [StaticApiModule::class, TheaterModule::class, TheaterModuleUI::class, HistoryModule::class, LocationModule::class, TicketModule::class, GoWatchItModule::class, UploadModule::class, DatabaseModule::class])
@Module
class AppModule {

    @Provides
    @Singleton
    fun provideoVideosManager(): VideosManager {
        return VideosManagerImpl()
    }
}