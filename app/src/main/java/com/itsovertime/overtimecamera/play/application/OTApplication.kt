package com.itsovertime.overtimecamera.play.application

import android.app.Activity
import android.app.Application
import com.crashlytics.android.Crashlytics
import com.facebook.drawee.backends.pipeline.BuildConfig
import com.facebook.drawee.backends.pipeline.Fresco
import com.itsovertime.overtimecamera.play.di.DaggerAppComponent
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import io.fabric.sdk.android.Fabric
import javax.inject.Inject


class OTApplication : Application(), HasActivityInjector {

    @Inject
    lateinit var activityDispatchingAndroidInjector: DispatchingAndroidInjector<Activity>

    override fun onCreate() {
        super.onCreate()
        if(!BuildConfig.DEBUG){
            Fabric.with(this, Crashlytics())
        }
        Fresco.initialize(this)
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


}