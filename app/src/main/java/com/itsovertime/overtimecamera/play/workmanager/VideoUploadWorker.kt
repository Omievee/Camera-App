package com.itsovertime.overtimecamera.play.workmanager

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.work.ListenableWorker
import androidx.work.Worker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.itsovertime.overtimecamera.play.db.AppDatabase
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.UploadState
import com.itsovertime.overtimecamera.play.network.EncryptedResponse
import com.itsovertime.overtimecamera.play.network.TokenResponse
import com.itsovertime.overtimecamera.play.network.Upload
import com.itsovertime.overtimecamera.play.network.VideoInstanceResponse
import com.itsovertime.overtimecamera.play.progress.ProgressManager
import com.itsovertime.overtimecamera.play.uploads.CompleteResponse
import com.itsovertime.overtimecamera.play.uploadsmanager.UploadsManager
import com.itsovertime.overtimecamera.play.videomanager.VideosManager
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.File
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

    var hdReady: Boolean = false
    var videos = mutableListOf<SavedVideo>()
    var instanceDisp: Disposable? = null

    @SuppressLint("CheckResult")
    override fun doWork(): Result {
        println("Started work...")
        try {

            getVideosFromDB().blockingGet()
            hdReady = inputData.getBoolean("HD", false)
            when (hdReady) {
                false -> progressManager.onSetMessageToMediumUploads()
                else -> progressManager.onSetMessageToHDUploads()
            }


            if (faveListHQ.size > 0 && faveList.isEmpty() || standardListHQ.size > 0 && standardList.isEmpty()) {
                // progressManager.onNotifyPendingUploads()
            }
            if (faveListHQ.isEmpty() || faveList.isEmpty() || standardListHQ.isEmpty() || standardList.isEmpty()) {
                //TODO -- notify all uploads complete
            }

//            WorkerUtils().makeStatusNotification(
//                message = "Uploading Video....",
//                context = context
//            )
            return Result.success()
        } catch (throwable: Throwable) {
            println("Error from worker... ${throwable.cause}")
            throwable.printStackTrace()
            return Result.failure()
        }

    }

    var queList = mutableListOf<SavedVideo>()
    var standardList = mutableListOf<SavedVideo>()
    var standardListHQ = mutableListOf<SavedVideo>()
    var faveList = mutableListOf<SavedVideo>()
    var faveListHQ = mutableListOf<SavedVideo>()

    var db = AppDatabase.getAppDataBase(context = context)
    @SuppressLint("CheckResult")
    fun getVideosFromDB(): Single<List<SavedVideo>> {
        serverDis?.dispose()
        awsDataDisposable?.dispose()
        complete?.dispose()
        up?.dispose()
        instanceDisp?.dispose()
        tokenDisposable?.dispose()

        queList.clear()
        faveList.clear()
        standardList.clear()
        faveListHQ.clear()
        standardListHQ.clear()

        return db!!.videoDao()
            .getVideosForUpload()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess {
                queList = it as MutableList<SavedVideo>
                println("Size... ${queList.size}")

                queList.removeIf {
                    it.highUploaded
                }

                val it = queList.iterator()
                while (it.hasNext()) {
                    val video = it.next()
                    println("Video is ${video.is_favorite} && ${video.mediumUploaded}")
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


                println("Fave list size: ${faveList.size}")
                println("Fave list size HQ: ${faveListHQ.size}")
                println("Standard list size: ${standardList.size}")
                println("Standard list size HQ: ${standardListHQ.size}")
                println("main list size.. ${queList.size}")

                when {
                    faveList.size > 0 -> {
                        synchronized(this) {
                            getVideoInstance(faveList[0])
                            faveList.remove(faveList[0])
                        }
                    }
                    standardList.size > 0 -> {
                        synchronized(this) {
                            getVideoInstance(standardList[0])
                            standardList.remove(standardList[0])
                        }
                    }
                }
            }

    }

    private var currentVideo: SavedVideo? = null
    @Synchronized
    private fun getVideoInstance(it: SavedVideo?) {
        currentVideo = it
        instanceDisp = uploadsManager
            .getVideoInstance(it)
            .map {
                videoInstanceResponse = it
            }
            .doAfterNext {
                currentVideo?.id = videoInstanceResponse?.video?.id.toString()
                requestTokenForUpload()
            }
            .doOnError {
                videosManager.resetUploadStateForCurrentVideo(
                    currentVideo = currentVideo ?: return@doOnError
                )
            }
            .subscribe({
            },
                {
                })

    }

    var videoInstanceResponse: VideoInstanceResponse? = null
    private var tokenResponse: TokenResponse? = null
    private var tokenDisposable: Disposable? = null
    private var awsDataDisposable: Disposable? = null
    private fun requestTokenForUpload() {
        println("Token...")
        awsDataDisposable =
            uploadsManager
                .getAWSDataForUpload()
                .doOnError {
                    it.printStackTrace()
                    println("made an error... ${it.message}")
                    videosManager.resetUploadStateForCurrentVideo(currentVideo ?: return@doOnError)
                }
                .map {
                    tokenResponse = it
                }
                .doAfterNext {
                    println("Success.... $tokenResponse")
                    beginUpload(token = tokenResponse)
                }
                .subscribe({
                }, {

                })
    }

    private var encryptionResponse: EncryptedResponse? = null
    private fun beginUpload(token: TokenResponse?) {
        println("BEGIN UPLOAD>... $token")
        tokenDisposable =
            uploadsManager
                .registerWithMD5(token ?: return, hdReady)
                .map {
                    println("Encrypt... $it")
                    encryptionResponse = it
                }
                .doAfterNext {
                    continueUploadProcess()
                }
                .doOnError {
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
    private var firstRun: Boolean = true
    private var fullBytes = byteArrayOf()
    var remainder: Int = 0
    var count = 0
    private fun continueUploadProcess() {
        println("Video ID: ${currentVideo?.id}")
        if (currentVideo?.id != "") {
            chunkToUpload = baseChunkSize
            if (currentVideo?.mediumUploaded == true && hdReady) {
                fullBytes = when (currentVideo?.trimmedVidPath) {
                    null -> File(currentVideo?.highRes).readBytes()
                    "" -> File(currentVideo?.highRes).readBytes()
                    else -> File(currentVideo?.trimmedVidPath).readBytes()
                }
                currentVideo?.uploadState = UploadState.UPLOADING_HIGH
                upload()
            } else {
                fullBytes = File(currentVideo?.mediumRes).readBytes()
                if (fullBytes.isEmpty()) {
                    return
                }
                currentVideo?.uploadState = UploadState.UPLOADING_MEDIUM
                println("medium.... ${currentVideo?.mediumRes}")
                upload()
            }
        } else {
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
        println("chunk ? $chunkToUpload")

        val sliceFromFullFile = fullBytes.sliceArray(
            IntRange(
                startRange,
                end - 1
            )
        )
        synchronized(this) {

            up = uploadsManager
                .uploadVideoToServer(
                    upload = encryptionResponse?.upload ?: return@synchronized,
                    array = sliceFromFullFile,
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
                            in 2..4 -> baseChunkSize
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
                    println("THIS IS AN ERROR......... ____________________________________________")
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
                    videosManager.resetUploadStateForCurrentVideo(
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
                            videosManager.resetUploadStateForCurrentVideo(
                                currentVideo = currentVideo ?: return@subscribe
                            )
                        }
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
                videosManager.resetUploadStateForCurrentVideo(currentVideo ?: return)
            }

            serverDis = uploadsManager
                .writerToServerAfterComplete(
                    uploadId = currentVideo?.id ?: "",
                    S3Key = upload?.S3Key ?: "",
                    vidWidth = width,
                    vidHeight = height,
                    hq = hdReady,
                    vid = currentVideo ?: return
                )
                .doAfterNext {
                    if (currentVideo?.uploadState == UploadState.UPLOADING_MEDIUM) {
                        videosManager.updateMediumUploaded(true, currentVideo?.clientId ?: "")
                        currentVideo?.uploadState = UploadState.UPLOADED_MEDIUM
                    } else {
                        currentVideo?.uploadState = UploadState.UPLOADED_HIGH
                        videosManager.updateHighuploaded(true, currentVideo?.clientId ?: "")
                    }

                }
                .subscribe({
                }, {
                    it.printStackTrace()
                    videosManager.resetUploadStateForCurrentVideo(currentVideo ?: return@subscribe)
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
        val width =
            Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH))
        val height =
            Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT))
        retriever.release()

        this.width = width
        this.height = height
    }
}


class DaggerWorkerFactory(
    private val uploads: UploadsManager,
    private val videos: VideosManager,
    private val progress: ProgressManager
) :
    WorkerFactory() {
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
                instance.progressManager = progress
            }
        }
        return instance
    }
}
