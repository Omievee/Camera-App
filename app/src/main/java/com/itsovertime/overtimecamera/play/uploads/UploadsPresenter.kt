package com.itsovertime.overtimecamera.play.uploads

import com.itsovertime.overtimecamera.play.network.EncryptedResponse
import com.itsovertime.overtimecamera.play.network.TokenResponse
import com.itsovertime.overtimecamera.play.network.VideoInstanceResponse
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

    var videoInstanceResponse: VideoInstanceResponse? = null
    var tokenResponse: TokenResponse? = null
    var tokenDisposable: Disposable? = null
    fun requestTokenForUpload(response: VideoInstanceResponse?) {
        awsDataDisposable?.dispose()
        awsDataDisposable =
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

    var awsDataDisposable: Disposable? = null
    var encryptionResponse: EncryptedResponse? = null
    private fun uploadVideo() {
        println("Token Response::: ${tokenResponse}")
        tokenDisposable?.dispose()
        tokenDisposable =
                uploadManager
                        .registerUploadForId(tokenResponse ?: return)
                        .map {
                            encryptionResponse = it
                        }
                        .doOnError {

                        }
                        .doOnSuccess {
                            uploadRegisteredVideo(id = encryptionResponse?.upload?.id ?: "")
                        }
                        .subscribe({

                        }, {

                        })

    }

    var uploadDisposable: Disposable? = null
    private fun uploadRegisteredVideo(id: String) {
        println("Continue with upload... .$id")
        uploadDisposable?.dispose()
        uploadDisposable =
                uploadManager
                        .uploadVideo(id)
                        .doOnError {
                            println("error from upload api... ${it.message}")
                        }
                        .doOnSuccess {
                            println("success $it.... ")
                        }
                        .subscribe({

                        }, {

                        })

    }

    fun onDestroy() {
        managerDisposable?.dispose()
        awsDataDisposable?.dispose()
        networkDisposable?.dispose()
        instanceDisposable?.dispose()
        tokenDisposable?.dispose()
    }

    fun displayBottomSheetSettings() {
        view.displaySettings()
    }
}