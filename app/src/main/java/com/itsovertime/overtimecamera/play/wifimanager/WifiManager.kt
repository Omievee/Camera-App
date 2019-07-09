package com.itsovertime.overtimecamera.play.wifimanager

interface WifiManager {


    fun onDetectNetworkStatus() : Boolean
    fun onNoNetworkDetected()
    fun onWeakNetworkConnection()
    fun onDetectNetworkReliability()
    fun onDetectWifi() : Boolean



}