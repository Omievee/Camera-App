package com.itsovertime.overtimecamera.play.network

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.widget.Toast
import com.itsovertime.overtimecamera.play.baseactivity.OTActivity
import com.itsovertime.overtimecamera.play.wifimanager.WifiManager
import dagger.android.AndroidInjection
import javax.inject.Inject


class NetworkSchedulerService : JobService(), NetworkStatusReceiver.ConnectivityReceiverListener {

    private var receiver: NetworkStatusReceiver? = null

    override fun onCreate() {
        super.onCreate()
        AndroidInjection.inject(this)
        receiver = NetworkStatusReceiver(this)
    }

    @Inject
    lateinit var listener: WifiManager

    override fun onStopJob(params: JobParameters?): Boolean {
        unregisterReceiver(receiver);
        return true
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        registerReceiver(receiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        return true
    }

    override fun onNetworkConnectionChanged(networkInfo: NetworkInfo?) {
        listener.onReceiveNetworkInfoFromBroadcast( networkInfo)
    }
}