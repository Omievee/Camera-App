package com.itsovertime.overtimecamera.play.uploads

import android.annotation.SuppressLint
import android.widget.ProgressBar
import com.itsovertime.overtimecamera.play.db.AppDatabase
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.UploadState
import com.itsovertime.overtimecamera.play.network.EncryptedResponse
import com.itsovertime.overtimecamera.play.network.TokenResponse
import com.itsovertime.overtimecamera.play.network.VideoInstanceResponse
import com.itsovertime.overtimecamera.play.progressbar.ProgressBarAnimation
import com.itsovertime.overtimecamera.play.uploadsmanager.UploadsManager
import com.itsovertime.overtimecamera.play.videomanager.VideosManager
import com.itsovertime.overtimecamera.play.wifimanager.NETWORK_TYPE
import com.itsovertime.overtimecamera.play.wifimanager.WifiManager
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

class UploadsPresenter(
    val view: UploadsFragment,
    val manager: VideosManager,
    private val wifiManager: WifiManager,
    private val uploadManager: UploadsManager
) {


    fun onCreate() {
        manager.loadFromDB()
        subscribeToNetworkUpdates()
    }

    fun onResume() {
        view.swipe2RefreshIsTrue()
        subscribeToVideosFromGallery()
        subscribeToCurrentVideoBeingUploaded()
    }

    var clientIdDisposable: Disposable? = null
    var currentVideoQue: List<SavedVideo>? = null
    private fun subscribeToCurrentVideoBeingUploaded() {
        clientIdDisposable?.dispose()
        clientIdDisposable =
            uploadManager
                .onUpdatedQue()
                .subscribe({
                    currentVideoQue = it
                    currentVideoQue?.forEach {
                        manager.updateVideoStatus(it, UploadState.QUEUED)
                        getVideoInstance(it)
                        println("QUE CHECK ::::::: ${it.is_favorite} && ${it.clientId}")
                    }
                }, {
                })
    }

    var videoList: List<SavedVideo>? = null
    private var managerDisposable: Disposable? = null
    private fun subscribeToVideosFromGallery() {
        managerDisposable?.dispose()
        managerDisposable = manager
            .subscribeToVideoGallery()
            .subscribe({
                videoList = it
                view.updateAdapter(it)
                view.swipe2RefreshIsFalse()
            }, {
                println("throwable: ${it.printStackTrace()}")
            })
    }

    private var networkDisposable: Disposable? = null
    var isWifiEnabled: Boolean = false
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

    private var instanceDisposable: Disposable? = null
    var currentVideo: SavedVideo? = null
    @Synchronized
    private fun getVideoInstance(it: SavedVideo) {
        currentVideo = it
        println("Current video is:::: $currentVideo")
        manager.updateVideoStatus(currentVideo ?: return, UploadState.REGISTERING)
        instanceDisposable?.dispose()
        instanceDisposable =
            uploadManager
                .getVideoInstance(currentVideo ?: return)
                .map {
                    videoInstanceResponse = it
                }
                .doOnSuccess {
                    manager.updateVideoInstanceId(
                        videoInstanceResponse?.video?.id.toString(),
                        currentVideo?.clientId.toString()
                    )
                    manager.updateVideoStatus(
                        currentVideo ?: return@doOnSuccess,
                        UploadState.REGISTERED
                    )
                    requestTokenForUpload()
                }
                .doOnError {
                    println("ERROR FROM INSTANCE::: ${it.message}")
                    uploadManager.resetUploadStateForCurrentVideo(
                        currentVideo = currentVideo ?: return@doOnError
                    )
                }
                .subscribe({
                }, {
                })

    }

    var videoInstanceResponse: VideoInstanceResponse? = null
    private var tokenResponse: TokenResponse? = null
    private var tokenDisposable: Disposable? = null
    private var awsDataDisposable: Disposable? = null
    private fun requestTokenForUpload() {
        awsDataDisposable?.dispose()
        awsDataDisposable =
            uploadManager
                .getAWSDataForUpload()
                .doOnError {
                    it.printStackTrace()
                }
                .map {
                    tokenResponse = it
                }
                .doOnSuccess {
                    beginUpload(token = tokenResponse)
                }
                .subscribe({
                }, {

                })
    }

    private var encryptionResponse: EncryptedResponse? = null
    private fun beginUpload(token: TokenResponse?) {
        tokenDisposable?.dispose()
        tokenDisposable =
            uploadManager
                .registerWithMD5(token ?: return)
                .map {
                    encryptionResponse = it
                    manager.updateVideoMd5(
                        md5 = encryptionResponse?.upload?.md5.toString(),
                        clientId = currentVideo?.clientId.toString()
                    )
                    manager.updateUploadId(
                        uplaodId = encryptionResponse?.upload?.id.toString(),
                        clientId = currentVideo?.clientId.toString()
                    )
                }
                .doOnSuccess {
                    getVideoForUpload(currentVideo ?: return@doOnSuccess)
                }
                .doOnError {
                    uploadManager.resetUploadStateForCurrentVideo(
                        currentVideo = currentVideo ?: return@doOnError
                    )
                }
                .subscribe({
                }, {
                })

    }

    private var vid: SavedVideo? = null
    @SuppressLint("CheckResult")
    private fun getVideoForUpload(savedVideo: SavedVideo) {
        val db = view?.context?.let { AppDatabase.getAppDataBase(context = it) }
        val videoDao = db?.videoDao()
        Single.fromCallable {
            with(videoDao) {
                this?.getVideoForUpload(savedVideo?.clientId)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                vid = it
            }
            .subscribe({
                firstRun = true
                doOnce = true
                continueUploadProcess()
            }, {
                it.printStackTrace()
            })
    }


    private var MIN_CHUNK_SIZE = 0.5 * 1024
    private var MAX_CHUNK_SIZE = 2 * 1024 * 1024
    private var chunkSize = 1024
    var chunk: Int = 0
    var startRange = 0
    private var uploadChunkIndex = 0
    private var firstRun: Boolean = true
    var maxProgress: Int = 0

    private fun continueUploadProcess() {
        when (vid?.uploadState) {
            UploadState.REGISTERED -> manager.updateVideoStatus(
                vid ?: return,
                UploadState.UPLOADING_MEDIUM
            )
            UploadState.UPLOADED_MEDIUM -> manager.updateVideoStatus(
                vid ?: return,
                UploadState.UPLOADING_HIGH
            )
            else -> {
                //should not be in que if complete ...
            }
        }
        var fullBytes = 0
        if (isWifiEnabled && vid?.uploadState == UploadState.UPLOADED_MEDIUM) {
            fullBytes = File(vid?.highRes).readBytes().size
            isHighQuality = true
        } else if (!isWifiEnabled && vid?.uploadState == UploadState.UPLOADED_MEDIUM) {
            //TODO account for "waiting to upload high res"
        } else {
            if (vid?.uploadState == UploadState.REGISTERED) {
                fullBytes = File(vid?.mediumRes).readBytes().size
            }
        }
        maxProgress = fullBytes

        chunk = if (!firstRun) {
            when (diffInSec) {
                in 0..1 -> MAX_CHUNK_SIZE
                in 1..2 -> chunkSize
                else -> MIN_CHUNK_SIZE.toInt()
            }
        } else {
            chunkSize
        }
        view.updateAdapter(
            videoList ?: emptyList(),
            ProgressData(0, startRange, isHighQuality, vid?.clientId.toString())
        )
        println("RESPONSE TIME::::::::: $diffInSec")

        firstRun = false
        val remainder = fullBytes - startRange
        val endRange = when (remainder < chunk) {
            true -> startRange + remainder - 1
            else -> startRange + chunk - 1
        }
        val dynamicDataSlice = File(vid?.mediumRes).readBytes().sliceArray(
            IntRange(
                startRange,
                endRange
            )
        )

        when (startRange <= fullBytes) {
            true -> synchronized(this) {
                upload(
                    chunkIndex = uploadChunkIndex,
                    bytes = dynamicDataSlice
                )
            }
            else -> {
                //checkForComplete()
            }
        }
        println("Remainder is $remainder Offset is: $startRange Chunk is: $chunk")
    }

    var isHighQuality: Boolean = false
    var doOnce: Boolean = false

    @SuppressLint("CheckResult")
    var up: Disposable? = null

    private fun upload(chunkIndex: Int, bytes: ByteArray) {


        synchronized(this) {
            up = uploadManager
                .uploadVideoToServer(
                    upload = encryptionResponse?.upload ?: return@synchronized,
                    array = bytes,
                    chunk = chunkIndex
                )
                .doAfterNext {
                    if (it.body()?.success == true) {
                        checkResponseTimeDifference(
                            timeSent = it.raw().sentRequestAtMillis(),
                            timeReceived = it.raw().receivedResponseAtMillis()
                        )
                        uploadChunkIndex++
                        startRange += chunk
                        continueUploadProcess()
                    }
                }
                .doOnError {
                    startRange = 0
                    uploadChunkIndex = 0
                    uploadManager.resetUploadStateForCurrentVideo(
                        currentVideo = currentVideo ?: return@doOnError
                    )
                }
                .subscribe({
                }, {
                    it.printStackTrace()
                })
        }
    }

    private var diffInSec: Long = 0
    private fun checkResponseTimeDifference(timeSent: Long, timeReceived: Long) {
        val timeDif = timeReceived - timeSent
        diffInSec = TimeUnit.MILLISECONDS.toSeconds(timeDif)
    }

    var complete: Disposable? = null
    private fun checkForComplete() {
        when {
            vid?.uploadState == UploadState.UPLOADING_MEDIUM -> manager.updateVideoStatus(
                vid ?: return, UploadState.UPLOADED_MEDIUM
            )
            vid?.uploadState == UploadState.UPLOADING_HIGH -> manager.updateVideoStatus(
                vid ?: return, UploadState.UPLOADED_HIGH
            )
            else -> uploadManager.resetUploadStateForCurrentVideo(
                currentVideo = currentVideo ?: return
            )
        }
        complete = uploadManager
            .onCompleteUpload(encryptionResponse?.upload?.id ?: "")
            .doOnError {
                uploadManager.resetUploadStateForCurrentVideo(
                    currentVideo = currentVideo ?: return@doOnError
                )
            }
            .subscribe({
                when (it.status) {
                    COMPLETE_RESPONSE.COMPLETING.name -> pingServerForStatus()
                    COMPLETE_RESPONSE.COMPLETED.name -> {
                        firstRun = true
                        manager.updateVideoStatus(vid ?: return@subscribe, UploadState.COMPLETE)
                        finalizeUpload()
                    }
                    else -> uploadManager.resetUploadStateForCurrentVideo(
                        currentVideo = vid ?: return@subscribe
                    )
                }
            }, {
                it.printStackTrace()
            })
    }

    var serverDis: Disposable? = null
    private fun finalizeUpload() {
        serverDis?.dispose()
        serverDis = uploadManager
            .writerToServerAfterComplete()
            .subscribe({

            }, {

            })

    }

    private fun pingServerForStatus() {
        val timer = Timer()
        val timerTask = object : TimerTask() {
            override fun run() {
                checkForComplete()
            }
        }
        timer.schedule(timerTask, 5000L)
    }

    fun onDestroy() {
        managerDisposable?.dispose()
        awsDataDisposable?.dispose()
        networkDisposable?.dispose()
        instanceDisposable?.dispose()
        tokenDisposable?.dispose()
        clientIdDisposable?.dispose()
        complete?.dispose()
        up?.dispose()
        serverDis?.dispose()
    }

    fun displayBottomSheetSettings() {
        view.displaySettings()
    }


}

enum class COMPLETE_RESPONSE {
    COMPLETING,
    COMPLETED,
    FAILED
}