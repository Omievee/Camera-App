package com.itsovertime.overtimecamera.play.uploads

import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.progressmanager.ProgressManager
import com.itsovertime.overtimecamera.play.progressmanager.UploadsMessage
import com.itsovertime.overtimecamera.play.videomanager.VideosManager
import com.itsovertime.overtimecamera.play.wifimanager.NETWORK_TYPE
import com.itsovertime.overtimecamera.play.wifimanager.WifiManager
import io.reactivex.disposables.Disposable

class UploadsPresenter(
    val view: UploadsActivity,
    val manager: VideosManager,
    private val wifiManager: WifiManager,
    private val progressManager: ProgressManager
) {
    fun onCreate() {
        subscribeToNetworkUpdates()
        subscribeToCompletedUploads()
    }

    var progDisp: Disposable? = null
    var prog: Int = 0
    private fun subscribeToUploadProgress() {
        progDisp?.dispose()
        progDisp = progressManager
            .subscribeToUploadProgress()
            .subscribe({
                println("upload progress.... $it")
                if (it.prog > 99) {
                    view.updateAdapter(list.asReversed(), debug, userEnabledHDUploads)
                } else view.updateProgressBar(it.id, it.prog, it.isHD)

            }, {

            })
    }

    var comp: Disposable? = null
    private fun subscribeToCompletedUploads() {
        comp?.dispose()
        comp = manager
            .subscribeToCompletedUploads()
            .subscribe({ s ->
                view.updateAdapter(list.asReversed(), debug, userEnabledHDUploads)
//                val vid = list.find {
//                    it.clientId == s.clientId
//                }
//                val index = list.indexOf(vid)
//                view.updateCompletedUpload(index)
            }, {
                it.printStackTrace()
            })
    }

    fun onRefresh() {
        view.swipe2RefreshIsTrue()
        loadVideoGallery()
    }

    fun onResume() {
        onRefresh()
        subscribeToCurrentUploadMsg()
        subscribeToUploadProgress()
    }

    var qualityDisp: Disposable? = null
    private fun subscribeToCurrentUploadMsg() {
        qualityDisp?.dispose()
        qualityDisp =
            progressManager
                .onUpdateUploadMessage()
                .subscribe({
                    println("Uploads message is $it")
                    when (it) {
                        UploadsMessage.Uploading_High -> view.setUploadingHdVideo()
                        UploadsMessage.Uploading_Medium -> view.setUploadingMedVideo()
                        UploadsMessage.Pending_High -> view.onNotifyOfPendingHDUploads()
                        UploadsMessage.Finished -> view.noVideos()
                        UploadsMessage.NO_NETWORK -> view.displayNoNetworkConnection()
                        else -> view.setNoVideosMsg()
                    }
                }, {
                })
    }


    var list = mutableListOf<SavedVideo>()
    var debug: Boolean = false
    private var managerDisposable: Disposable? = null
    private fun loadVideoGallery() {
        managerDisposable = manager
            .onGetVideosForUploadScreen()
            .map {
                this.list.clear()
                this.list.addAll(it)
            }
            .subscribe({
                view.updateAdapter(list.asReversed(), debug, userEnabledHDUploads)
                view.swipe2RefreshIsFalse()
            }, {
                println("throwable: ${it.printStackTrace()}")
            })
    }

    private var networkDisposable: Disposable? = null
    var userEnabledHDUploads: Boolean = false

    private fun subscribeToNetworkUpdates() {
        networkDisposable?.dispose()
        networkDisposable = wifiManager
            .subscribeToNetworkUpdates()
            .subscribe({
                when (it) {
                    NETWORK_TYPE.WIFI -> view.updateMsg()
                    NETWORK_TYPE.MOBILE_LTE -> view.updateMsg()
                    else -> view.displayNoNetworkConnection()
                }
            }, {
                it.printStackTrace()
            })
    }

    fun hdSwitchWasChecked(isChecked: Boolean) {
        if (isChecked) {
            manager.onNotifyHDUploadsTriggered(isChecked)
            userEnabledHDUploads = isChecked
            view.updateAdapter(list.asReversed(), debug, userEnabledHDUploads)
        }
    }

    fun displayBottomSheetSettings() {
        view.displaySettings()
    }

    fun updateAdapterForDebug() {
        debug = !debug
        view.updateAdapter(list.asReversed(), debug, userEnabledHDUploads)
    }
}

enum class CompleteResponse {
    COMPLETING,
    COMPLETED,
    FAILED
}