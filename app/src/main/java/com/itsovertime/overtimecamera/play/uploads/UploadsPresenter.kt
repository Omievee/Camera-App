package com.itsovertime.overtimecamera.play.uploads

import com.itsovertime.overtimecamera.play.uploadsmanager.UploadsManager
import com.itsovertime.overtimecamera.play.videomanager.VideosManager
import com.itsovertime.overtimecamera.play.wifimanager.NETWORK_TYPE
import com.itsovertime.overtimecamera.play.wifimanager.WifiManager
import io.reactivex.disposables.Disposable

class UploadsPresenter(
    val view: UploadsFragment,
    val manager: VideosManager,
    val wifiManager: WifiManager,
    val uploadManager: UploadsManager
) {

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

    private var networkDisposable: Disposable? = null
    private fun subscribeToNetworkUpdates() {
        networkDisposable?.dispose()
        networkDisposable = wifiManager
            .subscribeToNetworkUpdates()
            .subscribe({
                when (it) {
                    NETWORK_TYPE.WIFI -> view.displayWifiReady()
//                    NETWORK_TYPE.MOBILE_LTE -> view.display
//                    NETWORK_TYPE.MOBILE_EDGE -> view.display
                    else -> view.displayNoNetworkConnection()
                }
                //  getVideoInstance()

            }, {
                it.printStackTrace()
            })
    }


    var uploadDisposable: Disposable? = null
    private fun getVideoInstance() {
        uploadDisposable?.dispose()
        uploadDisposable =
            uploadManager
                .getVideoInstance()
                .subscribe({
                    println("SUccess from presenter?? $it")
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