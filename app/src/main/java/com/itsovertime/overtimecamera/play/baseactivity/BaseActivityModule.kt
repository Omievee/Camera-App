package com.itsovertime.overtimecamera.play.baseactivity

import com.itsovertime.overtimecamera.play.analytics.OTAnalyticsManager
import com.itsovertime.overtimecamera.play.authmanager.AuthenticationManager
import com.itsovertime.overtimecamera.play.di.ActivityScope
import dagger.Module
import dagger.Provides


@Module
class BaseActivityModule {

    @Provides
    @ActivityScope
    fun provideVM(
        baseActivity: BaseActivity,
        manager: AuthenticationManager,
        analytics:OTAnalyticsManager
    ): BaseActivityPresenter = BaseActivityPresenter(baseActivity, manager, analytics)
}