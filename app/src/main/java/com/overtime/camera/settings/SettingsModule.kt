package com.overtime.camera.settings

import com.overtime.camera.di.FragmentScope
import dagger.Module
import dagger.Provides

@Module
class SettingsModule {
    @Provides
    @FragmentScope
    fun providePresenter(fragment: SettingsFragment): SettingsPresenter = SettingsPresenter(fragment)
}
