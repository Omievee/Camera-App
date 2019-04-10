package com.overtime.camera.application

import com.overtime.camera.di.AppComponent
import dagger.android.AndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.support.DaggerApplication


class OTApplication : DaggerApplication(), HasActivityInjector {

    override fun onCreate() {
        super.onCreate()


    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return AppComponent.Builder.application(this).build()
    }
}