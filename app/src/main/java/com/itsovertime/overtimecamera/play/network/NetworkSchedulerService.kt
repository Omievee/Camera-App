package com.itsovertime.overtimecamera.play.network

import android.app.job.JobParameters
import android.app.job.JobService

class NetworkSchedulerService : JobService(), NetworkStatusReceiver.ConnectivityReceiverListener {
    override fun onStopJob(params: JobParameters?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onNetworkConnectionChanged(isConnected: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}