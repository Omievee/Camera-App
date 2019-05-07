package com.overtime.camera.camera

import com.overtime.camera.di.FragmentScope
import dagger.Module
import dagger.Provides

@Module
class CameraFragModule {
    @Provides
    @FragmentScope
    fun providePresenter(fragment: CameraFragment): CameraPresenter = CameraPresenter(fragment)
}

