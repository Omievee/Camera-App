package com.itsovertime.overtimecamera.play.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import com.itsovertime.overtimecamera.play.baseactivity.OTActivity
import com.itsovertime.overtimecamera.play.wifimanager.WifiManager
import javax.inject.Inject


class NetworkStatusReceiver(val context: Context) : BroadcastReceiver() {

    private var listener: ConnectivityReceiverListener? = null


    init {
        if (context is ConnectivityReceiverListener) {
            listener = context
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        println("onReceive...")
        listener?.onNetworkConnectionChanged(isConnected(context ?: return))
    }

    interface ConnectivityReceiverListener {
        fun onNetworkConnectionChanged(isConnected: Boolean)
    }


    private fun isConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo

        return activeNetwork != null && activeNetwork.isConnectedOrConnecting
    }
}