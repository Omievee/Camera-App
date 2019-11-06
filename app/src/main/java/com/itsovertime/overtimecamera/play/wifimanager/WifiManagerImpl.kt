package com.itsovertime.overtimecamera.play.wifimanager

import android.net.NetworkInfo
import android.telephony.TelephonyManager
import com.itsovertime.overtimecamera.play.application.OTApplication
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

class WifiManagerImpl(val context: OTApplication) : WifiManager {

    override fun subscribeToNetworkUpdates(): Observable<NETWORK_TYPE> {
        return subject
            .subscribeOn(Schedulers.single())
            .observeOn(AndroidSchedulers.mainThread())
    }


    private val subject: BehaviorSubject<NETWORK_TYPE> = BehaviorSubject.create()
    override fun onReceiveNetworkInfoFromBroadcast(networkInfo: NetworkInfo?) {
        when (networkInfo) {
            null -> onNoNetworkDetected()
            else -> determineNetworkType(networkInfo)
        }
    }

    private var networkType: NETWORK_TYPE? = null
    private fun determineNetworkType(networkInfo: NetworkInfo) {
        println("Network Info is.... ${networkInfo.isConnected}")
        networkType = if (networkInfo.isConnected && networkInfo.subtype == 0) {
            NETWORK_TYPE.WIFI
        } else if (networkInfo.isConnected && networkInfo.subtype == TelephonyManager.NETWORK_TYPE_LTE) {
            NETWORK_TYPE.MOBILE_LTE
        } else if (networkInfo.isConnected && networkInfo.subtype == TelephonyManager.NETWORK_TYPE_EDGE) {
            NETWORK_TYPE.MOBILE_EDGE
        } else {
            NETWORK_TYPE.UNKNOWN
        }

        subject.onNext(networkType ?: return)
    }

    override fun onDetectNetworkReliability() {

    }

    override fun onNoNetworkDetected() {
        networkType = NETWORK_TYPE.UNKNOWN
        subject.onNext(networkType!!)
    }

    override fun onWeakNetworkConnection() {

    }

}

enum class NETWORK_TYPE {
    WIFI, MOBILE_LTE, MOBILE_EDGE, UNKNOWN
}