package com.itsovertime.overtimecamera.play.onboarding

import com.itsovertime.overtimecamera.play.camera.CameraFragment
import com.itsovertime.overtimecamera.play.camera.CameraPresenter
import com.itsovertime.overtimecamera.play.di.FragmentScope
import com.itsovertime.overtimecamera.play.videomanager.VideosManager
import dagger.Module
import dagger.Provides


@Module
class OnBoardingModule {

    @Provides
    @FragmentScope
    fun providePresenter(fragment: OnBoardingFragment): OnBoardingPresenter =
            OnBoardingPresenter(fragment)
}