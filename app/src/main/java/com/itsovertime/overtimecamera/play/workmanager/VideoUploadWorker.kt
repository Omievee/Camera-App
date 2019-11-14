package com.itsovertime.overtimecamera.play.workmanager

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.work.*
import com.itsovertime.overtimecamera.play.db.AppDatabase
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.UploadState
import com.itsovertime.overtimecamera.play.network.EncryptedResponse
import com.itsovertime.overtimecamera.play.network.TokenResponse
import com.itsovertime.overtimecamera.play.network.Upload
import com.itsovertime.overtimecamera.play.network.VideoInstanceResponse
import com.itsovertime.overtimecamera.play.notifications.NotificationManager
import com.itsovertime.overtimecamera.play.progress.ProgressManager
import com.itsovertime.overtimecamera.play.progress.UploadsMessage
import com.itsovertime.overtimecamera.play.uploads.CompleteResponse
import com.itsovertime.overtimecamera.play.uploadsmanager.UploadsManager
import com.itsovertime.overtimecamera.play.videomanager.VideosManager
import io.reactivex.disposables.Disposable
import java.io.File
import java.lang.IllegalArgumentException
import java.lang.NumberFormatException
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class VideoUploadWorker(
    val context: Context,
    val workerParams: WorkerParameters
) :
    Worker(context, workerParams) {

    @Inject
    lateinit var uploadsManager: UploadsManager

    @Inject
    lateinit var videosManager: VideosManager

    @Inject
    lateinit var progressManager: ProgressManager

    @Inject
    lateinit var notifications: NotificationManager

    var hdReady: Boolean? = false
    var videos = mutableListOf<SavedVideo>()
    var instanceDisp: Disposable? = null

    @SuppressLint("CheckResult")


    override fun doWork(): Result {
        return try {
            println("SStarted work.....")
            hdReady = inputData.getBoolean("HD", false)

            getVideosFromDB()
            Result.success()
        } catch (throwable: Throwable) {
            println("Error from worker... ${throwable.cause}")
            throwable.printStackTrace()
            Result.failure()
        }
    }


    var queList = mutableListOf<SavedVideo>()
    var standardList = mutableListOf<SavedVideo>()
    var standardListHQ = mutableListOf<SavedVideo>()
    var faveList = mutableListOf<SavedVideo>()
    var faveListHQ = mutableListOf<SavedVideo>()

    var update: Disposable? = null
    private fun subscribeToUpdates() {
        update =
            videosManager
                .subscribeToVideoGallerySize()
                .subscribe({
                    println("size from gallery... $it")
                    println("size from gallery... ${faveList.size}")
                    println("size from gallery... ${standardList.size}")
                    if (it > 0 && faveList.size == 0 && standardList.size == 0) {

                    }
                }, {

                })
    }

    var vidDisp: Disposable? = null
    var db = AppDatabase.getAppDataBase(context = context)
    @SuppressLint("CheckResult")
    fun getVideosFromDB() {

        queList.clear()
        faveList.clear()
        standardList.clear()
        faveListHQ.clear()
        standardListHQ.clear()

        vidDisp = videosManager
            .onGetVideosForUpload()
            .map {
                queList = it as MutableList<SavedVideo>
                queList.removeIf {
                    it.highUploaded
                }
                val it = queList.iterator()
                while (it.hasNext()) {
                    val video = it.next()
                    if (video.is_favorite && !video.mediumUploaded) {
                        faveList.add(video)
                        it.remove()
                    } else if (!video.is_favorite && !video.mediumUploaded) {
                        standardList.add(video)
                        it.remove()
                    } else if (video.is_favorite && video.mediumUploaded) {
                        faveListHQ.add(video)
                        it.remove()
                    } else if (!video.is_favorite && video.mediumUploaded) {
                        standardListHQ.add(video)
                        it.remove()
                    }
                }
            }
            .doOnSuccess {
                beginProcess()
            }
            .subscribe({

            }, {

            })
    }

    @Synchronized
    private fun beginProcess() {
        serverDis?.dispose()
        awsDataDisposable?.dispose()
        complete?.dispose()
        up?.dispose()
        tokenDisposable?.dispose()


        val notifMsg = when (hdReady) {
            true -> "Uploading High Quality Videos.."
            else -> "Uploading Medium Quality Videos.."
        }

        println(
            "List Sizes :::: " +
                    "Fave List:: ${faveList.size} " +
                    "&& Standard List::${standardList.size} " +
                    "&& FaveHQ List:: ${faveListHQ.size} " +
                    "&& StandardHQ List:: ${standardListHQ.size}"
        )
        val mainListsAreEmpty = standardList.size == 0 && faveList.size == 0
        val HDListsAreEmpty = standardListHQ.isNotEmpty() && faveListHQ.isNotEmpty()

        when {
            faveList.size > 0 -> {
                progressManager.onCurrentUploadProcess(
                    UploadsMessage.Uploading_Medium
                )
                synchronized(this) {
                    if (!File(faveList[0].mediumRes).exists()) {
                        println("RESET FROM FILE CHECK -- FAVE")
                        videosManager.resetUploadStateForCurrentVideo(faveList[0])
                    } else {
                        requestTokenForUpload(faveList[0])
                        faveList.remove(faveList[0])
                    }
                }
            }
            standardList.size > 0 -> {
                progressManager.onCurrentUploadProcess(
                    UploadsMessage.Uploading_Medium
                )
                synchronized(this) {
                    if (!File(standardList[0].mediumRes).exists()) {
                        println("RESET FROM FILE CHECK -- MEDIUM")
                        videosManager.resetUploadStateForCurrentVideo(standardList[0])
                    } else {
                        requestTokenForUpload(standardList[0])
                        standardList.remove(standardList[0])
                    }
                }
            }
            faveListHQ.size > 0 && hdReady ?: false -> {
                progressManager.onCurrentUploadProcess(UploadsMessage.Uploading_High)
                synchronized(this) {
                    requestTokenForUpload(faveListHQ[0])
                    faveListHQ.remove(faveListHQ[0])
                }
            }
            standardListHQ.size > 0 && hdReady ?: false -> {
                progressManager.onCurrentUploadProcess(UploadsMessage.Uploading_High)
                synchronized(this) {
                    requestTokenForUpload(standardListHQ[0])
                    standardListHQ.remove(standardListHQ[0])
                }
            }
            faveList.size == 0 && standardList.size == 0 && faveListHQ.size > 0 && hdReady == false || standardListHQ.size > 0 && hdReady == false -> {
                progressManager.onCurrentUploadProcess(
                    UploadsMessage.Pending_High
                )
            }
            faveList.size == 0 && standardList.size == 0 && standardListHQ.size == 0 && faveListHQ.size == 0 -> {
                println("FINISHED WORK!!")
                videosManager.onNotifyWorkIsDone()
                progressManager.onCurrentUploadProcess(
                    UploadsMessage.Finished
                )
            }
        }
    }

    private var currentVideo: SavedVideo? = null
    private var tokenResponse: TokenResponse? = null
    private var tokenDisposable: Disposable? = null
    private var awsDataDisposable: Disposable? = null
    private fun requestTokenForUpload(savedVideo: SavedVideo) {
        currentVideo = savedVideo
        println("Current video being uploaded ---------> $currentVideo")
        awsDataDisposable =
            uploadsManager
                .getAWSDataForUpload()
                .doOnError {
                    it.printStackTrace()
                    println("RESET FROM TOKEN")
                    videosManager.resetUploadStateForCurrentVideo(currentVideo ?: return@doOnError)
                }
                .map {
                    tokenResponse = it
                    println("Token? $tokenResponse")
                }
                .doAfterNext {
                    beginUpload(token = tokenResponse)
                }
                .subscribe({
                }, {
                })
    }

    private var encryptionResponse: EncryptedResponse? = null
    private fun beginUpload(token: TokenResponse?) {
        println(" begin upload ------------> $token")
        tokenDisposable =
            uploadsManager
                .registerWithMD5(token ?: return, hdReady ?: false, currentVideo ?: return)
                .map {
                    encryptionResponse = it
                }
                .doAfterNext {
                    continueUploadProcess()

                }
                .doOnError {
                    println("RESET FROM BEGIN UPLOAD")
                    videosManager.resetUploadStateForCurrentVideo(
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
    private var fullBytes = byteArrayOf()
    var remainder: Int = 0
    var count = 0
    private fun continueUploadProcess() {
        println("current video in process -- $currentVideo")
        if (!currentVideo?.id.isNullOrEmpty()) {
            chunkToUpload = baseChunkSize
            if (currentVideo?.mediumUploaded == true && hdReady == true) {
                fullBytes = File(currentVideo?.encodedPath).readBytes()
                currentVideo?.uploadState = UploadState.UPLOADING_HIGH
                upload()
            } else {
                println("this is else ....")
                fullBytes = File(currentVideo?.mediumRes).readBytes()
                currentVideo?.uploadState = UploadState.UPLOADING_MEDIUM
                upload()
            }
        } else {
            println("RESET FROM NO UPLOAD ID")
            videosManager.resetUploadStateForCurrentVideo(currentVideo ?: return)
        }
    }

    private var startRange = 0
    private var chunkToUpload: Int = 0
    @SuppressLint("CheckResult")
    var up: Disposable? = null

    private fun upload() {
        val fullFileSize = fullBytes.size
        println("Filesize -------- $fullFileSize")
        val previousStartPlusDynamicChunk = startRange + chunkToUpload
        val end = minOf(fullFileSize, previousStartPlusDynamicChunk)
        val chunkFromFile = fullBytes.sliceArray(
            IntRange(
                startRange,
                end - 1
            )
        )
        val progress = (end * 100L / fullFileSize).toInt()
        progressManager.onUpdateProgress(currentVideo?.clientId ?: "", progress, hdReady ?: false)

        synchronized(this) {
            up = uploadsManager
                .uploadVideoToServer(
                    upload = encryptionResponse?.upload ?: return@synchronized,
                    array = chunkFromFile,
                    chunk = uploadChunkIndex
                )
                .doAfterNext {
                    if (it?.code() == 502) {
                        upload()
                    }
                    if (it.body()?.success == true) {
                        chunkToUpload = when (checkResponseTimeDifference(
                            timeSent = it.raw().sentRequestAtMillis(),
                            timeReceived = it.raw().receivedResponseAtMillis()
                        )) {
                            in 0..2 -> maxChunkSize
                            in 3..4 -> baseChunkSize
                            else -> minChunkSize
                        }

                        if (startRange >= end) {
                            checkForComplete()
                            startRange = 0
                            chunkToUpload = 0
                            uploadChunkIndex = 0
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
                    println("RESET FROM UPLOAD ERROR")
                    videosManager.resetUploadStateForCurrentVideo(
                        currentVideo = currentVideo ?: return@doOnError
                    )
                }
                .subscribe({
                }, {
                    println("THIS IS AN ERROR......... ____________________++STACKTRACE++________________________")
                    it.printStackTrace()
                })
        }
    }

    private fun checkResponseTimeDifference(timeSent: Long, timeReceived: Long): Long {
        val timeDif = timeReceived - timeSent
        return TimeUnit.MILLISECONDS.toSeconds(timeDif)
    }


    var complete: Disposable? = null
    @Synchronized
    private fun checkForComplete() {
        synchronized(this) {
            complete = uploadsManager
                .onCompleteUpload(encryptionResponse?.upload?.id ?: "")
                .doOnError {
                    println("ERROR FROM COMPLETE!!! ${it.message}")
                    println("ERROR FROM COMPLETE!!! ${it.cause}")
                    println("RESET FROM COMPLETE CHECK")
                    videosManager.resetUploadStateForCurrentVideo(
                        currentVideo = currentVideo ?: return@doOnError
                    )
                    it.printStackTrace()
                }
                .subscribe({
                    if (it.code() == 502) {
                        checkForComplete()
                    }
                    when (it.body()?.status) {
                        CompleteResponse.COMPLETING.name -> pingServerForStatus()
                        CompleteResponse.COMPLETED.name -> finalizeUpload(it.body()?.upload)
                        else -> pingServerForStatus()
                    }
                }, {
                    it.printStackTrace()
                })
        }
    }


    private var serverDis: Disposable? = null
    @Synchronized
    private fun finalizeUpload(upload: Upload?) {
        val path = when (hdReady) {
            true -> currentVideo?.trimmedVidPath
            else -> currentVideo?.mediumRes
        }

        synchronized(this) {
            try {
                getVideoDimensions(path = path ?: "")
            } catch (arg: IllegalAccessException) {
                arg.printStackTrace()
                println("RESET FROM DIMENSIONS EXCEPTION")
                videosManager.resetUploadStateForCurrentVideo(currentVideo ?: return)
            }

            serverDis = uploadsManager
                .writerToServerAfterComplete(
                    uploadId = currentVideo?.id ?: "",
                    S3Key = upload?.S3Key ?: "",
                    vidWidth = width,
                    vidHeight = height,
                    hq = hdReady ?: false,
                    vid = currentVideo ?: return
                )
                .doAfterNext {
                    progressManager.onUpdateProgress(
                        currentVideo?.clientId ?: "",
                        100,
                        hdReady ?: false
                    )
                    if (currentVideo?.uploadState == UploadState.UPLOADING_MEDIUM) {
                        currentVideo?.uploadState = UploadState.UPLOADED_MEDIUM
                        videosManager.updateMediumUploaded(true, currentVideo?.clientId ?: "")

                    } else {
                        currentVideo?.uploadState = UploadState.UPLOADED_HIGH
                        videosManager.updateHighuploaded(true, currentVideo?.clientId ?: "")
                    }
                    getVideosFromDB()
                }
                .subscribe({

                },
                    {
                        it.printStackTrace()
                        videosManager.resetUploadStateForCurrentVideo(
                            currentVideo ?: return@subscribe
                        )
                    })
        }
    }


    private fun pingServerForStatus() {
        val timerTask = object : TimerTask() {
            override fun run() {
                synchronized(this) {
                    checkForComplete()
                }
            }
        }
        Timer().schedule(timerTask, 5000)
    }

    var width: Int = 0
    var height: Int = 0
    private fun getVideoDimensions(path: String) {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        var width = 0
        var height = 0
        try {
            width =
                Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH))
            height =
                Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT))
            retriever.release()
        } catch (nf: NumberFormatException) {
            videosManager.resetUploadStateForCurrentVideo(currentVideo ?: return)
            retriever.release()

        } catch (ia: IllegalArgumentException) {
            videosManager.resetUploadStateForCurrentVideo(currentVideo ?: return)
            retriever.release()
        }
        this.width = width
        this.height = height
    }
}


class DaggerWorkerFactory(
    private val uploads: UploadsManager,
    private val videos: VideosManager,
    private val progress: ProgressManager,
    private val notifications: NotificationManager
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        val workerKlass = Class.forName(workerClassName).asSubclass(Worker::class.java)
        val constructor =
            workerKlass.getDeclaredConstructor(Context::class.java, WorkerParameters::class.java)
        val instance = constructor.newInstance(appContext, workerParameters)

        when (instance) {
            is VideoUploadWorker -> {
                instance.uploadsManager = uploads
                instance.videosManager = videos
                instance.notifications = notifications
                instance.progressManager = progress
            }
        }
        return instance
    }
}
