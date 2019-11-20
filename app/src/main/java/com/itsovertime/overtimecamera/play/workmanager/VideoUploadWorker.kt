package com.itsovertime.overtimecamera.play.workmanager

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.work.*
import com.itsovertime.overtimecamera.play.db.AppDatabase
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.UploadState
import com.itsovertime.overtimecamera.play.network.EncryptedResponse
import com.itsovertime.overtimecamera.play.network.TokenResponse
import com.itsovertime.overtimecamera.play.network.Upload
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
import java.lang.RuntimeException
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

    var interruptUpload: Boolean = false
    var uploading: Boolean = false

    override fun doWork(): Result {
        return try {
            hdReady = inputData.getBoolean("HD", false)
            subscribeToUpdates()
            subscribeToNewFaves()
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


    var TAG = "UPLOADING PROCESS"
    var update: Disposable? = null
    private fun subscribeToUpdates() {
        update =
            videosManager
                .subscribeToVideoGallerySize()
                .subscribe({
                    Log.d(TAG, "Subscribed to db updates.. $it... $uploading")
                    if (it > 0 && !uploading && hdReady == false) {
                        Log.d(TAG, "Getting videos $it && $uploading ")

                        getVideosFromDB()
                    }
                }, {

                })
    }

    var faves: Disposable? = null
    private fun subscribeToNewFaves() {
        faves =
            videosManager
                .subscribeToNewFavoriteVideoEvent()
                .subscribe({
                    Log.d(TAG, "Subscribed to favorite updates.. $it...")
                    if (it && (currentVideo?.is_favorite == false)) {
                        Log.d(TAG, "New Favorite! $it...")
                        interruptUpload = true
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
                Log.d(TAG, "Sorting Que....")
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

        if (mainListsAreEmpty) {
            uploading = false
        }
        Log.d(TAG, "Starting upload process... mainlist? $mainListsAreEmpty")
        when {
            faveList.size > 0 -> {
                progressManager.onCurrentUploadProcess(
                    UploadsMessage.Uploading_Medium
                )
                synchronized(this) {
                    Log.d(TAG, "uploading favorite video.... ${faveList[0]}.")
                    requestTokenForUpload(faveList[0])
                    faveList.remove(faveList[0])
                }
            }
            standardList.size > 0 -> {
                progressManager.onCurrentUploadProcess(
                    UploadsMessage.Uploading_Medium
                )
                synchronized(this) {
                    Log.d(TAG, "uploading standard video.... ${standardList[0]}.")
                    requestTokenForUpload(standardList[0])
                    standardList.remove(standardList[0])
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

    @Throws(RuntimeException::class)
    private fun fileIsNotCorrupt(file: File): Boolean {
        return if (file.readBytes().isEmpty()) {
            false
        } else {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, Uri.fromFile(file))
            val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
            val isVideo = "yes" == hasVideo
            retriever.release()
            isVideo
        }
    }

    private fun stopUploadForNewFavorite() {
        Log.d(TAG, "Stopping upload for new favorite....")
        interruptUpload = false
        currentVideo = null
        getVideosFromDB()
    }

    private var currentVideo: SavedVideo? = null
    private var tokenResponse: TokenResponse? = null
    private var tokenDisposable: Disposable? = null
    private var awsDataDisposable: Disposable? = null
    private fun requestTokenForUpload(savedVideo: SavedVideo) {
        uploading = true
        currentVideo = savedVideo

        Log.d(TAG, "getting token......")
        if (!interruptUpload) {
            awsDataDisposable =
                uploadsManager
                    .getAWSDataForUpload()
                    .doOnError {
                        it.printStackTrace()
                        Log.d(TAG, "RESET FROM TOKEN.....")
                        uploading = false
                        videosManager.resetUploadStateForCurrentVideo(
                            currentVideo ?: return@doOnError
                        )
                    }
                    .map {
                        tokenResponse = it
                    }
                    .doAfterNext {
                        beginUpload(token = tokenResponse)
                    }
                    .subscribe({
                    }, {
                    })
        } else stopUploadForNewFavorite()
    }

    private var encryptionResponse: EncryptedResponse? = null
    private fun beginUpload(token: TokenResponse?) {
        Log.d(TAG, "being upload function......")
        if (!interruptUpload) {
            tokenDisposable =
                uploadsManager
                    .registerWithMD5(token ?: return, hdReady ?: false, currentVideo ?: return)
                    .map {
                        encryptionResponse = it
                    }
                    .doAfterNext {
                        checkFileStatusBeforeUpload()
                    }
                    .doOnError {
                        Log.d(TAG, "RESET FROM BEGIN UPLOAD......")
                        uploading = false
                        videosManager.resetUploadStateForCurrentVideo(
                            currentVideo = currentVideo ?: return@doOnError
                        )
                    }
                    .subscribe({
                    }, {
                    })
        } else stopUploadForNewFavorite()

    }

    private var minChunkSize = (0.5 * 1024).toInt()
    private var maxChunkSize = 1024 * 2 * 1024
    private var baseChunkSize = 1024
    private var uploadChunkIndex = 0
    private var fullBytes = byteArrayOf()
    var remainder: Int = 0
    var count = 0
    private fun checkFileStatusBeforeUpload() {
        Log.d(TAG, "CHECKING FILE......")
        if (!interruptUpload) {
            if (!currentVideo?.uploadId.isNullOrEmpty()) {
                chunkToUpload = baseChunkSize
                if (currentVideo?.mediumUploaded == true && hdReady == true) {
                    fullBytes = File(currentVideo?.encodedPath).readBytes()
                    currentVideo?.uploadState = UploadState.UPLOADING_HIGH
                    upload()
                } else {
                    fullBytes = File(currentVideo?.mediumRes).readBytes()
                    if (!fileIsNotCorrupt(File(currentVideo?.mediumRes)) || !File(currentVideo?.mediumRes).exists() || fullBytes.isEmpty()) {
                        uploading = false
                        videosManager.resetUploadStateForCurrentVideo(currentVideo ?: return)
                    } else {

                        currentVideo?.uploadState = UploadState.UPLOADING_MEDIUM
                        upload()
                    }
                }
            } else {
                Log.d(TAG, "RESETTING FROM CHECK......")
                uploading = false
                videosManager.resetUploadStateForCurrentVideo(currentVideo ?: return)
            }
        } else stopUploadForNewFavorite()

    }

    private var startRange = 0
    private var chunkToUpload: Int = 0
    @SuppressLint("CheckResult")
    var up: Disposable? = null

    private fun upload() {
        Log.d(TAG, "Good file, starting upload......")
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
                        if (!interruptUpload) {
                            if (startRange >= end) {
                                Log.d(TAG, "checking for completed upload......")
                                checkForComplete()
                                startRange = 0
                                chunkToUpload = 0
                                uploadChunkIndex = 0
                            } else {
                                startRange = end
                                uploadChunkIndex++
                                upload()
                            }
                        } else stopUploadForNewFavorite()

                    }
                }
                .doOnError {
                    startRange = 0
                    uploadChunkIndex = 0
                    println("RESET FROM UPLOAD ERROR")
                    uploading = false
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
            if (!interruptUpload) {
                complete = uploadsManager
                    .onCompleteUpload(encryptionResponse?.upload?.id ?: "")
                    .doOnError {
                        Log.d(TAG, "ERROR FROM COMPLETE CHECK......")
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
                            else -> {
                                uploading = false
                                videosManager.resetUploadStateForCurrentVideo(
                                    currentVideo ?: return@subscribe
                                )
                            }
                        }
                    }, {
                        it.printStackTrace()
                    })
            } else stopUploadForNewFavorite()

        }
    }


    private var serverDis: Disposable? = null
    @Synchronized
    private fun finalizeUpload(upload: Upload?) {
        Log.d(TAG, "FINALIZING UPLOAD........")
        val path = when (hdReady) {
            true -> currentVideo?.trimmedVidPath
            else -> currentVideo?.mediumRes
        }
        if (!interruptUpload) {
            synchronized(this) {
                try {
                    getVideoDimensions(path = path ?: "")
                } catch (arg: IllegalAccessException) {
                    arg.printStackTrace()
                    Log.d(TAG, "RESET FROM DIMENSIONS CHECK.......")
                    uploading = false
                    videosManager.resetUploadStateForCurrentVideo(currentVideo ?: return)
                }

                serverDis = uploadsManager
                    .writerToServerAfterComplete(
                        uploadId = currentVideo?.uploadId ?: "",
                        S3Key = upload?.S3Key ?: "",
                        vidWidth = width,
                        vidHeight = height,
                        hq = hdReady ?: false,
                        vid = currentVideo ?: return
                    )
                    .doOnError {
                        Log.d(TAG, "FINAL STEP ERROR!........")
                    }
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
                        Log.d(TAG, "REPEAT PROCESS.........")
                        getVideosFromDB()
                    }
                    .subscribe({

                    },
                        {
                            it.printStackTrace()
                            uploading = false
                            videosManager.resetUploadStateForCurrentVideo(
                                currentVideo ?: return@subscribe
                            )
                        })
            }
        } else stopUploadForNewFavorite()


    }


    private fun pingServerForStatus() {
        val timerTask = object : TimerTask() {
            override fun run() {
                synchronized(this) {
                    Log.d(TAG, "PING SERVER......")
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
