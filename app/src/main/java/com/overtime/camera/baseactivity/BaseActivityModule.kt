package com.overtime.camera.baseactivity

import com.overtime.camera.di.ActivityScope
import dagger.Module
import dagger.Provides


@Module
class BaseActivityModule {

    @Provides
    @ActivityScope
    fun provideVM(baseActivity: BaseActivity): BaseActivityVM = BaseActivityVM(baseActivity)
}