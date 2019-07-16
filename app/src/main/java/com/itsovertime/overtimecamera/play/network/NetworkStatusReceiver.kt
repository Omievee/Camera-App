package com.itsovertime.overtimecamera.play.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo


class NetworkStatusReceiver(val context: Context) : BroadcastReceiver() {

    private var listener: ConnectivityReceiverListener? = null

    init {
        if (context is ConnectivityReceiverListener) {
            listener = context
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        listener?.onNetworkConnectionChanged(checkForWifi(context ?: return))
    }

    interface ConnectivityReceiverListener {
        fun onNetworkConnectionChanged(isConnected: NetworkInfo?)
    }


    private fun checkForWifi(context: Context): NetworkInfo? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo

        return activeNetwork
    }
}