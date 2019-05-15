package com.overtime.camera.di

import com.overtime.camera.baseactivity.BaseActivity
import com.overtime.camera.baseactivity.BaseActivityModule
import com.overtime.camera.camera.CameraFragModule
import com.overtime.camera.camera.CameraFragment
import com.overtime.camera.settings.SettingsFragment
import com.overtime.camera.settings.SettingsModule
import com.overtime.camera.uploads.UploadsFragment
import com.overtime.camera.uploads.UploadsModule
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
}