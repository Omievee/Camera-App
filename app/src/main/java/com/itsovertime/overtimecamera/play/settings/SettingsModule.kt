package com.itsovertime.overtimecamera.play.settings

import com.itsovertime.overtimecamera.play.authmanager.AuthenticationManager
import com.itsovertime.overtimecamera.play.di.FragmentScope
import dagger.Module
import dagger.Provides

@Module
class SettingsModule {
    @Provides
    @FragmentScope
    fun providePresenter(fragment: SettingsFragment, auth:AuthenticationManager): SettingsPresenter = SettingsPresenter(fragment, auth)
}
