package com.itsovertime.overtimecamera.play.network

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.widget.Toast


class NetworkSchedulerService : JobService(), NetworkStatusReceiver.ConnectivityReceiverListener {
    private var receiver: NetworkStatusReceiver? = null

    override fun onCreate() {
        super.onCreate()
        receiver = NetworkStatusReceiver(this)
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        unregisterReceiver(receiver);
        return true;
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        println("start job........")
        registerReceiver(receiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        return true
    }

    override fun onNetworkConnectionChanged(isConnected: Boolean) {
        val message = if (isConnected) "Good! Connected to Internet" else "Sorry! Not connected to internet"
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }
}