package com.itsovertime.overtimecamera.play.application

import android.app.Activity
import android.app.Application
import android.app.Service
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import com.crashlytics.android.Crashlytics
import com.facebook.drawee.backends.pipeline.Fresco
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.itsovertime.overtimecamera.play.BuildConfig
import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.deviceid.DeviceId
import com.itsovertime.overtimecamera.play.di.DaggerAppComponent
import com.itsovertime.overtimecamera.play.userpreference.UserPreference
import com.mixpanel.android.mpmetrics.MixpanelAPI
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.HasServiceInjector
import io.fabric.sdk.android.Fabric
import io.reactivex.plugins.RxJavaPlugins
import org.json.JSONObject
import javax.inject.Inject


class OTApplication : Application(), HasActivityInjector, HasServiceInjector {


    @Inject
    lateinit var activityDispatchingAndroidInjector: DispatchingAndroidInjector<Activity>

    @Inject
    lateinit var workerFactory: WorkerFactory


    @Inject
    lateinit var serviceInjector: DispatchingAndroidInjector<Service>

    var token: String? = null
    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())
        Fresco.initialize(this)
        UserPreference.load(this)
        inject()
        RxJavaPlugins.setErrorHandler { throwable ->
            println("throws ::::::::::: ${throwable.cause}")
            println("throws ::::::::::: ${throwable.message}")
            println("throws ::::::::::: ${throwable.printStackTrace()}")
        }

        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("", "getInstanceId failed", task.exception)
                    return@OnCompleteListener
                }
                token = task.result?.token.toString()
            })
        val props = JSONObject()
        props.put("token", token)
        props.put("distinct_id", DeviceId.getID(this))
        val key = when (BuildConfig.DEBUG) {
            true -> getString(R.string.MXP_TOKEN_BETA)
            else -> getString(R.string.MXP_TOKEN)
        }
        MixpanelAPI.getInstance(this, key).registerSuperPropertiesOnce(props)
        MixpanelAPI.getInstance(this, key).people.identify(
            DeviceId.getID(
                this
            )
        )
        configureWorkManager()
    }

    private fun configureMixPanel() {

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


    private fun configureWorkManager() {
        val config = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

        WorkManager.initialize(this, config)
    }
}