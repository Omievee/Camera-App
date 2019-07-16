package com.itsovertime.overtimecamera.play.application

import android.app.Activity
import android.app.Application
import android.app.Service
import com.crashlytics.android.Crashlytics
import com.facebook.drawee.backends.pipeline.Fresco
import com.itsovertime.overtimecamera.play.BuildConfig
import com.itsovertime.overtimecamera.play.di.DaggerAppComponent
import com.itsovertime.overtimecamera.play.userpreference.UserPreference
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.HasServiceInjector
import io.fabric.sdk.android.Fabric
import javax.inject.Inject


class OTApplication : Application(), HasActivityInjector, HasServiceInjector {



    @Inject
    lateinit var activityDispatchingAndroidInjector: DispatchingAndroidInjector<Activity>

    @Inject
    lateinit var serviceInjector: DispatchingAndroidInjector<Service>

    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())
        Fresco.initialize(this)
        UserPreference.load(this)
        inject()
    }

    fun inject() {
        DaggerAppComponent
            .builder()
            .application(this)
            .build()
            .inject(this)
    }

    override fun activityInjector(): AndroidInjector<Activity> {
        return activityDispatchingAndroidInjector
    }
    override fun serviceInjector(): AndroidInjector<Service> {
        return serviceInjector
    }
}