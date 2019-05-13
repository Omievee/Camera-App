package com.overtime.camera.uploads

import com.overtime.camera.di.FragmentScope
import com.overtime.camera.videomanager.VideosManager
import dagger.Module
import dagger.Provides

@Module
class UploadsModule {
    @Provides
    @FragmentScope
    fun providePresenter(fragment: UploadsFragment, manager: VideosManager): UploadsPresenter = UploadsPresenter(fragment, manager)
}

