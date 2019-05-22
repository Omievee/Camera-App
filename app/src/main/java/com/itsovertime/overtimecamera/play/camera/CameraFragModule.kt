package com.itsovertime.overtimecamera.play.camera

import com.itsovertime.overtimecamera.play.di.FragmentScope
import com.itsovertime.overtimecamera.play.videomanager.VideosManager
import dagger.Module
import dagger.Provides

@Module
class CameraFragModule {
    @Provides
    @FragmentScope
    fun providePresenter(fragment: CameraFragment, manager: VideosManager): CameraPresenter =
        CameraPresenter(fragment, manager)
}




//TODO: Fix adapter being updated.... See why wont save local... Clipping Recyclerview children...