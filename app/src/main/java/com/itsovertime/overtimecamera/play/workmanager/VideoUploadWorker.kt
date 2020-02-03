package com.itsovertime.overtimecamera.play.workmanager

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.work.*
import com.crashlytics.android.Crashlytics
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException
import com.itsovertime.overtimecamera.play.analytics.OTAnalyticsManager
import com.itsovertime.overtimecamera.play.db.AppDatabase
import com.itsovertime.overtimecamera.play.filemanager.FileManager
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.UploadState
import com.itsovertime.overtimecamera.play.network.EncryptedResponse
import com.itsovertime.overtimecamera.play.network.TokenResponse
import com.itsovertime.overtimecamera.play.network.Upload
import com.itsovertime.overtimecamera.play.notifications.NotificationManager
import com.itsovertime.overtimecamera.play.progressmanager.ProgressManager
import com.itsovertime.overtimecamera.play.progressmanager.UploadsMessage
import com.itsovertime.overtimecamera.play.uploads.CompleteResponse
import com.itsovertime.overtimecamera.play.uploadsmanager.UploadsManager
import com.itsovertime.overtimecamera.play.videomanager.VideosManager
import com.itsovertime.overtimecamera.play.videomanager.VideosManagerImpl
import com.itsovertime.overtimecamera.play.wifimanager.NETWORK_TYPE
import com.itsovertime.overtimecamera.play.wifimanager.WifiManager
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.lang.Exception
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

    @Inject
    lateinit var analyticsManager: OTAnalyticsManager

    @Inject
    lateinit var wifiManager: WifiManager

    @Inject
    lateinit var fileManager: FileManager

    private var hdReady: Boolean? = false


    @SuppressLint("CheckResult")
    var uploading: Boolean = false
    var isConnectedToInternet: Boolean = false
    private var stopUploadForNewFavorite: Boolean = false
    override fun doWork(): Result {
        return try {
            subscribeToUpdates()
            subscribeToNewFaves()
            subscribeToHDSwitch()
            subscribeToEncodeComplete()
            subscribeToWifiStatus()
            Result.success()
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
            println("Error from worker... ${throwable.cause}")
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
                .subscribeToNewVideos()
                .subscribe({
                    Log.d(TAG, "Subscribed to db updates.. $it... $uploading")
                    if (it && !uploading) {
                        Log.d(TAG, "Getting videos $it && $uploading ")
                        getVideosFromDB()
                    }
                }, {
                    it.printStackTrace()
                })
    }


    var wifiD: Disposable? = null
    private fun subscribeToWifiStatus() {
        wifiD =
            wifiManager
                .subscribeToNetworkUpdates()
                .subscribe({
                    println("Network check from upload worker... $it")
                    isConnectedToInternet = when (it) {
                        NETWORK_TYPE.UNKNOWN -> {
                            uploadingIsFalse()
                            false
                        }
                        else -> {
                            if (!uploading) {
                                getVideosFromDB()
                            }
                            true
                        }
                    }
                }, {
                    it.printStackTrace()
                })
    }


    private fun isDeviceConnected(): Boolean {
        return isConnectedToInternet
    }

    var en: Disposable? = null
    private fun subscribeToEncodeComplete() {
        en = videosManager
            .subscribeToEncodeComplete()
            .subscribe({
                currentVideo = it
            }, {

            })
    }

    var faves: Disposable? = null
    private fun subscribeToNewFaves() {
        faves =
            videosManager
                .subscribeToNewFavoriteVideoEvent()
                .subscribe({
                    if (!currentVideo?.is_favorite!!) {
                        stopUploadForNewFavorite = true
                    }
                }, {
                    it.printStackTrace()
                })
    }

    var hdSwitch: Disposable? = null
    private fun subscribeToHDSwitch() {
        hdSwitch =
            videosManager
                .subscribeToHDSwitch()
                .subscribe({
                    hdReady = it
                    if (!uploading) {
                        getVideosFromDB()
                    }
                }, {
                    it.printStackTrace()
                })

    }


    var vidDisp: Disposable? = null
    var db = AppDatabase.getAppDataBase(context = context)
    @SuppressLint("CheckResult")
    fun getVideosFromDB() {
        println("Started db query...")
        queList.clear()
        faveList.clear()
        standardList.clear()
        faveListHQ.clear()
        standardListHQ.clear()
        var num: Int = 0
        vidDisp?.dispose()
        vidDisp = videosManager
            .onGetVideosForUpload()
            .map {
                queList = it as MutableList<SavedVideo>
                queList.removeIf {
                    it.highUploaded
                }
                val it = queList.iterator()
                println("QUE LIST =====================================================================")
                queList.forEach {
                    if (it.videoId.isEmpty()) {
                        println("REGISTER.....")
                       videosManager.onRegisterVideoWithServer(false, it)
                    }
                }
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

    var uploadingHD: Boolean = false
    @Synchronized
    private fun beginProcess() {
        serverDis?.dispose()
        awsDataDisposable?.dispose()
        complete?.dispose()
        up?.dispose()
        tokenDisposable?.dispose()

        println(
            "List Sizes :::: " +
                    "Fave List:: ${faveList.size} " +
                    "&& Standard List::${standardList.size} " +
                    "&& FaveHQ List:: ${faveListHQ.size} " +
                    "&& StandardHQ List:: ${standardListHQ.size}"
        )
        val mainListsAreEmpty = standardList.size == 0 && faveList.size == 0
        val HDListsAreEmpty = standardListHQ.isEmpty() && faveListHQ.isEmpty()

        if (mainListsAreEmpty) {
            uploadingIsFalse()
        }
        if (HDListsAreEmpty) {
            hdReady = false
            uploadingHD = false
            notifications.onUpdateProgressNotification(
                "HD Uploads complete!"
            )
        }
        println("pre when...")
        when {
            faveList.size > 0 -> {
                progressManager.onCurrentUploadProcess(
                    UploadsMessage.Uploading_Medium
                )
                determineVideoStatus(faveList[0], faveList)
            }
            standardList.size > 0 -> {
                progressManager.onCurrentUploadProcess(
                    UploadsMessage.Uploading_Medium
                )
                println("standard logic... ${standardList[0]}")
                determineVideoStatus(standardList[0], standardList)
            }
            faveListHQ.size > 0 && hdReady ?: false && faveList.isEmpty() && standardList.isEmpty() -> {
                progressManager.onCurrentUploadProcess(UploadsMessage.Uploading_High)
                notifications.onCreateProgressNotification(
                    "Uploading HD Videos",
                    "Do not close the app",
                    true
                )
                synchronized(this) {
                    uploadingIsTrue()
                    uploadingHD = true
                    encodeVideoForUpload(faveListHQ[0])
                    faveListHQ.remove(faveListHQ[0])
                }
            }
            standardListHQ.size > 0 && hdReady ?: false && faveList.isEmpty() && standardList.isEmpty() -> {
                progressManager.onCurrentUploadProcess(UploadsMessage.Uploading_High)
                notifications.onCreateProgressNotification(
                    "Uploading HD Videos",
                    "Do not close the app",
                    true
                )
                synchronized(this) {
                    uploadingIsTrue()
                    uploadingHD = true
                    encodeVideoForUpload(standardListHQ[0])
                    standardListHQ.remove(standardListHQ[0])
                }
            }
            faveList.size == 0 && standardList.size == 0 && faveListHQ.size > 0 && hdReady == false || standardListHQ.size > 0 && hdReady == false -> {
                progressManager.onCurrentUploadProcess(
                    UploadsMessage.Pending_High
                )
            }
            faveList.size == 0 && standardList.size == 0 && standardListHQ.size == 0 && faveListHQ.size == 0 -> {
                progressManager.onCurrentUploadProcess(
                    UploadsMessage.Finished
                )
            }
        }
    }

    val start = "Process Start"
    private fun determineVideoStatus(video: SavedVideo, list: MutableList<SavedVideo>) {
        currentVideo = video
        println("This is the video to upload... $currentVideo")
        when (video?.videoId.isEmpty()) {
            true -> {
                println("this is empty....${video.videoId}")
                reset(video, VideosManagerImpl.RESET.RESET_NO_VIDEO_ID, start)
            }
            else -> {
                when (fileCheck(video)) {
                    true -> {
                        requestTokenForUpload(video)
                        list.remove(list[0])
                    }
                    else -> {
                        reset(video, VideosManagerImpl.RESET.RESET_CORRUPT_FILE, start)
                    }
                }
            }
        }
    }

    private fun fileCheck(currentVideo: SavedVideo?): Boolean {
        var valid = false
        val path = currentVideo?.mediumRes != null
        if (!path) {
            return valid
        }
        val goodFile = File(currentVideo?.mediumRes).exists()
        if (!goodFile) {
            return valid
        } else if (File(currentVideo?.mediumRes).readBytes().isEmpty()) {
            return valid
        }
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, Uri.fromFile(File(currentVideo?.mediumRes)))
        val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
        val isVideo = "yes" == hasVideo
        retriever.release()
        if (isVideo) {
            valid = true
            return valid
        }
        return valid
    }


    fun reset(video: SavedVideo, reason: VideosManagerImpl.RESET, stage: String) {
        uploadingIsFalse()
        videosManager.onResetCurrentVideo(video, reason, stage)
    }


    private fun fileForHDEncodedVideo(fileName: String, clientId: String): File {
        val mediaStorageDir =
            File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "UploadHD")
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            println("Failed....")
        }
        videosManager.onUpdateEncodedPath(
            File(mediaStorageDir.path + File.separator + "1080.$fileName").absolutePath,
            clientId
        )
        return File(mediaStorageDir.path + File.separator + "1080.$fileName")
    }


    @SuppressLint("CheckResult")
    private fun encodeVideoForUpload(savedVideo: SavedVideo) {
        var ffmpeg: FFmpeg = FFmpeg.getInstance(context)
        if (!savedVideo.encodedPath.isNullOrEmpty()) {
            if (fileManager.onDoesFileExist(savedVideo.encodedPath.toString())) {
                fileManager.onDeleteFile(savedVideo.encodedPath.toString())
            }
        }

        println("File to upload... $savedVideo")
        println("what is this file.... ${savedVideo.trimmedVidPath.isNullOrEmpty()}")
        println("what is this file.... ${savedVideo.trimmedVidPath == null}")
        println("what is this file.... ${savedVideo.trimmedVidPath.equals("null")}")

        val encodeFile = if (savedVideo.trimmedVidPath.equals("null") || savedVideo.trimmedVidPath == null || savedVideo.trimmedVidPath == "") {
            File(savedVideo.highRes)
        } else {
            File(savedVideo.trimmedVidPath)
        }

        println("This is a file... $encodeFile")
        Single.fromCallable {
            val newFile =
                fileForHDEncodedVideo(encodeFile.name, savedVideo.clientId)
            val encodeCommand = arrayOf(
                // I command reads from designated input file
                "-i",
                // file input
                encodeFile.absolutePath,
                // video codec to write to
                "-vcodec",
                // value of codec - h264
                "h264",
                "-preset",
                "ultrafast",
                "-crf",
                "28",
                "-maxrate",
                "16000k",
                "-bufsize",
                "16000k",
                "-filter:v",
                "fps=60",
                // new file
                newFile.absolutePath
            )
            try {
                synchronized(this) {
                    ffmpeg.execute(encodeCommand, object : ExecuteBinaryResponseHandler() {
                        override fun onStart() {
                            super.onStart()
                            println("STARTING ENCODE!!")
                        }

                        override fun onSuccess(message: String?) {
                            super.onSuccess(message)
                            println("ENCODE Successful... $message")

                        }

                        override fun onProgress(message: String?) {
                            super.onProgress(message)
                            println("encode progress $message")
                        }

                        override fun onFinish() {
                            super.onFinish()
                            println("ENCODING FINISHED...")
                            getNewHDVideoForUpload(currentVideo ?: return)
                        }

                        override fun onFailure(message: String?) {
                            super.onFailure(message)
                            println("ENCODING FAILED $message")
                            uploadingIsFalse()
//                            encodeVideoForUpload(currentVideo ?: return)
                            Crashlytics.log("Failed to execute ffmpeg -- $message")
                        }
                    })
                }
            } catch (e: FFmpegCommandAlreadyRunningException) {
                println("FFMPEG ALREADY RUNNING :: ${e.message}")
                ffmpeg.killRunningProcesses()
                val delay = object : TimerTask() {
                    override fun run() {
                        encodeVideoForUpload(currentVideo ?: return)
                    }
                }
                Timer().schedule(delay, 2000)
                Crashlytics.log("FFMPEG -- ${e.message}")
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
            }, {
                it.printStackTrace()
            })
    }

    var encodeDisp: Disposable? = null
    private fun getNewHDVideoForUpload(video: SavedVideo) {
        encodeDisp = videosManager
            .onGetEncodedVideo(video.clientId)
            .map {
                println("Retrieved video $it")
                currentVideo = it
                if (!File(it?.encodedPath).exists()) {
                    println("Wtf.. file is gone... ")
                    // encodeVideoForUpload(currentVideo ?: return@map)
                } else requestTokenForUpload(currentVideo ?: return@map)
            }
            .doOnError {
                uploadingIsFalse()
                videosManager.onResetCurrentVideo(currentVideo ?: return@doOnError, VideosManagerImpl.RESET.RESET_UNKOWN, "Get Encode From DB")
            }
            .subscribe({
            }, {
            })
    }


    private fun stopUploadForNewFavorite() {
        Log.d(TAG, "Stopping upload for new favorite....")
        stopUploadForNewFavorite = false
        hdReady = false
        currentVideo = null
        getVideosFromDB()
    }

    private var currentVideo: SavedVideo? = null
    private var tokenResponse: TokenResponse? = null
    private var tokenDisposable: Disposable? = null
    private var awsDataDisposable: Disposable? = null
    private fun requestTokenForUpload(savedVideo: SavedVideo) {
        println("")
        if (isDeviceConnected()) {
            awsDataDisposable =
                uploadsManager
                    .getAWSDataForUpload()
                    .retry(3)
                    .doOnError {
                        if (it.message.equals("HTTP 502 Bad Gateway") || it.message.equals("HTTP 504 Gateway Time-out")) {
                            requestTokenForUpload(savedVideo)
                        } else {
                            it.printStackTrace()
                            uploadingIsFalse()
//                            videosManager.onResetCurrentVideo(
//                                currentVideo ?: return@doOnError,
//                                reason
//                            )
                            analyticsManager.onTrackUploadEvent(
                                Failed_Token,
                                arrayOf(
                                    "client_id = ${savedVideo.clientId}",
                                    "failed_response = ${it.message}"
                                )
                            )
                        }
                    }


                    .doAfterNext {
                        analyticsManager.onTrackUploadEvent(
                            Upload_Token,
                            arrayOf(
                                "client_id = ${savedVideo.clientId}",
                                "s3_bucket = ${tokenResponse?.S3Bucket}",
                                "s3_key = ${tokenResponse?.S3Key} "
                            )
                        )
                        beginUpload(token = it, video = savedVideo)
                    }
                    .subscribe({
                    }, {
                    })
        }
    }

    private var encryptionResponse: EncryptedResponse? = null
    private fun beginUpload(token: TokenResponse?, video: SavedVideo) {
        if (isDeviceConnected()) {
            tokenDisposable =
                token?.let {
                    uploadsManager
                        .registerWithMD5(it, uploadingHD, video)
                        .map {
                            encryptionResponse = it
                        }
                        .doAfterNext {
                            checkFileStatusBeforeUpload(video)
                            analyticsManager.onTrackUploadEvent(
                                Register_Upload,
                                arrayOf(
                                    "client_id = ${video.clientId}",
                                    "upload_id = ${video.videoId}"
                                )
                            )
                        }
                        .doOnError {
                            if (it.message.equals("HTTP 502 Bad Gateway")) {
                                beginUpload(token, video)
                            } else {
                                uploadingIsFalse()
                                analyticsManager.onTrackUploadEvent(
                                    Failed_Register_Upload,
                                    arrayOf(
                                        "client_id = ${video.clientId}",
                                        "failed_response = ${it.message}"
                                    )
                                )
//                                videosManager.onResetCurrentVideo(
//                                    currentVideo = currentVideo ?: return@doOnError,
//                                    reason = reason
//                                )
                            }
                        }
                        .subscribe({
                        }, {
                        })
                }
        }
    }

    private var minChunkSize = (0.5 * 1024).toInt()
    private var maxChunkSize = 1024 * 2 * 1024
    private var baseChunkSize = 1024
    private var uploadChunkIndex = 0
    private var fullBytes = byteArrayOf()
    var remainder: Int = 0
    var count = 0
    private fun checkFileStatusBeforeUpload(video: SavedVideo) {
        Log.d(TAG, "CHECKING FILE..... ${video?.videoId}.")
        when (video?.videoId?.isEmpty()) {
            true -> reset(video, VideosManagerImpl.RESET.RESET_NO_VIDEO_ID, "checkFileStatusBeforeUpload")
            else -> {
                if (isDeviceConnected()) {
                    chunkToUpload = baseChunkSize
                    if (video.mediumUploaded && uploadingHD) {
                        video.uploadState = UploadState.UPLOADING_HIGH
                        fullBytes = File(video.encodedPath).readBytes()
                        upload()
                    } else {
                        if (fileCheck(video)) {
                            fullBytes = File(video.mediumRes).readBytes()
                            video.uploadState = UploadState.UPLOADING_MEDIUM
                            upload()
                        } else println("BAD FILE BEFORE UPLOAD")
                    }
                }
            }
        }

    }

    private var startRange = 0
    private var chunkToUpload: Int = 0
    @SuppressLint("CheckResult")
    var up: Disposable? = null
    var part_size: Int? = 0
    var prog: Int? = 0
    private fun upload() {
        if (isDeviceConnected()) {
            val fullFileSize = fullBytes.size

            val previousStartPlusDynamicChunk = startRange + chunkToUpload
            val end = minOf(fullFileSize, previousStartPlusDynamicChunk)
            val chunkFromFile = fullBytes.sliceArray(
                IntRange(
                    startRange,
                    end - 1
                )
            )
            println("----------------- chunk to upload $uploadChunkIndex")
            println("----------------- slice to upload ${chunkFromFile.size}")
            println("----------------- full file $fullFileSize")

            part_size = chunkFromFile.size
            val progress = (end * 100L / fullFileSize).toInt()
            prog = progress
            progressManager.onUpdateProgress(
                currentVideo?.clientId ?: "",
                progress,
                uploadingHD
            )

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
                        analyticsManager.onTrackUploadEvent(
                            Uploaded_Part,
                            arrayOf(
                                "client_id = ${currentVideo?.clientId}",
                                "upload_quality = ${qualityCheck()}",
                                "part_index = $uploadChunkIndex",
                                "part_offset = $previousStartPlusDynamicChunk",
                                "chunkSize = $chunkToUpload",
                                "part_size = $part_size",
                                "total_size = $fullFileSize",
                                "progress = $prog"
                            )
                        )
                        if (it.body()?.success == true) {
                            val sent = it.raw().sentRequestAtMillis()
                            val received = it.raw().receivedResponseAtMillis()
                            chunkToUpload = when (checkResponseTimeDifference(timeSent = sent, timeReceived = received)) {
                                in 0..1 -> maxChunkSize
                                in 2..3 -> baseChunkSize
                                else -> minChunkSize
                            }

                            if (startRange >= end) {
                                checkForComplete()
                                startRange = 0
                                chunkToUpload = 0
                                uploadChunkIndex = 0
                                prog = 0
                                part_size = 0
                            } else {
                                startRange = end
                                uploadChunkIndex++
                                upload()
                            }
                        }
                    }
                    .doOnError {
                        if (it.message.equals("HTTP 502 Bad Gateway")) {
                            upload()
                        } else {
                            startRange = 0
                            uploadChunkIndex = 0
                            prog = 0
                            part_size = 0
                            analyticsManager.onTrackUploadEvent(
                                Failed_Uploaded_Part,
                                arrayOf(
                                    "client_id = ${currentVideo?.clientId}",
                                    "upload_quality = ${qualityCheck()}",
                                    "part_index = $uploadChunkIndex",
                                    "part_offset = $previousStartPlusDynamicChunk",
                                    "chunkSize = $chunkToUpload",
                                    "part_size = $part_size",
                                    "total_size = $fullFileSize",
                                    "failed_response = ${it.message}"
                                )
                            )
                            uploadingIsFalse()
//                            videosManager.onResetCurrentVideo(
//                                currentVideo = currentVideo ?: return@doOnError,
//                                reason = reason
//                            )
                        }

                    }
                    .subscribe({
                    }, {
                        it.printStackTrace()
                    })
            }
        }
    }

    private fun qualityCheck(): String {
        return when (uploadingHD) {
            false -> "medium"
            else -> {
                when (currentVideo?.trimmedVidPath.isNullOrEmpty()) {
                    true -> "high"
                    else -> "trimmed"
                }
            }
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
            if (isDeviceConnected()) {
                println("STOP FOR NEW UPLOAD================================= $stopUploadForNewFavorite")
                complete = uploadsManager
                    .onCompleteUpload(encryptionResponse?.upload?.id ?: "")
                    .doOnError {
                        it.printStackTrace()
                        println("ERROR FROM COMPLETE! ${it.message}")
                        if (it.message?.contains("502 Bad Gateway") == true) {
                            checkForComplete()
                        } else {
                            reset(currentVideo ?: return@doOnError, VideosManagerImpl.RESET.RESET_UNKOWN, "Failed onComplete: ${it.message}")
                        }
                    }
                    .subscribe({
                        if (it.code() == 502) {
                            checkForComplete()
                        }
                        val status = it.body()?.status?.let { it1 -> CompleteResponse.valueOf(it1) }
                        println("THIS IS THE COMPLETE STATUS:::::: $status")
                        when (status) {
                            CompleteResponse.COMPLETING -> {
                                println("COMPLETING RESPONSE...... ${it.body()?.status}")
                                pingServerForStatus()
                            }
                            CompleteResponse.COMPLETED -> {
                                println("DONE RESPONSE...... ${it.body()?.status}")
                                finalizeUpload(it.body()?.upload)
                            }
                            CompleteResponse.FAILED -> {
                                println("FAILED RESPONSE........ ${it.body()?.status}")
                                reset(currentVideo ?: return@subscribe, VideosManagerImpl.RESET.RESET_UNKOWN, "Failed Md5 Matching ${fullBytes.size}")
                            }
                            else -> {
                                println("This is an else from complete body........ ${it.body()?.status}")
                                reset(currentVideo ?: return@subscribe, VideosManagerImpl.RESET.RESET_UNKOWN, "Failed Md5 Matching ${fullBytes.size}")
                            }
                        }
                    }, {
                        it.printStackTrace()
                    })
            }
        }
    }


    private var serverDis: Disposable? = null
    @Synchronized
    private fun finalizeUpload(upload: Upload?) {
        val path = when (uploadingHD) {
            true -> currentVideo?.encodedPath
            else -> currentVideo?.mediumRes
        }
        if (isDeviceConnected()) {
            synchronized(this) {
                try {
                    getVideoDimensions(path = path)
                } catch (arg: IllegalAccessException) {
                    arg.printStackTrace()
                    uploadingIsFalse()
                    //videosManager.onResetCurrentVideo(currentVideo ?: return, reason)
                }

                println("Write to server final.... ${currentVideo?.videoId}")
                serverDis = uploadsManager
                    .writerToServerAfterComplete(
                        uploadId = currentVideo?.videoId ?: return@synchronized,
                        S3Key = upload?.S3Key ?: return@synchronized,
                        vidWidth = width,
                        vidHeight = height,
                        hq = uploadingHD,
                        vid = currentVideo ?: return
                    )
                    .doOnError {
                        if (it.message.equals("HTTP 502 Bad Gateway")) {
                            finalizeUpload(upload)
                        } else {
                            uploadingIsFalse()
//                            videosManager.onResetCurrentVideo(
//                                currentVideo ?: return@doOnError,
//                                reason
//                            )
                            it.printStackTrace()
                        }
                    }
                    .doAfterNext {
                        println("Server response>>>>>>>>>>>>>>")
                        analyticsManager.onTrackUploadEvent(
                            Completed,
                            arrayOf(
                                "client_id = ${currentVideo?.clientId}",
                                "s3.key = ${upload?.S3Key}",
                                "upload.quality = ${qualityCheck()}",
                                "upload.upload_rate = ",
                                "upload.time = "
                            )
                        )
                        progressManager.onUpdateProgress(
                            currentVideo?.clientId ?: "",
                            100,
                            uploadingHD
                        )

                        println("Finalized video..... ${currentVideo?.uploadState}")
                        when (uploadingHD) {
                            true -> {
                                currentVideo?.uploadState = UploadState.UPLOADED_HIGH
                                videosManager.updateHighuploaded(
                                    true,
                                    currentVideo ?: return@doAfterNext
                                )
                            }
                            else -> {
                                currentVideo?.uploadState = UploadState.UPLOADED_MEDIUM
                                videosManager.updateMediumUploaded(true, currentVideo?.clientId ?: "")
                            }
                        }
                        uploadingIsFalse()
                        videosManager.onNotifyWorkIsDone(currentVideo ?: return@doAfterNext)
                    }
                    .subscribe({
                    },
                        {
                            it.printStackTrace()
                        })
            }
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
    private fun getVideoDimensions(path: String?) {
        val retriever = MediaMetadataRetriever()
        var width = 0
        var height = 0
        try {
            retriever.setDataSource(path)
            width =
                Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH))
            height =
                Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT))
            retriever.release()
        } catch (nf: NumberFormatException) {
            uploadingIsFalse()
            // videosManager.onResetCurrentVideo(currentVideo ?: return, reason)
            retriever.release()
        } catch (ia: IllegalArgumentException) {
            uploadingIsFalse()
            //  videosManager.onResetCurrentVideo(currentVideo ?: return, reason)
            retriever.release()
        } catch (r: RuntimeException) {
            r.printStackTrace()
            uploadingIsFalse()
            // videosManager.onResetCurrentVideo(currentVideo ?: return, reason)
            retriever.release()
        }
        this.width = width
        this.height = height
    }

    private fun uploadingIsTrue() {
        uploading = true
        println("UPLOADING BOOLEAN =========================================== $uploading")
    }

    private fun uploadingIsFalse() {
        uploading = false
        println("UPLOADING BOOLEAN =========================================== $uploading")
    }

    companion object {
        const val Register = "Registered Video"
        const val Failed_Registration = "Failed to register video"
        const val Upload_Token = "Received upload token"
        const val Failed_Token = "Failed to get upload token"
        const val Register_Upload = "Registered upload"
        const val Failed_Register_Upload = "Failed to register upload"
        const val Uploaded_Part = "Uploaded part"
        const val Failed_Uploaded_Part = "Failed to upload part"
        const val Completed = "Completed video"
        const val Failed_Completed = "Failed to complete video"
        const val Set_Path = "Set video path"
        const val Failed_Path = "Failed to set video path"
    }
}

class DaggerWorkerFactory(
    private val uploads: UploadsManager,
    private val videos: VideosManager,
    private val progress: ProgressManager,
    private val notifications: NotificationManager,
    private val analytics: OTAnalyticsManager,
    private val wifi: WifiManager,
    private val file: FileManager
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
                instance.analyticsManager = analytics
                instance.wifiManager = wifi
                instance.fileManager = file
            }
        }
        return instance
    }
}

