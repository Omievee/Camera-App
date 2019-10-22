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
        subscribeToVideosFromGallery()
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
                    println("This is.... $it")
                    when (it) {
                        UploadsMessage.Uploading_High -> view.setUploadingHdVideo()
                        UploadsMessage.Uploading_Medium -> view.setUploadingMedVideo()
                        UploadsMessage.Pending_High -> view.onNotifyOfPendingHDUploads()
                    }
                }, {

                })
    }


    var type = UploadType.UserView
    var list = mutableListOf<SavedVideo>()
    private var managerDisposable: Disposable? = null
    private fun subscribeToVideosFromGallery() {
        managerDisposable = manager
            .subscribeToVideoGallery()
            .map {
                this.list.addAll(it)
            }
            .subscribe({
                view.updateAdapter(list, type)
                view.swipe2RefreshIsFalse()
            }, {
                println("throwable: ${it.printStackTrace()}")
            })
    }

    private var networkDisposable: Disposable? = null
    var isWifiEnabled: Boolean = false
    var userEnabledHDUploads: Boolean = false

    private fun subscribeToNetworkUpdates() {
        networkDisposable?.dispose()
        networkDisposable = wifiManager
            .subscribeToNetworkUpdates()
            .subscribe({
                when (it) {
                    NETWORK_TYPE.WIFI -> {
                        //TODO: logic to prompt for HD uploads & toggle...
                        isWifiEnabled = true
                        view.displayWifiReady()
                    }
//                    NETWORK_TYPE.MOBILE_LTE -> view.display
//                    NETWORK_TYPE.MOBILE_EDGE -> view.display
                    else -> {
                        isWifiEnabled = false
                        view.displayNoNetworkConnection()
                    }
                }
            }, {
                it.printStackTrace()
            })
    }


    fun hdSwitchWasChecked(isChecked: Boolean) {
        println("checked.... $isChecked")
        if (isChecked) {
            val inputData = Data.Builder().apply {
                putBoolean("HD", true)
            }
            WorkManager.getInstance(view).enqueue(
                OneTimeWorkRequestBuilder<VideoUploadWorker>()
                    .setInputData(inputData.build())
                    .build()
            )
        }
    }

    fun displayBottomSheetSettings() {
        view.displaySettings()
    }

    fun updateAdapterForDebug() {
        type = if (type == UploadType.UserView) {
            UploadType.DebugView
        } else UploadType.UserView
        view.updateAdapter(list, type)
    }
}

enum class CompleteResponse {
    COMPLETING,
    COMPLETED,
    FAILED
}