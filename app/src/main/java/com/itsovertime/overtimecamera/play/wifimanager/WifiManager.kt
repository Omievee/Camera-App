package com.itsovertime.overtimecamera.play.wifimanager

import android.net.NetworkInfo
import io.reactivex.Observable

interface WifiManager {


    fun onNoNetworkDetected()
    fun onWeakNetworkConnection()
    fun onDetectNetworkReliability()
    fun onReceiveNetworkInfoFromBroadcast(networkInfo: NetworkInfo?)
    fun subscribeToNetworkUpdates(): Observable<NETWORK_TYPE>
}