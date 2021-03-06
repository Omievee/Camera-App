package com.itsovertime.overtimecamera.play.di

import com.itsovertime.overtimecamera.play.baseactivity.BaseActivity
import com.itsovertime.overtimecamera.play.baseactivity.BaseActivityModule
import com.itsovertime.overtimecamera.play.camera.CameraFragModule
import com.itsovertime.overtimecamera.play.camera.CameraFragment
import com.itsovertime.overtimecamera.play.onboarding.OnBoardingFragment
import com.itsovertime.overtimecamera.play.onboarding.OnBoardingModule
import com.itsovertime.overtimecamera.play.onboarding.OnboardingActivity
import com.itsovertime.overtimecamera.play.settings.SettingsFragment
import com.itsovertime.overtimecamera.play.settings.SettingsModule
import com.itsovertime.overtimecamera.play.splashscreen.SplashActivity
import com.itsovertime.overtimecamera.play.splashscreen.SplashModule
import com.itsovertime.overtimecamera.play.uploads.UploadsActivity
import com.itsovertime.overtimecamera.play.uploads.UploadsModule
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector

@Module
interface AppBindingModule {
    /**
     * Generates boilerplate
     */
    @ActivityScope
    @ContributesAndroidInjector(modules = [BaseActivityModule::class])
    fun baseActivity(): BaseActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [SplashModule::class])
    fun splashActivity(): SplashActivity

    @ActivityScope
    @ContributesAndroidInjector
    fun onboardActivity(): OnboardingActivity

    @FragmentScope
    @ContributesAndroidInjector(modules = [CameraFragModule::class])
    fun cameraFrag(): CameraFragment


    @ActivityScope
    @ContributesAndroidInjector(modules = [UploadsModule::class])
    fun uploadFrag(): UploadsActivity

    @FragmentScope
    @ContributesAndroidInjector(modules = [SettingsModule::class])
    fun settingsFrag(): SettingsFragment

    @FragmentScope
    @ContributesAndroidInjector(modules = [OnBoardingModule::class])
    fun onboardingFrag(): OnBoardingFragment


}