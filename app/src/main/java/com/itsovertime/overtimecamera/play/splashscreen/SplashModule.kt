package com.itsovertime.overtimecamera.play.splashscreen

import com.itsovertime.overtimecamera.play.authmanager.AuthenticationManager
import com.itsovertime.overtimecamera.play.di.ActivityScope
import dagger.Module
import dagger.Provides


@Module
class SplashModule {
    @Provides
    @ActivityScope
    fun provideSplash(activity: SplashActivity, manager: AuthenticationManager): SplashPresenter =
        SplashPresenter(activity, manager)

}