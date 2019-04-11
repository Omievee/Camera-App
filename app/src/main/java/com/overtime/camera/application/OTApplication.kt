package com.overtime.camera.application

import dagger.android.AndroidInjector
import dagger.android.HasActivityInjector
import android.app.Activity
import android.app.Application
import com.overtime.camera.di.DaggerAppComponent
import dagger.android.DispatchingAndroidInjector
import javax.inject.Inject


class OTApplication : Application(), HasActivityInjector {

    @Inject
    lateinit var activityDispatchingAndroidInjector: DispatchingAndroidInjector<Activity>

    override fun onCreate() {
        super.onCreate()
        inject()
    }

    override fun activityInjector(): AndroidInjector<Activity> {
        return activityDispatchingAndroidInjector
    }

    protected fun inject() {
        DaggerAppComponent
            .builder()
            .application(this)
            .build()
            .inject(this)
    }


}