package com.itsovertime.overtimecamera.play.uploads

import com.itsovertime.overtimecamera.play.di.ActivityScope
import com.itsovertime.overtimecamera.play.di.FragmentScope
import com.itsovertime.overtimecamera.play.progress.ProgressManager
import com.itsovertime.overtimecamera.play.uploadsmanager.UploadsManager
import com.itsovertime.overtimecamera.play.videomanager.VideosManager
import com.itsovertime.overtimecamera.play.wifimanager.WifiManager
import dagger.Module
import dagger.Provides


@Module
class UploadsModule {
    @Provides
    @ActivityScope
    fun providePresenter(
        fragment: UploadsActivity,
        manager: VideosManager,
        wifiManager: WifiManager,
        progress:ProgressManager
    ): UploadsPresenter =
        UploadsPresenter(fragment, manager, wifiManager, progress)
}

