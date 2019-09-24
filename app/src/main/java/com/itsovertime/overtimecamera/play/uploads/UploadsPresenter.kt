package com.itsovertime.overtimecamera.play.uploads

import android.annotation.SuppressLint
import android.media.MediaMetadataRetriever
import android.util.Log
import android.widget.Toast
import com.itsovertime.overtimecamera.play.db.AppDatabase
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.UploadState.*
import com.itsovertime.overtimecamera.play.network.EncryptedResponse
import com.itsovertime.overtimecamera.play.network.TokenResponse
import com.itsovertime.overtimecamera.play.network.Upload
import com.itsovertime.overtimecamera.play.network.VideoInstanceResponse
import com.itsovertime.overtimecamera.play.quemanager.QueManager
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
    private val uploadManager: UploadsManager,
    private val queManager: QueManager
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
        subscribeToQue()
    }

    private fun getLatestVideoForUploading() {
        val videoFromQue = queManager.onGetNextVideo()
        val videoFromHQ = queManager.onGetNextVideoFromMediumList()
        if (videoFromQue != null) {
            println("UPLOADING REG!!!")
            getVideoInstance(videoFromQue)
        } else {
            println("everything uploaded... $videoFromHQ")
        }
//        else if (videoFromHQ != null && userEnabledHDUploads) {
//            if(videoFromHQ.uploadState == QUEUED){
//                println("UPLOADING REG!!!")
//                getVideoInstance(videoFromHQ)
//            }else manager.resetUploadStateForCurrentVideo(videoFromHQ)
//        } else {
//            println("--------ALL VIDEOS UPLOADED-----------")
//            return
//        }
    }


    private var queDisp: Disposable? = null
    private fun subscribeToQue() {
        queDisp?.dispose()
        queDisp = queManager
            .onIsQueReady()
            .subscribe({
                if (it) {
                    getLatestVideoForUploading()
                }
            }, {

            })
    }

    var clientIdDisposable: Disposable? = null

    private var managerDisposable: Disposable? = null
    private fun subscribeToVideosFromGallery() {
        managerDisposable = manager
            .subscribeToVideoGallery()
            .subscribe({
                view.updateAdapter(it.asReversed())
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

    private var instanceDisposable: Disposable? = null
    var currentVideo: SavedVideo? = null
    @Synchronized
    private fun getVideoInstance(it: SavedVideo?) {
        currentVideo = it
        currentVideo?.uploadState = REGISTERING
        instanceDisposable =
            uploadManager
                .getVideoInstance(currentVideo ?: return)
                .map {
                    videoInstanceResponse = it
                }
                .doOnSuccess {
                    currentVideo?.id = videoInstanceResponse?.video?.id.toString()
                    currentVideo?.uploadState = REGISTERED
                    requestTokenForUpload()
                }
                .doOnError {
                    manager.resetUploadStateForCurrentVideo(
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
        println("AWS DATA")
        awsDataDisposable =
            uploadManager
                .getAWSDataForUpload()
                .doOnError {
                    println("Error...")
                    manager.resetUploadStateForCurrentVideo(currentVideo ?: return@doOnError)
                    it.printStackTrace()
                }
                .map {
                    println("Token.. $it")
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
        tokenDisposable =
            uploadManager
                .registerWithMD5(token ?: return)
                .map {
                    println("Encrypt... $it")
                    encryptionResponse = it
                }
                .doOnSuccess {
                    continueUploadProcess()
                }
                .doOnError {
                    manager.resetUploadStateForCurrentVideo(
                        currentVideo = currentVideo ?: return@doOnError
                    )
                }
                .subscribe({
                }, {
                })

    }

    private var minChunkSize = (0.5 * 1024).toInt()
    private var maxChunkSize = 1024 * 2 * 1024
    private var baseChunkSize = 1024
    private var uploadChunkIndex = 0
    private var firstRun: Boolean = true
    private var fullBytes = byteArrayOf()
    var remainder: Int = 0
    var count = 0
    private fun continueUploadProcess() {
        println("Video ID: ${currentVideo?.id}")
        if (currentVideo?.id != "") {
            chunkToUpload = baseChunkSize
            if (currentVideo?.mediumUploaded == true) {
                fullBytes = File(currentVideo?.highRes).readBytes()
                isHighQuality = true
                currentVideo?.uploadState = UPLOADING_HIGH
                upload()
            } else {
                fullBytes = File(currentVideo?.mediumRes).readBytes()
                currentVideo?.uploadState = UPLOADING_MEDIUM
                upload()
            }
        } else {
            manager.resetUploadStateForCurrentVideo(currentVideo ?: return)
        }
    }

    private var startRange = 0
    private var chunkToUpload: Int = 0
    var isHighQuality: Boolean = false
    @SuppressLint("CheckResult")
    var up: Disposable? = null

    private fun upload() {
        val fullFileSize = fullBytes.size
        val previousStartPlusDynamicChunk = startRange + chunkToUpload

        val end = minOf(fullFileSize, previousStartPlusDynamicChunk)
        println("chunk ? $chunkToUpload")

        val sliceFromFullFile = fullBytes.sliceArray(
            IntRange(
                startRange,
                end - 1
            )
        )
        synchronized(this) {
            up = uploadManager
                .uploadVideoToServer(
                    upload = encryptionResponse?.upload ?: return@synchronized,
                    array = sliceFromFullFile,
                    chunk = uploadChunkIndex
                )
                .doAfterNext {
                    if (it.body()?.success == true) {
                        chunkToUpload = when (checkResponseTimeDifference(
                            timeSent = it.raw().sentRequestAtMillis(),
                            timeReceived = it.raw().receivedResponseAtMillis()
                        )) {
                            in 0..1 -> maxChunkSize
                            in 1..2 -> baseChunkSize
                            else -> minChunkSize
                        }

                        if (startRange >= end) {
                            checkForComplete()
                            startRange = 0
                            chunkToUpload = 0
                        } else {
                            startRange = end
                            uploadChunkIndex++
                            upload()
                        }
                    }
                }
                .doOnError {
                    startRange = 0
                    uploadChunkIndex = 0
                    manager.resetUploadStateForCurrentVideo(
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
    private fun checkResponseTimeDifference(timeSent: Long, timeReceived: Long): Long {
        val timeDif = timeReceived - timeSent
        return TimeUnit.MILLISECONDS.toSeconds(timeDif)
    }


    var CODE = 0
    var complete: Disposable? = null
    private fun checkForComplete() {
        complete = uploadManager
            .onCompleteUpload(encryptionResponse?.upload?.id ?: "")
            .doOnError {
                manager.resetUploadStateForCurrentVideo(
                    currentVideo = currentVideo ?: return@doOnError
                )
            }
            .subscribe({
                if (it.code() == 502) {
                    checkForComplete()
                }
                when (it.body()?.status) {
                    CompleteResponse.COMPLETING.name -> pingServerForStatus()
                    CompleteResponse.COMPLETED.name -> {
                        firstRun = true
                        finalizeUpload(it.body()?.upload)
                    }
                    else -> {
                        manager.resetUploadStateForCurrentVideo(
                            currentVideo = currentVideo ?: return@subscribe
                        )
                    }
                }
            }, {
                it.printStackTrace()
            })
    }


    var serverDis: Disposable? = null
    private fun finalizeUpload(upload: Upload?) {


        val path = when (isHighQuality) {
            true -> currentVideo?.highRes
            else -> currentVideo?.mediumRes
        }
        try {
            getVideoDimensions(path = path ?: "")
        } catch (arg: IllegalAccessException) {
            arg.printStackTrace()
            manager.resetUploadStateForCurrentVideo(currentVideo ?: return)
        }

        serverDis = uploadManager
            .writerToServerAfterComplete(
                uploadId = currentVideo?.id ?: "",
                S3Key = upload?.S3Key ?: "",
                vidWidth = width,
                vidHeight = height,
                hq = isHighQuality,
                vid = currentVideo ?: return
            )
            .doOnSuccess {
                if (currentVideo?.uploadState == UPLOADING_MEDIUM) {
                    manager.updateMediumUploaded(true, currentVideo?.clientId ?: "")
                    currentVideo?.uploadState = UPLOADED_MEDIUM
                } else {
                    currentVideo?.uploadState = UPLOADED_HIGH
                    manager.updateHighuploaded(true, currentVideo?.clientId ?: "")
                }
            }
            .subscribe({
            }, {
                it.printStackTrace()
                manager.resetUploadStateForCurrentVideo(currentVideo ?: return@subscribe)
            })
    }


    private fun pingServerForStatus() {
        val timer = Timer()
        val timerTask = object : TimerTask() {
            override fun run() {
                synchronized(this) {
                    checkForComplete()
                }
            }
        }
        timer.schedule(timerTask, 5000)
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

    var width: Int = 0
    var height: Int = 0
    private fun getVideoDimensions(path: String) {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        val width =
            Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH))
        val height =
            Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT))
        retriever.release()

        this.width = width
        this.height = height
    }

    fun hdSwitchWasChecked(isChecked: Boolean) {
        println("checked.... $isChecked")
    }
}

enum class CompleteResponse {
    COMPLETING,
    COMPLETED,
    FAILED
}