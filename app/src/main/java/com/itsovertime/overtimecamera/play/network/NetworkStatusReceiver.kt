package com.itsovertime.overtimecamera.play.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NetworkStatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

    }

    interface ConnectivityReceiverListener {
        fun onNetworkConnectionChanged(isConnected: Boolean)
    }
}