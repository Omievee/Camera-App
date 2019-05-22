package com.itsovertime.overtimecamera.play.baseactivity

import com.itsovertime.overtimecamera.play.di.ActivityScope
import dagger.Module
import dagger.Provides


@Module
class BaseActivityModule {

    @Provides
    @ActivityScope
    fun provideVM(baseActivity: BaseActivity): BaseActivityPresenter = BaseActivityPresenter(baseActivity)
}