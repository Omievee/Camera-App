package com.itsovertime.overtimecamera.play.uploads

import com.itsovertime.overtimecamera.play.di.FragmentScope
import com.itsovertime.overtimecamera.play.videomanager.VideosManager
import dagger.Module
import dagger.Provides

@Module
class UploadsModule {
    @Provides
    @FragmentScope
    fun providePresenter(fragment: UploadsFragment, manager: VideosManager): UploadsPresenter = UploadsPresenter(fragment, manager)
}

