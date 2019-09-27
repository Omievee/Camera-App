package com.itsovertime.overtimecamera.play.uploadsmanager

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
import com.itsovertime.overtimecamera.play.uploads.CompleteResponse
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
    context: Context,
    workerParams: WorkerParameters
) :
    Worker(context, workerParams) {

    @Inject
    lateinit var uploadsManager: UploadsManager

    @Inject
    lateinit var videosManager: VideosManager

    var videos = mutableListOf<SavedVideo>()
    var instanceDisp: Disposable? = null
    override fun doWork(): Result {

        try {
            processListForUploads(getVideosFromDB()?.blockingGet())


            when {
                faveList.size > 0 -> getVideoInstance(faveList[0])
                standardList.size > 0 -> getVideoInstance(standardList[0])

            }

            if (faveListHQ.size > 0 || standardListHQ.size > 0) {

            }

            return Result.success()
        } catch (throwable: Throwable) {

            return Result.failure()
        }

    }

    var queList = mutableListOf<SavedVideo>()
    var standardList = mutableListOf<SavedVideo>()
    var standardListHQ = mutableListOf<SavedVideo>()
    var faveList = mutableListOf<SavedVideo>()
    var faveListHQ = mutableListOf<SavedVideo>()
    private fun processListForUploads(v: List<SavedVideo>?) {
        queList.clear()
        standardList.clear()
        standardListHQ.clear()
        faveList.clear()
        faveListHQ.clear()


        v?.forEach {
            if (it.is_favorite) {
                queList.add(0, it)
            } else queList.add(it)
        }
        queList.removeIf {
            it.highUploaded
        }

        queList.forEach {
            if (it.is_favorite && !it.mediumUploaded) {
                faveList.add(0, it)
            } else if (!it.is_favorite && !it.mediumUploaded) {
                standardList.add(0, it)
            } else if (it.is_favorite && it.mediumUploaded) {
                faveListHQ.add(0, it)
            } else if (!it.is_favorite && it.mediumUploaded) {
                standardListHQ.add(0, it)
            }
        }

        println("Size is.... ${standardList.size}")
        println("Size is.... ${faveList.size}")
        println("Size is.... ${standardListHQ.size}")
        println("Size is.... ${faveListHQ.size}")
        println("Size is.... ${queList.size}")
    }

    var db = AppDatabase.getAppDataBase(context = context)
    @SuppressLint("CheckResult")
    fun getVideosFromDB(): Single<List<SavedVideo>>? {
        return db?.videoDao()
            ?.getVideosForUpload()
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
    }

    var currentVideo: SavedVideo? = null
    @Synchronized
    private fun getVideoInstance(it: SavedVideo?) {
        currentVideo = it
        instanceDisp = uploadsManager
            .getVideoInstance(it)
            .map {
                videoInstanceResponse = it
            }
            .doOnSuccess {
                currentVideo?.id = videoInstanceResponse?.video?.id.toString()
                requestTokenForUpload()
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

    var videoInstanceResponse: VideoInstanceResponse? = null
    private var tokenResponse: TokenResponse? = null
    private var tokenDisposable: Disposable? = null
    private var awsDataDisposable: Disposable? = null
    private fun requestTokenForUpload() {
        awsDataDisposable =
            uploadsManager
                .getAWSDataForUpload()
                .doOnError {
                    videosManager.resetUploadStateForCurrentVideo(currentVideo ?: return@doOnError)
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
            uploadsManager
                .registerWithMD5(token ?: return)
                .map {
                    println("Encrypt... $it")
                    encryptionResponse = it
                }
                .doOnSuccess {
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
            if (currentVideo?.mediumUploaded == true) {
                fullBytes = File(currentVideo?.highRes).readBytes()
                isHighQuality = true
                currentVideo?.uploadState = UploadState.UPLOADING_HIGH
                upload()
            } else {
                fullBytes = File(currentVideo?.mediumRes).readBytes()
                currentVideo?.uploadState = UploadState.UPLOADING_MEDIUM
                upload()
            }
        } else {
            videosManager.resetUploadStateForCurrentVideo(currentVideo ?: return)
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
            up = uploadsManager
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
                    videosManager.resetUploadStateForCurrentVideo(
                        currentVideo = currentVideo ?: return@doOnError
                    )
                }
                .subscribe({
                }, {
                    it.printStackTrace()
                })
        }
    }

    private fun checkResponseTimeDifference(timeSent: Long, timeReceived: Long): Long {
        val timeDif = timeReceived - timeSent
        return TimeUnit.MILLISECONDS.toSeconds(timeDif)
    }


    var complete: Disposable? = null
    private fun checkForComplete() {
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


    private var serverDis: Disposable? = null
    private fun finalizeUpload(upload: Upload?) {
        val path = when (isHighQuality) {
            true -> currentVideo?.highRes
            else -> currentVideo?.mediumRes
        }
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
                hq = isHighQuality,
                vid = currentVideo ?: return
            )
            .doOnSuccess {
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


class DaggerWorkerFactory(private val uploads: UploadsManager, private val videos: VideosManager) :
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
            }
        }
        return instance
    }
}
