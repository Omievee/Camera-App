package com.itsovertime.overtimecamera.play.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import com.itsovertime.overtimecamera.play.network.NetworkStatusReceiver.ConnectivityReceiverListener


class NetworkStatusReceiver(context: Context) : BroadcastReceiver() {

    private var listener: ConnectivityReceiverListener? = null

    init {
        if (context is ConnectivityReceiverListener) {
            listener = context
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        println("receive... $listener")
        isConnected(context ?: return)


        listener?.onNetworkConnectionChanged(isConnected(context ?: return))
    }

    interface ConnectivityReceiverListener {
        fun onNetworkConnectionChanged(isConnected: Boolean)
    }

    fun connectivityReceiver(listener: ConnectivityReceiverListener) {
        this.listener = listener
    }

    fun isConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting
    }

}