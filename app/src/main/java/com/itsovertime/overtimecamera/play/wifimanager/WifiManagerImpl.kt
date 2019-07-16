package com.itsovertime.overtimecamera.play.wifimanager

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.itsovertime.overtimecamera.play.application.OTApplication
import com.itsovertime.overtimecamera.play.uploadsmanager.UploadsManager

class WifiManagerImpl(val context: OTApplication, val manager:UploadsManager) : WifiManager {

    override fun onDetectWifi(): Boolean {
        return when (activeNetwork?.type) {
            ConnectivityManager.TYPE_WIFI -> true
            else -> false
        }
    }

    override fun onDetectNetworkReliability() {

    }

    private var cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var activeNetwork: NetworkInfo? = cm.activeNetworkInfo

    override fun onDetectNetworkStatus(): Boolean {
        return activeNetwork?.isConnected ?: false
    }

    override fun onNoNetworkDetected() {
        println("WORKS")
    }

    override fun onWeakNetworkConnection() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}