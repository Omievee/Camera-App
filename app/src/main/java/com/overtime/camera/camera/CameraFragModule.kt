package com.overtime.camera.camera

import com.overtime.camera.di.FragmentScope
import com.overtime.camera.videomanager.VideosManager
import dagger.Module
import dagger.Provides

@Module
class CameraFragModule {
    @Provides
    @FragmentScope
    fun providePresenter(fragment: CameraFragment, manager: VideosManager): CameraPresenter =
        CameraPresenter(fragment, manager)
}

