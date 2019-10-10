package com.itsovertime.overtimecamera.play.uploads

import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.itsovertime.overtimecamera.play.progress.ProgressManager
import com.itsovertime.overtimecamera.play.videomanager.VideosManager
import com.itsovertime.overtimecamera.play.wifimanager.NETWORK_TYPE
import com.itsovertime.overtimecamera.play.wifimanager.WifiManager
import com.itsovertime.overtimecamera.play.workmanager.VideoUploadWorker
import io.reactivex.disposables.Disposable

class UploadsPresenter(
    val view: UploadsActivity,
    val manager: VideosManager,
    private val wifiManager: WifiManager,
    val progressManager: ProgressManager
) {
    fun onCreate() {
        manager.loadFromDB()
        subscribeToNetworkUpdates()
    }

    fun onRefresh() {
        view.swipe2RefreshIsTrue()
        subscribeToVideosFromGallery()
    }

    fun onResume() {
        onRefresh()
        subscribeToPendingUploads()
        subcribeToQualityBeingUploaded()
    }

    var qualityDisp: Disposable? = null
    private fun subcribeToQualityBeingUploaded() {
        qualityDisp?.dispose()
        qualityDisp =
            progressManager
                .subscribeToCurrentVideoQuality()
                .subscribe({
                    when (it) {
                        true -> view.setUploadingHdVideo()
                        else -> view.setUploadingMedVideo()
                    }
                }, {

                })
    }


    var pendingDisposable: Disposable? = null
    private fun subscribeToPendingUploads() {
        pendingDisposable = progressManager
            .subscribeToPendingHQUploads()
            .subscribe({
                println("True.. $it")
                if (it) {
                    view.notifyPendingUploads()
                }
            }, {

            })
    }

    private var managerDisposable: Disposable? = null
    private fun subscribeToVideosFromGallery() {
        managerDisposable = manager
            .subscribeToVideoGallery()
            .subscribe({
                view.updateAdapter(it)
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
}

enum class CompleteResponse {
    COMPLETING,
    COMPLETED,
    FAILED
}