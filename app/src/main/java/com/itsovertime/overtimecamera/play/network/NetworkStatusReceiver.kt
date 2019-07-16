package com.itsovertime.overtimecamera.play.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager


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
        fun onNetworkConnectionChanged(isConnected: Boolean)
    }


    private fun checkForWifi(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        println("Network Type:::::: ${activeNetwork?.type}")
        return when (activeNetwork?.type) {
            ConnectivityManager.TYPE_WIFI -> true
            else -> false
        }
    }
}