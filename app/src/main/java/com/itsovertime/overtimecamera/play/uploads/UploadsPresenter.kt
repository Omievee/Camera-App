package com.itsovertime.overtimecamera.play.uploads

import com.itsovertime.overtimecamera.play.network.TokenResponse
import com.itsovertime.overtimecamera.play.network.VideoResponse
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


    fun onCreate() {
        manager.loadFromDB()
    }

    fun onResume() {
        view.swipe2RefreshIsTrue()
        subscribeToVideosFromGallery()
        subscribeToNetworkUpdates()


    }

    var managerDisposable: Disposable? = null
    private fun subscribeToVideosFromGallery() {
        managerDisposable?.dispose()
        managerDisposable = manager
                .subscribeToVideoGallery()
                .subscribe({
                    view.updateAdapter(it)
                    view.swipe2RefreshIsFalse()
                    if (!it?.isNullOrEmpty()!!) {
                        getVideoInstance()
                    }
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


    var instanceDisposable: Disposable? = null
    private fun getVideoInstance() {
        instanceDisposable?.dispose()
        instanceDisposable =
                uploadManager
                        .getVideoInstance()
                        .doOnError {
                        }
                        .map {
                            videoInstanceResponse = it
                        }
                        .doOnSuccess {
                            requestTokenForUpload(videoInstanceResponse)
                        }
                        .subscribe({
                        }, {

                        })
    }

    var videoInstanceResponse: VideoResponse? = null
    var tokenResponse: TokenResponse? = null
    var tokenDisposable: Disposable? = null
    fun requestTokenForUpload(response: VideoResponse?) {
        tokenDisposable?.dispose()
        tokenDisposable =
                uploadManager
                        .getAWSDataForUpload(response ?: return)
                        .doOnError {
                        }
                        .map {
                            tokenResponse = it
                        }
                        .doOnSuccess {
                            uploadVideo()
                        }
                        .subscribe({
                        }, {

                        })
    }

    var uploadDisposable: Disposable? = null
    private fun uploadVideo() {
        println("Token Response::: ${tokenResponse}")
        uploadDisposable?.dispose()
        uploadDisposable =
                uploadManager
                        .registerUploadForId(tokenResponse ?: return)
                        .doOnError {

                        }
                        .doOnSuccess {
                            println("success from upload.... ${it.upload?.id}")
                        }
                        .subscribe({

                        }, {

                        })

    }

    fun onDestroy() {
        managerDisposable?.dispose()
        uploadDisposable?.dispose()
        networkDisposable?.dispose()
        instanceDisposable?.dispose()
        tokenDisposable?.dispose()
    }

    fun displayBottomSheetSettings() {
        view.displaySettings()
    }
}