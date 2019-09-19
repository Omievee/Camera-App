package com.itsovertime.overtimecamera.play.uploads

import android.annotation.SuppressLint
import android.media.MediaMetadataRetriever
import android.util.Log
import android.widget.Toast
import com.itsovertime.overtimecamera.play.db.AppDatabase
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.UploadState
import com.itsovertime.overtimecamera.play.network.EncryptedResponse
import com.itsovertime.overtimecamera.play.network.TokenResponse
import com.itsovertime.overtimecamera.play.network.Upload
import com.itsovertime.overtimecamera.play.network.VideoInstanceResponse
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
import kotlin.math.max

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

    fun onRefresh() {
        view.swipe2RefreshIsTrue()
        subscribeToVideosFromGallery()
    }

    fun onResume() {
        subscribeToCurrentVideoBeingUploaded()
        onRefresh()
    }

    var clientIdDisposable: Disposable? = null
    var mediumUploads = mutableListOf<SavedVideo>()
    private fun subscribeToCurrentVideoBeingUploaded() {
        clientIdDisposable =
            uploadManager
                .onUpdateQue()
                .subscribe({
                    println("QUE LIST:: ${it.size}")
                    it.sortBy {
                        it.is_favorite
                    }
                    it.forEach {
                        println("UPLOAD STATE FROM QUE:::: ${it.uploadState}")
                        if (it.uploadState == UploadState.QUEUED) {
                            getVideoInstance(it)
                        } else if (it.uploadState == UploadState.REGISTERED
                            || it.uploadState == UploadState.REGISTERING
                            || it.uploadState == UploadState.UPLOADING_MEDIUM
                            || it.uploadState == UploadState.UPLOADED_MEDIUM
                            || it.uploadState == UploadState.UPLOADING_HIGH
                            || it.uploadState == UploadState.UPLOADED_HIGH
                        ) {
                            manager.resetUploadStateForCurrentVideo(it)
                        } else if (it.uploadState == UploadState.UPLOADED_MEDIUM && userEnabledHDUploads) {
                            getVideoInstance(it)
                        }
                    }
                }, {
                })
    }

    var videoList: List<SavedVideo>? = null
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

    private var instanceDisposable: Disposable? = null
    var currentVideo: SavedVideo? = null
    @Synchronized
    private fun getVideoInstance(it: SavedVideo) {
        println("______INSTANCE____")
        currentVideo = it
        manager.updateVideoStatus(currentVideo ?: return, UploadState.REGISTERING)
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
                    manager.resetUploadStateForCurrentVideo(
                        currentVideo = currentVideo ?: return@doOnError
                    )
                }
                .subscribe({
                }, {
                })

    }

    var db = view.context?.let { AppDatabase.getAppDataBase(context = it) }
    var videoDao = db?.videoDao()
    private var vid: SavedVideo? = null
    @SuppressLint("CheckResult")
    private fun getVideoForUpload(savedVideo: SavedVideo) {
        Single.fromCallable {
            with(videoDao) {
                this?.getVideoForUpload(savedVideo.clientId)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                vid = it
                println("vid is :${vid?.clientId}")
            }
            .doFinally {
                continueUploadProcess()
            }
            .subscribe({
            }, {
                it.printStackTrace()
            })
    }


    private var minChunkSize = (0.5 * 1024).toInt()
    private var maxChunkSize = 1024 * 2
    private var baseChunkSize = 1024
    private var chunkToUpload: Int = 0
    private var uploadChunkIndex = 0
    private var firstRun: Boolean = true
    private var fullBytes = byteArrayOf()
    var remainder: Int = 0
    var count = 0
    private fun continueUploadProcess() {
        println("This hit... $count++")
        if (vid?.id != "") {
            println("Status... ${vid?.uploadState}")
            when (vid?.uploadState) {
                UploadState.REGISTERED -> {
                    manager.updateVideoStatus(
                        vid ?: return,
                        UploadState.UPLOADING_MEDIUM
                    )
                    fullBytes = File(vid?.mediumRes).readBytes()
                    getSliceOfDataForUpload()
                    //chunkToUpload = baseChunkSize
                }
                UploadState.UPLOADED_MEDIUM -> {
                    if (userEnabledHDUploads) {
                        manager.updateVideoStatus(
                            vid ?: return,
                            UploadState.UPLOADING_HIGH
                        )
                        isHighQuality = true
                        // getVideoForUpload(vid ?: return)
                        fullBytes = File(vid?.highRes).readBytes()
                        //chunkToUpload = baseChunkSize
                    }
                }
                else -> {
                }
            }
        }

    }

    var chunkOrRemainder: Int = 0
    private var startRange = 0
    private fun getSliceOfDataForUpload() {
        remainder = fullBytes.size.minus((startRange + maxChunkSize))
        // println("FULL DATA : ${fullBytes.size} ++ REMAINDER $remainder")
        if (remainder > maxChunkSize) {
            val endRange = startRange + maxChunkSize
            val slice = fullBytes.sliceArray(
                IntRange(
                    startRange,
                    endRange - 1
                )
            )
            Log.d(
                "Upload",
                "FIRST IF---- start $startRange, end $endRange remaining $remainder, slice ${slice.size}"
            )
            startRange += maxChunkSize
            synchronized(this) {
                upload(
                    chunkIndex = uploadChunkIndex,
                    bytes = slice,
                    finalChunk = false
                )
            }


            //getSliceOfDataForUpload()
        } else {
            val endRange = startRange + remainder
            val slice = fullBytes.sliceArray(
                IntRange(
                    startRange,
                    endRange - 1
                )
            )
            Log.d(
                "Upload",
                "Else---- start $startRange, end $endRange remaining $remainder, slice ${slice.size} Full size ${fullBytes.size}"
            )

            synchronized(this) {
                upload(
                    chunkIndex = uploadChunkIndex,
                    bytes = slice,
                    finalChunk = false
                )
            }
            if (endRange != fullBytes.size) {
                val r = fullBytes.size.minus(endRange)
                val final = fullBytes.sliceArray(
                    IntRange(
                        endRange,
                        fullBytes.size - 1
                    )
                )
                synchronized(this) {
                    upload(
                        chunkIndex = uploadChunkIndex,
                        bytes = final,
                        finalChunk = true
                    )
                }
                Log.d(
                    "Upload",
                    "FINAL---- start $endRange, end ${fullBytes.size - 1} remaining $r, slice ${final.size} Full size ${fullBytes.size}"
                )
            }
        }
    }

    var f: Boolean = false


    var isHighQuality: Boolean = false
    @SuppressLint("CheckResult")
    var up: Disposable? = null

    private fun upload(chunkIndex: Int, bytes: ByteArray, finalChunk: Boolean) {
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
                        if (it.code() == 502) {
                            getSliceOfDataForUpload()
                        }
                        Log.d("TAG", "final chunk? $finalChunk")
//                        chunkToUpload = maxChunkSize
                        if (!finalChunk) {
                            uploadChunkIndex++
                            getSliceOfDataForUpload()
                        } else checkForComplete()

                    }
                }
                .doOnError {
                    Toast.makeText(view.context, "ERROR", Toast.LENGTH_SHORT).show()
//                    startRange = 0
//                    uploadChunkIndex = 0
//                    manager.resetUploadStateForCurrentVideo(
//                        currentVideo = currentVideo ?: return@doOnError
//                    )
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
        complete = uploadManager
            .onCompleteUpload(encryptionResponse?.upload?.id ?: "")
            .doOnError {
                manager.resetUploadStateForCurrentVideo(
                    currentVideo = currentVideo ?: return@doOnError
                )
            }
            .subscribe({
                if (it.code() == 502) {
                    this.continueUploadProcess()
                }
                when (it.body()?.status) {
                    CompleteResponse.COMPLETING.name -> pingServerForStatus()
                    CompleteResponse.COMPLETED.name -> {
                        firstRun = true
                        finalizeUpload(it.body()?.upload)
                    }
                    else -> manager.resetUploadStateForCurrentVideo(
                        currentVideo = vid ?: return@subscribe
                    )
                }
            }, {
                it.printStackTrace()
            })
    }

    var serverDis: Disposable? = null
    private fun finalizeUpload(upload: Upload?) {
        when {
            vid?.uploadState == UploadState.UPLOADING_MEDIUM -> {
                manager.updateVideoStatus(
                    vid ?: return, UploadState.UPLOADED_MEDIUM
                )
                getVideoForUpload(vid ?: return)
            }
            vid?.uploadState == UploadState.UPLOADING_HIGH -> {
                manager.updateVideoStatus(
                    vid ?: return, UploadState.UPLOADED_HIGH
                )
                getVideoForUpload(vid ?: return)
            }
            else -> {
            }
        }
        val path = when (isHighQuality) {
            true -> vid?.highRes
            else -> vid?.mediumRes
        }
        println("UPLOAD STATE ::: ${vid?.uploadState}")
        getVideoDimensions(path = path ?: "")
        serverDis = uploadManager
            .writerToServerAfterComplete(
                uploadId = vid?.id ?: "",
                S3Key = upload?.S3Key ?: "",
                vidWidth = width,
                vidHeight = height,
                hq = isHighQuality,
                vid = vid ?: return
            )
            .doOnSuccess {
                if (vid?.uploadState == UploadState.UPLOADED_HIGH) manager.updateVideoStatus(
                    vid ?: return@doOnSuccess, UploadState.COMPLETE
                )

            }
            .subscribe({
                println("WRITE TO SERVER SUCCESS")
            }, {
                println("ERROR FROM WRITE TO SERVER")
                it.printStackTrace()
                manager.resetUploadStateForCurrentVideo(vid ?: return@subscribe)
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
}

enum class CompleteResponse {
    COMPLETING,
    COMPLETED,
    FAILED
}