package com.itsovertime.overtimecamera.play.uploads

import com.itsovertime.overtimecamera.play.videomanager.VideosManager
import com.itsovertime.overtimecamera.play.wifimanager.NETWORK_TYPE
import com.itsovertime.overtimecamera.play.wifimanager.WifiManager
import io.reactivex.disposables.Disposable

class UploadsPresenter(val view: UploadsFragment, val manager: VideosManager, val wifiManager: WifiManager) {

    var managerDisposable: Disposable? = null

    fun onCreate() {
        manager.loadFromDB()
    }

    fun onResume() {
        view.swipe2RefreshIsTrue()
        subscribeToVideosFromGallery()
        subscribeToNetworkUpdates()
    }


    private fun subscribeToVideosFromGallery() {
        managerDisposable?.dispose()
        managerDisposable = manager
            .subscribeToVideoGallery()
            .map {
                view.updateAdapter(it)
                view.swipe2RefreshIsFalse()
            }.subscribe({
            }, {
                println("throwable: ${it.printStackTrace()}")
            })
    }

    var networkDisposable: Disposable? = null
    private fun subscribeToNetworkUpdates() {
        networkDisposable?.dispose()
        networkDisposable = wifiManager
            .subscribeToNetworkUpdates()
            .subscribe({
                if (it == NETWORK_TYPE.WIFI){
                    view.displayWifiReady()
                }
                println("TYPE OF NETWORK.... ${it}")
            }, {

            })


    }


    fun onDestroy() {
        managerDisposable?.dispose()
        networkDisposable?.dispose()
    }

    fun displayBottomSheetSettings() {
        view.displaySettings()
    }
}