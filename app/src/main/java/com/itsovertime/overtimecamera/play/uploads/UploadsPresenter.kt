package com.itsovertime.overtimecamera.play.uploads

import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.progress.ProgressManager
import com.itsovertime.overtimecamera.play.progress.UploadsMessage
import com.itsovertime.overtimecamera.play.videomanager.VideosManager
import com.itsovertime.overtimecamera.play.wifimanager.NETWORK_TYPE
import com.itsovertime.overtimecamera.play.wifimanager.WifiManager
import com.itsovertime.overtimecamera.play.workmanager.VideoUploadWorker
import io.reactivex.disposables.Disposable

class UploadsPresenter(
    val view: UploadsActivity,
    val manager: VideosManager,
    private val wifiManager: WifiManager,
    private val progressManager: ProgressManager
) {
    fun onCreate() {
        manager.loadFromDB()
        subscribeToNetworkUpdates()
    }

    var progDisp: Disposable? = null
    var prog: Int = 0
    private fun subscribeToUploadProgress() {
        progDisp?.dispose()
        progDisp = progressManager
            .subscribeToUploadProgress()
            .subscribe({
                view.updateProgressBar(it.id, it.prog, it.isHD)
            }, {

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
                    when (it) {
                        UploadsMessage.Uploading_High -> view.setUploadingHdVideo()
                        UploadsMessage.Uploading_Medium -> view.setUploadingMedVideo()
                        UploadsMessage.Pending_High -> view.onNotifyOfPendingHDUploads()
                        UploadsMessage.Finished -> view.noVideos()
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
//            val inputData = Data.Builder().apply {
//                putBoolean("HD", true)
//            }
//            WorkManager.getInstance(view).enqueue(
//                OneTimeWorkRequestBuilder<VideoUploadWorker>()
//                    .setInputData(inputData.build())
//                    .build()
//            )
            userEnabledHDUploads = isChecked
            view.updateAdapter(list, debug, userEnabledHDUploads)
        }
    }

    fun displayBottomSheetSettings() {
        view.displaySettings()
    }

    fun updateAdapterForDebug() {
        debug = !debug
        view.updateAdapter(list, debug, userEnabledHDUploads)
    }
}

enum class CompleteResponse {
    COMPLETING,
    COMPLETED,
    FAILED
}