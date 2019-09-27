package com.itsovertime.overtimecamera.play.di

import androidx.work.WorkerFactory
import com.itsovertime.overtimecamera.play.application.OTApplication
import com.itsovertime.overtimecamera.play.authmanager.AuthenticationManager
import com.itsovertime.overtimecamera.play.authmanager.AuthenticationManagerImpl
import com.itsovertime.overtimecamera.play.eventmanager.EventManager
import com.itsovertime.overtimecamera.play.eventmanager.EventManagerImpl
import com.itsovertime.overtimecamera.play.network.Api
import com.itsovertime.overtimecamera.play.network.JobBindingModule
import com.itsovertime.overtimecamera.play.network.NetworkSchedulerService
import com.itsovertime.overtimecamera.play.network.StaticApiModule
import com.itsovertime.overtimecamera.play.progress.ProgressManager
import com.itsovertime.overtimecamera.play.progress.ProgressManagerImpl
import com.itsovertime.overtimecamera.play.workmanager.DaggerWorkerFactory
import com.itsovertime.overtimecamera.play.uploadsmanager.UploadsManager
import com.itsovertime.overtimecamera.play.uploadsmanager.UploadsManagerImpl
import com.itsovertime.overtimecamera.play.videomanager.VideosManager
import com.itsovertime.overtimecamera.play.videomanager.VideosManagerImpl
import com.itsovertime.overtimecamera.play.wifimanager.WifiManager
import com.itsovertime.overtimecamera.play.wifimanager.WifiManagerImpl
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(includes = [StaticApiModule::class, JobBindingModule::class])
class AppModule {

    @Provides
    @Singleton
    fun provideVideosManager(
        context: OTApplication,
        manager: UploadsManager
    ): VideosManager {
        return VideosManagerImpl(context, manager)
    }


    @Provides
    @Singleton
    fun provideUploadManager(
        context: OTApplication,
        api: Api,
        wifiManager: WifiManager
    ): UploadsManager {
        return UploadsManagerImpl(context, api, wifiManager)
    }

    @Provides
    @Singleton
    fun provideWifiManager(context: OTApplication): WifiManager {
        return WifiManagerImpl(context)
    }

    @Provides
    @Singleton
    fun provideJobBinding(): NetworkSchedulerService {
        return NetworkSchedulerService()
    }


    @Provides
    @Singleton
    fun provideEventManager(api: Api): EventManager {
        return EventManagerImpl(api)
    }


    @Provides
    @Singleton
    fun provideAuthManager(context: OTApplication, api: Api): AuthenticationManager {
        return AuthenticationManagerImpl(context, api)
    }

    @Provides
    @Singleton
    fun workerFactory(
        uploads: UploadsManager,
        videos: VideosManager,
        progress: ProgressManager
    ): WorkerFactory {
        return DaggerWorkerFactory(
            uploads,
            videos,
            progress
        )
    }


    @Provides
    @Singleton
    fun provideProgressManager(context: OTApplication): ProgressManager {
        return ProgressManagerImpl(context)
    }

}
