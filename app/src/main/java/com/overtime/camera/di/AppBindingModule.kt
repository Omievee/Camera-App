package com.overtime.camera.di

import com.overtime.camera.baseactivity.BaseActivity
import com.overtime.camera.baseactivity.BaseActivityModule
import com.overtime.camera.camera.CameraFragment
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
    @ContributesAndroidInjector
    fun camereaFrag(): CameraFragment
}