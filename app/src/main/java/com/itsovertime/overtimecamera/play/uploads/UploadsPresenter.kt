package com.itsovertime.overtimecamera.play.uploads

import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.UploadState
import com.itsovertime.overtimecamera.play.network.EncryptedResponse
import com.itsovertime.overtimecamera.play.network.TokenResponse
import com.itsovertime.overtimecamera.play.network.Upload
import com.itsovertime.overtimecamera.play.network.VideoInstanceResponse
import com.itsovertime.overtimecamera.play.uploadsmanager.CurrentVideoUpload
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
        subscribeToNetworkUpdates()
        subscribeToCurrentVideoBeingUploaded()

    }

    fun onResume() {
        view.swipe2RefreshIsTrue()
        subscribeToVideosFromGallery()
    }

    var clientIdDisposable: Disposable? = null
    var currentVideo: CurrentVideoUpload? = null
    private fun subscribeToCurrentVideoBeingUploaded() {
        clientIdDisposable?.dispose()
        clientIdDisposable =
            uploadManager
                .onCurrentFileBeingUploaded()
                .subscribe({
                    currentVideo = it
                }, {

                })
    }


    private fun updateCurrentVideoStatus(currentVideo: SavedVideo?, state: UploadState) {
        currentVideo?.let {
            manager.updateVideoStatus(it, state)
        }
    }

    private var managerDisposable: Disposable? = null
    private fun subscribeToVideosFromGallery() {
        managerDisposable?.dispose()
        managerDisposable = manager
            .subscribeToVideoGallery()
            .subscribe({
                view.updateAdapter(it)
                view.swipe2RefreshIsFalse()
                if (!it.isNullOrEmpty()) {
                    uploadManager?.beginUploadProcess()
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

    private var instanceDisposable: Disposable? = null
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
                    println("Succes from presenter...")
                    requestTokenForUpload(videoInstanceResponse)
                }
                .subscribe({
                }, {

                })

    }

    var videoInstanceResponse: VideoInstanceResponse? = null
    private var tokenResponse: TokenResponse? = null
    private var tokenDisposable: Disposable? = null
    private fun requestTokenForUpload(response: VideoInstanceResponse?) {
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
                    beginUpload()
                }
                .subscribe({
                }, {

                })
    }

    fun updateUploadId() {

    }

    private var awsDataDisposable: Disposable? = null
    private var encryptionResponse: EncryptedResponse? = null
    private fun beginUpload() {
        tokenDisposable?.dispose()
        tokenDisposable =
            uploadManager
                .registerWithMD5(tokenResponse ?: return)
                .map {
                    encryptionResponse = it
                    manager.updateVideoMd5(
                        encryptionResponse?.upload?.md5 ?: "",
                        currentVideo?.video?.clientId ?: ""
                    )
                    manager.updateUploadId(
                        encryptionResponse?.upload?.id ?: "",
                        currentVideo?.video?.clientId ?: ""
                    )
                }
                .doOnError {

                }
                .doOnSuccess {

                    uploadRegisteredVideo(
                        upload = encryptionResponse?.upload ?: return@doOnSuccess
                    )
                }
                .subscribe({

                }, {

                })

    }

    var uploadDisposable: Disposable? = null
    private fun uploadRegisteredVideo(upload: Upload) {
        println("upload registerd video.... $upload")
        uploadDisposable?.dispose()
        uploadManager
            .prepareVideoForUpload(
                upload
            )
    }

    fun onDestroy() {
        managerDisposable?.dispose()
        awsDataDisposable?.dispose()
        networkDisposable?.dispose()
        instanceDisposable?.dispose()
        tokenDisposable?.dispose()
        clientIdDisposable?.dispose()
    }

    fun displayBottomSheetSettings() {
        view.displaySettings()
    }
}