package com.itsovertime.overtimecamera.play.camera

import com.itsovertime.overtimecamera.play.authmanager.AuthenticationManager
import com.itsovertime.overtimecamera.play.di.FragmentScope
import com.itsovertime.overtimecamera.play.eventmanager.EventManager
import com.itsovertime.overtimecamera.play.videomanager.VideosManager
import dagger.Module
import dagger.Provides

@Module
class CameraFragModule {

    @Provides
    @FragmentScope
    fun providePresenter(
        fragment: CameraFragment,
        manager: VideosManager,
        eventManager: EventManager,
        authManager: AuthenticationManager
    ): CameraPresenter =
        CameraPresenter(fragment, manager, authManager, eventManager)
}



