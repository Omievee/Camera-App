package com.itsovertime.overtimecamera.play.uploads

import android.annotation.SuppressLint
import android.media.MediaMetadataRetriever
import android.util.Log
import android.widget.Toast
import androidx.annotation.IntegerRes
import com.itsovertime.overtimecamera.play.db.AppDatabase
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.UploadState
import com.itsovertime.overtimecamera.play.model.UploadState.*
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
import kotlin.math.min

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
    fun subscribeToCurrentVideoBeingUploaded() {
        clientIdDisposable =
            uploadManager
                .onUpdateQue()
                .subscribe({
                    println("QUE LIST:: ${it.size}")
                    it.sortBy {
                        it.is_favorite
                    }
//                    it.forEach {
//                        println("Saved Video:: $it")
//                        if (it.uploadState == QUEUED) {
//                            getVideoInstance(it)
//                        } else if (it.uploadState == UPLOADED_MEDIUM && userEnabledHDUploads) {
//                            getVideoInstance(it)
//                        } else {
//                            manager.resetUploadStateForCurrentVideo(it)
//                        }
//                    }
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

    private var instanceDisposable: Disposable? = null
    var currentVideo: SavedVideo? = null
    @Synchronized
    private fun getVideoInstance(it: SavedVideo) {
        currentVideo = it
        updateState(currentVideo ?: return)
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
                    updateState(currentVideo ?: return@doOnSuccess)
                    // requestTokenForUpload()
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

    private var minChunkSize = (0.5 * 1024).toInt()
    private var maxChunkSize = 1024 * 2 * 1024
    private var baseChunkSize = 1024
    private var uploadChunkIndex = 0
    private var firstRun: Boolean = true
    private var fullBytes = byteArrayOf()
    var remainder: Int = 0
    var count = 0
    private fun continueUploadProcess() {
        println("Continuing... ${vid?.id}")
        if (vid?.id != "") {
            updateState(vid ?: return)
            chunkToUpload = baseChunkSize
            if (vid?.mediumUploaded == true) {
                fullBytes = File(vid?.highRes).readBytes()
                isHighQuality = true
                getSliceOfDataForUpload()
            } else {
                fullBytes = File(vid?.mediumRes).readBytes()
                getSliceOfDataForUpload()
            }
        } else manager.resetUploadStateForCurrentVideo(vid ?: return)
    }

    private var startRange = 0
    private var chunkToUpload: Int = 0
    private fun getSliceOfDataForUpload() {
        chunkToUpload = maxChunkSize
        val end = minOf(fullBytes.size, startRange + chunkToUpload)
        println("chunk ? $chunkToUpload")

        val slice = fullBytes.sliceArray(
            IntRange(
                startRange,
                end - 1
            )
        )
        if (startRange >= end) {
            println("COMPLETE___ ${fullBytes.size} start $startRange end $end sliced--- ${slice.size}")
            checkForComplete()
            startRange = 0
            chunkToUpload = 0
        } else {
            println("Continue....... ${fullBytes.size} start $startRange end $end sliced--- ${slice.size}")
//            startRange += chunkToUpload
//            getSliceOfDataForUpload()
            synchronized(this) {
                upload(chunkIndex = uploadChunkIndex, bytes = slice)
            }
        }
    }

    var isHighQuality: Boolean = false
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
//                        chunkToUpload = when (checkResponseTimeDifference(
//                            timeSent = it.raw().sentRequestAtMillis(),
//                            timeReceived = it.raw().receivedResponseAtMillis()
//                        )) {
//                            in 0..1 -> maxChunkSize
//                            in 1..2 -> baseChunkSize
//                            else -> minChunkSize
//                        }
                        if (it.code() == 502) {
                            getSliceOfDataForUpload()
                        }
                        chunkToUpload = maxChunkSize
                        startRange += chunkToUpload
                        uploadChunkIndex++
                        getSliceOfDataForUpload()
                    }
                }
                .doOnError {
                    Toast.makeText(view.context, "ERROR", Toast.LENGTH_SHORT).show()
//                    startRange = 0
//                    uploadChunkIndex = 0
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

                //                manager.resetUploadStateForCurrentVideo(
//                    currentVideo = currentVideo ?: return@doOnError
//                )
            }
            .subscribe({
                if (it.code() == 502) {
                    checkForComplete()  //20895161 -- 20895161
                }
                when (it.body()?.status) {
                    CompleteResponse.COMPLETING.name -> pingServerForStatus()
                    CompleteResponse.COMPLETED.name -> {
                        firstRun = true
                        finalVideo(it.body()?.upload, vid ?: return@subscribe)
                    }
                    else -> manager.resetUploadStateForCurrentVideo(
                        currentVideo = vid ?: return@subscribe
                    )
                }
            }, {
                it.printStackTrace()
            })
    }

    @SuppressLint("CheckResult")
    private fun getVideoForUpload(savedVideo: SavedVideo) {
        println("GETTTING VIDEO!")
        Single.fromCallable {
            with(videoDao) {
                this?.getVideoForUpload(savedVideo.clientId)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                vid = it
            }
            .doFinally {
                println("finally...")
                continueUploadProcess()
            }
            .subscribe({
            }, {
                it.printStackTrace()
            })
    }

    var vid2: SavedVideo? = null
    @SuppressLint("CheckResult")
    private fun finalVideo(upload: Upload?, savedVideo: SavedVideo) {
        Single.fromCallable {
            with(videoDao) {
                this?.getVideoForUpload(savedVideo.clientId)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                vid2 = it
            }
            .subscribe({
                finalizeUpload(upload)
            }, {
                it.printStackTrace()
            })
    }


    var serverDis: Disposable? = null
    private fun finalizeUpload(upload: Upload?) {
        println("Status?? ${vid2?.uploadState}")
        updateState(vid2 ?: return)
        val path = when (isHighQuality) {
            true -> vid2?.highRes
            else -> vid2?.mediumRes
        }
        try {
            getVideoDimensions(path = path ?: "")
        } catch (arg: IllegalAccessException) {
            arg.printStackTrace()
            manager.resetUploadStateForCurrentVideo(vid2 ?: return)
        }

        serverDis = uploadManager
            .writerToServerAfterComplete(
                uploadId = vid?.id ?: "",
                S3Key = upload?.S3Key ?: "",
                vidWidth = width,
                vidHeight = height,
                hq = isHighQuality,
                vid = vid ?: return
            )

            .subscribe({
            }, {
                it.printStackTrace()
                manager.resetUploadStateForCurrentVideo(vid ?: return@subscribe)
            })
    }

    fun updateState(vid: SavedVideo) {
        when (vid.uploadState) {
            QUEUED -> manager.updateVideoStatus(vid, REGISTERING)
            REGISTERING -> manager.updateVideoStatus(vid, REGISTERED)
            REGISTERED -> manager.updateVideoStatus(vid, UPLOADED_MEDIUM)
            UPLOADED_HIGH -> manager.updateVideoStatus(vid, COMPLETE)
            else -> manager.resetUploadStateForCurrentVideo(vid)
        }
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