package com.overtime.camera.uploads

import com.overtime.camera.di.FragmentScope
import dagger.Module
import dagger.Provides

@Module
class UploadsModule {
    @Provides
    @FragmentScope
    fun providePresenter(fragment: UploadsFragment): UploadsPresenter = UploadsPresenter(fragment)
}

