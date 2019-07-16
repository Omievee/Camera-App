package com.itsovertime.overtimecamera.play.uploads

import com.itsovertime.overtimecamera.play.di.FragmentScope
import com.itsovertime.overtimecamera.play.videomanager.VideosManager
import com.itsovertime.overtimecamera.play.wifimanager.WifiManager
import dagger.Module
import dagger.Provides

@Module
class UploadsModule {
    @Provides
    @FragmentScope
    fun providePresenter(fragment: UploadsFragment, manager: VideosManager, wifiManager: WifiManager): UploadsPresenter = UploadsPresenter(fragment, manager, wifiManager)
}

