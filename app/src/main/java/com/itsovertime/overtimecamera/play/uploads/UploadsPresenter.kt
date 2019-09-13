package com.itsovertime.overtimecamera.play.uploads

import android.annotation.SuppressLint
import android.media.MediaMetadataRetriever
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

    var uploadInProcess: Boolean = false
    var clientIdDisposable: Disposable? = null
    var mediumUploads = mutableListOf<SavedVideo>()
    private fun subscribeToCurrentVideoBeingUploaded() {
        clientIdDisposable?.dispose()
        clientIdDisposable =
            uploadManager
                .onUpdatedMedQue()
                .subscribe({
                    println("QUE LIST:: ${it.size}")
                    it.forEach {
                        println("UPLOAD STATE FROM QUE:::: ${it.uploadState}")
                        if (it.uploadState == UploadState.QUEUED) {
                            getVideoInstance(it)
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
        managerDisposable?.dispose()
        managerDisposable = manager
            .subscribeToVideoGallery()
            .subscribe({
                videoList = it
                println("first...")
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
                    uploadInProcess = true
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
        awsDataDisposable?.dispose()
        awsDataDisposable =
            uploadManager
                .getAWSDataForUpload()
                .doOnError {
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
            }
            .subscribe({
                firstRun = true
                synchronized(this) {
                    continueUploadProcess()
                }
            }, {
                it.printStackTrace()
            })
    }


    private var minChunkSize = (0.5 * 1024).toInt()
    private var maxChunkSize = 2 * 1024 * 1024
    private var baseChunkSize = 1024
    private var chunkBasedOffResponseTime: Int = 0
    private var startRange = 0
    private var uploadChunkIndex = 0
    private var firstRun: Boolean = true
    private var fullBytes = 0
    private fun continueUploadProcess() {
        println("UploadState ::${vid?.uploadState}")

        if (firstRun) {
            when (vid?.uploadState) {
                UploadState.REGISTERED -> {
                    manager.updateVideoStatus(
                        vid ?: return,
                        UploadState.UPLOADING_MEDIUM
                    )
                    getVideoForUpload(vid ?: return)
                    fullBytes = File(vid?.mediumRes).readBytes().size
                }
                UploadState.UPLOADED_MEDIUM -> {
                    if (userEnabledHDUploads) {
                        manager.updateVideoStatus(
                            vid ?: return,
                            UploadState.UPLOADING_HIGH
                        )
                        isHighQuality = true
                        getVideoForUpload(vid ?: return)
                        fullBytes = File(vid?.highRes).readBytes().size
                    }
                }
                else -> {
                }
            }
        }


        chunkBasedOffResponseTime = if (!firstRun) {
            when (diffInSec) {
                in 0..1 -> maxChunkSize
                in 1..2 -> baseChunkSize
                else -> minChunkSize
            }
        } else {
            baseChunkSize
        }

//        view.updateAdapter(
//            videos = videoList ?: emptyList(),
//            data = ProgressData(
//                start = 0,
//                end = startRange,
//                isHighQuality = isHighQuality,
//                id = vid?.clientId.toString()
//            )
//        )
        firstRun = false
        val remainder = fullBytes - startRange
        val endRange = when (remainder < chunkBasedOffResponseTime) {
            true -> startRange + remainder - 1
            else -> startRange + chunkBasedOffResponseTime - 1
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
                checkForComplete()
            }
        }
        println("Remainder is $remainder Offset is: $startRange Chunk is: $chunkBasedOffResponseTime")
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
                        checkResponseTimeDifference(
                            timeSent = it.raw().sentRequestAtMillis(),
                            timeReceived = it.raw().receivedResponseAtMillis()
                        )
                        uploadChunkIndex++
                        startRange += chunkBasedOffResponseTime
                        continueUploadProcess()
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
                when (it.status) {
                    CompleteResponse.COMPLETING.name -> pingServerForStatus()
                    CompleteResponse.COMPLETED.name -> {
                        firstRun = true
                        finalizeUpload(it.upload)
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
        getVideoDimensions(path = path ?: "")

        serverDis?.dispose()
        serverDis = uploadManager
            .writerToServerAfterComplete(
                uploadId = vid?.id ?: "",
                S3Key = upload?.S3Key ?: "",
                vidWidth = width,
                vidHeight = height
            )
            .doOnSuccess {
                uploadInProcess = false
                if (vid?.uploadState == UploadState.UPLOADED_HIGH) manager.updateVideoStatus(
                    vid ?: return@doOnSuccess, UploadState.COMPLETE
                )
            }
            .subscribe({
                println("Response:: WRITE TO SERVER: $it")

            }, {
                println("ERROR FROM WRITE TO SERVER")
                it.printStackTrace()
                // manager.resetUploadStateForCurrentVideo(vid ?: return@subscribe)
            })
    }

    private fun pingServerForStatus() {
        val timer = Timer()
        val timerTask = object : TimerTask() {
            override fun run() {
                checkForComplete()
            }
        }
        timer.schedule(timerTask, 3000)
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