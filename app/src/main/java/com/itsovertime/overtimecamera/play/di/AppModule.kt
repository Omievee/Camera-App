package com.itsovertime.overtimecamera.play.di

import androidx.work.WorkerFactory
import com.itsovertime.overtimecamera.play.analytics.OTAnalyticsManager
import com.itsovertime.overtimecamera.play.analytics.OTAnalyticsManagerImpl
import com.itsovertime.overtimecamera.play.application.OTApplication
import com.itsovertime.overtimecamera.play.authmanager.AuthenticationManager
import com.itsovertime.overtimecamera.play.authmanager.AuthenticationManagerImpl
import com.itsovertime.overtimecamera.play.eventmanager.EventManager
import com.itsovertime.overtimecamera.play.eventmanager.EventManagerImpl
import com.itsovertime.overtimecamera.play.filemanager.FileManager
import com.itsovertime.overtimecamera.play.filemanager.FileManagerImpl
import com.itsovertime.overtimecamera.play.network.Api
import com.itsovertime.overtimecamera.play.network.JobBindingModule
import com.itsovertime.overtimecamera.play.network.NetworkSchedulerService
import com.itsovertime.overtimecamera.play.network.StaticApiModule
import com.itsovertime.overtimecamera.play.notifications.NotificationManager
import com.itsovertime.overtimecamera.play.notifications.NotificationManagerImpl
import com.itsovertime.overtimecamera.play.progressmanager.ProgressManager
import com.itsovertime.overtimecamera.play.progressmanager.ProgressManagerImpl
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
        manager: UploadsManager,
        wifi: WifiManager,
        analytics: OTAnalyticsManager
    ): VideosManager {
        return VideosManagerImpl(
            context, manager, wifi, analytics
        )
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
    fun provideEventManager(api: Api, context: OTApplication): EventManager {
        return EventManagerImpl(api, context)
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
        progress: ProgressManager,
        notifications: NotificationManager,
        analytics: OTAnalyticsManager,
        wifi:WifiManager,
        file: FileManager
    ): WorkerFactory {
        return DaggerWorkerFactory(
            uploads,
            videos,
            progress,
            notifications,
            analytics,
            wifi,
            file
        )
    }

    @Provides
    @Singleton
    fun provideProgressManager(context: OTApplication): ProgressManager {
        return ProgressManagerImpl(context)
    }


    @Provides
    @Singleton
    fun provideNotificationManager(context: OTApplication): NotificationManager {
        return NotificationManagerImpl(context)
    }


    @Provides
    @Singleton
    fun provideAnalyticsManager(context: OTApplication, api: Api): OTAnalyticsManager {
        return OTAnalyticsManagerImpl(context, api)
    }

    @Provides
    @Singleton
    fun provideFileManager(context: OTApplication): FileManager {
        return FileManagerImpl(context)
    }
}
