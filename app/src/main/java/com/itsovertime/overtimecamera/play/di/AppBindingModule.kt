package com.itsovertime.overtimecamera.play.di

import com.itsovertime.overtimecamera.play.baseactivity.BaseActivity
import com.itsovertime.overtimecamera.play.baseactivity.BaseActivityModule
import com.itsovertime.overtimecamera.play.camera.CameraFragModule
import com.itsovertime.overtimecamera.play.camera.CameraFragment
import com.itsovertime.overtimecamera.play.events.EventsFragment
import com.itsovertime.overtimecamera.play.events.Eventsmodule
import com.itsovertime.overtimecamera.play.onboarding.OnBoardingFragment
import com.itsovertime.overtimecamera.play.onboarding.OnBoardingModule
import com.itsovertime.overtimecamera.play.settings.SettingsFragment
import com.itsovertime.overtimecamera.play.settings.SettingsModule
import com.itsovertime.overtimecamera.play.uploads.UploadsFragment
import com.itsovertime.overtimecamera.play.uploads.UploadsModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
interface AppBindingModule {
    /**
     * Generates boilerplate
     */
    @ActivityScope
    @ContributesAndroidInjector(modules = [BaseActivityModule::class])
    fun baseActivity(): BaseActivity

    @FragmentScope
    @ContributesAndroidInjector(modules = [CameraFragModule::class])
    fun cameraFrag(): CameraFragment

    @FragmentScope
    @ContributesAndroidInjector(modules = [UploadsModule::class])
    fun uploadFrag(): UploadsFragment

    @FragmentScope
    @ContributesAndroidInjector(modules = [SettingsModule::class])
    fun settingsFrag(): SettingsFragment

    @FragmentScope
    @ContributesAndroidInjector(modules = [OnBoardingModule::class])
    fun onboardingFrag(): OnBoardingFragment

    @FragmentScope
    @ContributesAndroidInjector(modules = [Eventsmodule::class])
    fun eventsFrag(): EventsFragment
}