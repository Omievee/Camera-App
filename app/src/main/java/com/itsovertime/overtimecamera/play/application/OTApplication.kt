package com.itsovertime.overtimecamera.play.application

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import com.crashlytics.android.Crashlytics
import com.facebook.drawee.backends.pipeline.BuildConfig
import com.facebook.drawee.backends.pipeline.Fresco
import com.itsovertime.overtimecamera.play.di.DaggerAppComponent
import com.itsovertime.overtimecamera.play.network.NetworkStatusReceiver
import com.itsovertime.overtimecamera.play.userpreference.UserPreference
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import io.fabric.sdk.android.Fabric
import javax.inject.Inject
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import com.itsovertime.overtimecamera.play.network.NetworkSchedulerService


class OTApplication : Application(), HasActivityInjector {

    @Inject
    lateinit var activityDispatchingAndroidInjector: DispatchingAndroidInjector<Activity>



    override fun onCreate() {
        super.onCreate()
        if (!BuildConfig.DEBUG) {
            Fabric.with(this, Crashlytics())
        }
        Fresco.initialize(this)
        inject()
        UserPreference.load(this)

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