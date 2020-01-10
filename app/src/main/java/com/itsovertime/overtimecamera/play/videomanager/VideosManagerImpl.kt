package com.itsovertime.overtimecamera.play.videomanager

import android.annotation.SuppressLint
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException
import com.google.firebase.analytics.FirebaseAnalytics
import com.itsovertime.overtimecamera.play.analytics.OTAnalyticsManager
import com.itsovertime.overtimecamera.play.application.OTApplication
import com.itsovertime.overtimecamera.play.db.AppDatabase
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.UploadState
import com.itsovertime.overtimecamera.play.uploadsmanager.UploadsManager
import com.itsovertime.overtimecamera.play.wifimanager.NETWORK_TYPE
import com.otaliastudios.transcoder.Transcoder
import com.otaliastudios.transcoder.TranscoderListener
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_camera.*
import net.ypresto.androidtranscoder.MediaTranscoder
import net.ypresto.androidtranscoder.format.MediaFormatStrategyPresets
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


class VideosManagerImpl(
    val context: OTApplication,
    val manager: UploadsManager,
    val wifi: com.itsovertime.overtimecamera.play.wifimanager.WifiManager,
    val analytics: OTAnalyticsManager
) : VideosManager {

    var data = AppDatabase.getAppDataBase(context)
    override fun onGetVideosForUploadScreen(): Single<List<SavedVideo>> {
        return data!!.videoDao()
            .getVideosForUpload()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    }

    override fun onGetVideosForUpload(): Single<List<SavedVideo>> {
        return db!!.videoDao()
            .getVideosForUpload()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    @SuppressLint("CheckResult")
    override fun onUpdatedTaggedAthletesInDb(
        taggedAthletesArray: ArrayList<String>,
        clientId: String
    ) {
        Single.fromCallable {
            with(videoDao) {
                this?.updateTaggedAthletesField(taggedAthletesArray, clientId)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({

            }, {
                it.printStackTrace()
            })
    }


    @SuppressLint("CheckResult")
    override fun updateHighuploaded(qualityUploaded: Boolean, video: SavedVideo) {
        Single.fromCallable {
            with(videoDao) {
                this?.updateHighUpload(qualityUploaded, video.clientId, UploadState.UPLOADED_HIGH)
            }
            with(videoDao) {
                this?.getVideoForUpload(video.clientId)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                println("This was the uploaded video... $it")
                deleteSuccessfullyUploadedVideo(video)
            }, {
                it.printStackTrace()
            })
    }

    override fun onNotifyWorkIsDone(savedVideo: SavedVideo) {
        if (pendingMediumUploads.size > 0 || mainList.size > 0) {
            newVideos.onNext(true)
        }
        completedSub.onNext(savedVideo)
    }

    private fun deleteSuccessfullyUploadedVideo(video: SavedVideo) {
        if (File(video.encodedPath).exists()) {
            File(video.encodedPath).delete()
        }
    }

    @SuppressLint("CheckResult")
    override fun updateMediumUploaded(qualityUploaded: Boolean, clientId: String) {
        Single.fromCallable {
            with(videoDao) {
                this?.updateMediumUpload(qualityUploaded, clientId, UploadState.UPLOADED_MEDIUM)
            }
            with(videoDao) {
                this?.getVideos()
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                updateUploadsCounter(it, false)
            }, {
                it.printStackTrace()
            })
    }

    var db = AppDatabase.getAppDataBase(context = context)
    private var videoDao = db?.videoDao()
    @SuppressLint("CheckResult")
    override fun onUpdateUploadIdInDb(uplaodId: String, savedVideo: SavedVideo) {
        Single.fromCallable {
            with(videoDao) {
                this?.updateUploadId(uplaodId, savedVideo.clientId)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                newVideos.onNext(true)
            }, {
                it.printStackTrace()
            })
    }


    @SuppressLint("CheckResult")
    override fun updateVideoStatus(video: SavedVideo, state: UploadState) {
        Single.fromCallable {
            with(videoDao) {
                this?.updateVideoState(state, video.clientId)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorReturn {
                it.printStackTrace()
            }
            .subscribe({
            }, {
                it.printStackTrace()
            })
    }

    @SuppressLint("CheckResult")
    override fun updateVideoMd5(md5: String, clientId: String) {
        Single.fromCallable {
            with(videoDao) {
                this?.updateVideoMd5(md5, clientId)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorReturn {
                it.printStackTrace()
            }
            .subscribe({
            }, {
                it.printStackTrace()
            })
    }

    var ffmpeg: FFmpeg = FFmpeg.getInstance(context)
    override fun onLoadFFMPEG() {
        try {
            ffmpeg.loadBinary(object : LoadBinaryResponseHandler() {
                override fun onFailure() {
                    super.onFailure()
                    Crashlytics.log("FFMPEG -- LOAD FAILURE")
                }
            })


        } catch (e: FFmpegNotSupportedException) {
            e.printStackTrace()
            Crashlytics.log("FFMPEG not supported -- ${e.message}")
        }
    }

    private var seekToEndOf = "-sseof"
    private var videoCodec = "-vcodec"
    private var codecValue = "h264"
    private var commandYOverwrite = "-y"
    private var readInput = "-i"
    private var commandCCopy = "-c"
    private var copyVideo = "copy"

    private var encodedVid: BehaviorSubject<SavedVideo> = BehaviorSubject.create()
    override fun subscribeToEncodeComplete(): Observable<SavedVideo> {
        return encodedVid
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    private var newVideos: BehaviorSubject<Boolean> = BehaviorSubject.create()
    override fun subscribeToNewVideos(): Observable<Boolean> {
        return newVideos
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    var completedSub: BehaviorSubject<SavedVideo> = BehaviorSubject.create()
    override fun subscribeToCompletedUploads(): Observable<SavedVideo> {
        return completedSub
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }


    var hdSubject: BehaviorSubject<Boolean> = BehaviorSubject.create()
    override fun onNotifyHDUploadsTriggered(hd: Boolean) {
        hdSubject.onNext(hd)
    }

    override fun subscribeToHDSwitch(): Observable<Boolean> {
        return hdSubject
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    var start: Long = 0L
    var finish: Long = 0L
    @Synchronized
    private fun onTrimVideo(savedVideo: SavedVideo) {
        println("STARTING TRIM!! $savedVideo")
        val newFile = fileForTrimmedVideo(File(savedVideo.highRes).name, savedVideo.clientId)
        val maxVideoLengthFromEvent = "-${savedVideo.max_video_length}"
        val complexCommand = arrayOf(
            //seek to end of video
            seekToEndOf,
            //given amount of time from Event -
            maxVideoLengthFromEvent,
            // Y command overwrites files w/ out permission
            commandYOverwrite,
            // I command reads from designated input file
            readInput,
            // file input
            File(savedVideo.highRes).absolutePath,
            // video codec to write to
            videoCodec,
            // value of codec - H264
            "h264",
            // C command dictates what to do w/ file
            commandCCopy,
            // copy the file to given location
            copyVideo,
            // new file that was copied from old
            newFile.absolutePath
        )
        try {
            synchronized(this) {
                ffmpeg.execute(complexCommand, object : ExecuteBinaryResponseHandler() {
                    override fun onSuccess(message: String?) {
                        super.onSuccess(message)
                        onTransCodeVideo(savedVideo, newFile)
                        Log.d(TAG, "successful trim...")
                    }

                    override fun onProgress(message: String?) {
                        super.onProgress(message)
                        println("Trim progress -- $message")
                    }

                    override fun onStart() {
                        super.onStart()
                        start = System.currentTimeMillis()
                    }

                    override fun onFinish() {
                        super.onFinish()

                        finish = System.currentTimeMillis()
                        analytics.onTrackTrim(
                            arrayOf(
                                "video_client_id = ${savedVideo.clientId}",
                                "duration = ${(TimeUnit.MILLISECONDS.toSeconds(finish - start))}",
                                "trimmed_to = ${savedVideo.max_video_length}",
                                "trimmed_from = ${TimeUnit.MILLISECONDS.toSeconds(
                                    assetTimeLength(savedVideo)
                                )}"
                            )
                        )

                    }

                    override fun onFailure(message: String?) {
                        super.onFailure(message)
                        println("TRIM FAILURE $message")
                        Crashlytics.log("Failed to execute ffmpeg -- $message")
                    }
                })
            }
        } catch (e: FFmpegCommandAlreadyRunningException) {
            println("FFMPEG :: ${e.message}")
            Crashlytics.log("FFMPEG -- ${e.message}")
        }


    }

    @SuppressLint("CheckResult")
    private fun updateMediumFilePath(absolutePath: String, clientId: String) {
        Single.fromCallable {
            with(videoDao) {
                this?.updateMediumQualityPath(absolutePath, clientId)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorReturn {

                it.printStackTrace()
            }
            .subscribe({
            }, {
                it.printStackTrace()
            })
    }


    @SuppressLint("CheckResult")
    override fun onUpdateEncodedPath(path: String, clientId: String) {
        Single.fromCallable {
            with(videoDao) {
                this?.updateEncodedPath(path, clientId)
            }
            with(videoDao) {
                this?.getVideoForUpload(clientId)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Log.d(TAG, "COMPLETE FROM ENCODING DB")
                encodedVid.onNext(it ?: return@subscribe)
            }, {
                it.printStackTrace()
            })
    }

    override fun onGetEncodedVideo(clientId: String): Single<SavedVideo> {
        return db!!
            .videoDao().getEncodedVideo(clientId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    }


    private fun fileForTrimmedVideo(fileName: String, clientId: String): File {
        val mediaStorageDir =
            File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Trimmed")
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            println("Failed....")
        }
        updateTrimmedVidPathInDB(
            File(mediaStorageDir.path + File.separator + "trim.$fileName").absolutePath,
            clientId
        )

        return File(mediaStorageDir.path + File.separator + "trim.$fileName")
    }

    @SuppressLint("CheckResult")
    private fun updateTrimmedVidPathInDB(path: String, clientId: String) {
        Single.fromCallable {
            with(videoDao) {
                this?.updateTrimVideoPath(path, clientId)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorReturn {
                it.printStackTrace()
            }
            .subscribe({

            }, {
                it.printStackTrace()
            })
    }

    private fun compressedFile(file: File, video: SavedVideo): File {
        val mediaStorageDir =
            File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "UploadMedium")
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
        }

        val filePath = mediaStorageDir.path + File.separator + "720.${file.name}"
        updateMediumFilePath(
            File(filePath).absolutePath,
            video.clientId
        )
        return File(filePath)
    }


    @SuppressLint("CheckResult")
    override fun onVideoIsFunny(isFunny: Boolean, clientId: String) {
        pendingVidRegistration?.is_funny = true
        Single.fromCallable {
            with(videoDao) {
                this?.setVideoAsFunny(is_funny = isFunny, lastID = clientId)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorReturn {
                it.printStackTrace()
            }
            .subscribe({
            }, {
                it.printStackTrace()
            })
    }


    private val subject: BehaviorSubject<List<SavedVideo>> = BehaviorSubject.create()
    private val total: BehaviorSubject<Int> = BehaviorSubject.create()
    var listener: MediaTranscoder.Listener? = null
    override fun onTransCodeVideo(savedVideo: SavedVideo, videoFile: File) {
        println("STARTING TRANSCODE =========================================")
        val file = Uri.fromFile(videoFile)
        val parcelFileDescriptor = context.contentResolver.openAssetFileDescriptor(file, "rw")
        val fileDescriptor = parcelFileDescriptor?.fileDescriptor
        synchronized(this) {
            try {
                listener = object : MediaTranscoder.Listener {
                    override fun onTranscodeProgress(progress: Double) {
                        println("PROGRESS $progress")
                    }

                    override fun onTranscodeCanceled() {}
                    override fun onTranscodeFailed(exception: Exception?) {
                        Log.d(TAG, "transcode failed.. ${exception?.message}...")
                        listener = null
                        exception?.printStackTrace()
                        onResetCurrentVideo(savedVideo)
                    }

                    override fun onTranscodeCompleted() {
                        println("TRANSCODE COMPLETE ========================================= ${savedVideo.uploadId}")
                        listener = null
                        when (savedVideo.uploadId.isNullOrEmpty()) {
                            true -> {
                                println("registering video.....................")
                                onRegisterVideoWithServer(savedVideo)
                            }
                            else -> {
                                println("on next video.....................")
                                synchronized(this) {
                                    val alertWorker = object : TimerTask() {
                                        override fun run() {
                                            newVideos.onNext(true)
                                        }
                                    }
                                    Timer().schedule(alertWorker, 1500)
                                }
                            }
                        }
                    }
                }
                MediaTranscoder.getInstance().transcodeVideo(
                    fileDescriptor,
                    compressedFile(videoFile, savedVideo).absolutePath,
                    MediaFormatStrategyPresets.createAndroid720pStrategy(), listener
                )
            } catch (r: RuntimeException) {
                Crashlytics.log("MediaTranscoder-Error ${r.message}")
                r.printStackTrace()
            } catch (io: IOException) {
                Crashlytics.log("MediaTranscoder-Error ${io.message}")

                io.printStackTrace()
            } catch (ia: IllegalArgumentException) {
                Crashlytics.log("MediaTranscoder-Error ${ia.message}")
                ia.printStackTrace()
            }
        }
    }

    fun assetTimeLength(video: SavedVideo): Long {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.fromFile(File(video.highRes)))
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val timeInMillisec = time.toLong() / 1000
            retriever.release()
            return timeInMillisec
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
        return 0L
    }

    private fun isVideoDurationLongerThanMaxTime(file: SavedVideo): Boolean {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.fromFile(File(file.highRes)))
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val timeInMillisec = time.toLong() / 1000
            retriever.release()
            return timeInMillisec > file.max_video_length
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
        return false
    }

    var TAG = "VIDEO PROCESS"
    @SuppressLint("CheckResult")
    @Synchronized
    override fun onSaveVideoToDb(video: SavedVideo) {
        pendingVidRegistration = video
        this.lastVideoMaxTime = video.max_video_length.toString()
        Single.fromCallable {
            with(videoDao) {
                this?.saveVideoData(video)
            }
            with(videoDao) {
                this?.getVideos()
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError {
                it.printStackTrace()
            }
            .subscribe({
                updateUploadsCounter(it, false)
                Log.d(TAG, "saved video complete... $pendingVidRegistration")
                if (isVideoDurationLongerThanMaxTime(
                        pendingVidRegistration ?: return@subscribe
                    ) && pendingVidRegistration?.is_selfie == false
                ) {
                    onTrimVideo(pendingVidRegistration ?: return@subscribe)
                } else {
                    onTransCodeVideo(
                        pendingVidRegistration ?: return@subscribe,
                        File(pendingVidRegistration?.highRes)
                    )
                }
            }, {
                it.printStackTrace()
            })
    }

    var pendingMediumUploads = mutableListOf<SavedVideo>()
    var mainList = mutableListOf<SavedVideo>()
    private fun updateUploadsCounter(it: List<SavedVideo>?, firstLoad: Boolean) {
        mainList.clear()
        it?.let { it1 -> mainList.addAll(it1) }
        mainList.removeIf {
            it.mediumUploaded && it.highUploaded
        }
        pendingMediumUploads.clear()
        it?.let { it1 -> pendingMediumUploads.addAll(it1) }

        pendingMediumUploads.removeIf {
            it.mediumUploaded
        }
        if (firstLoad && pendingMediumUploads.size > 0) {
            analytics.debugMessage("Checking First Run", "this is a first run....")
            newVideos.onNext(true)
        }
        if (firstLoad) {
            checkConnection()
        }

        total.onNext(pendingMediumUploads.size)
    }

    var pendingVidRegistration: SavedVideo? = null
    @SuppressLint("CheckResult")
    override fun onVideoIsFavorite(isFavorite: Boolean, video: SavedVideo) {
        pendingVidRegistration?.is_favorite = true
        Single.fromCallable {
            with(videoDao) {
                this?.setVideoAsFavorite(is_favorite = isFavorite, lastID = video.clientId)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorReturn {
                it.printStackTrace()
            }
            .subscribe({
                println("Success from favoring video...")
            }, {
                it.printStackTrace()
            })
    }

    private var lastVideoMaxTime: String? = ""

    @Synchronized
    @SuppressLint("CheckResult")
    override fun onLoadDb() {
        Single.fromCallable {
            db?.videoDao()?.getVideos()
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                updateUploadsCounter(it, true)
            }, {
                it.printStackTrace()
            })
    }

    var wifiDisp: Disposable? = null
    var disconnected: Boolean = false
    private fun checkConnection() {
        wifiDisp?.dispose()
        wifiDisp = wifi
            .subscribeToNetworkUpdates()
            .subscribe({
                disconnected = when (it) {
                    NETWORK_TYPE.UNKNOWN -> true
                    else -> false
                }
            }, {
                it.printStackTrace()
            })
    }

    var disp: Disposable? = null
    override fun onRegisterVideoWithServer(saved: SavedVideo) {
        var uploadId = ""
        disp?.dispose()
        disp = manager
            .getVideoInstance(saved)
            .retry(3)
            .doOnError {
                it.printStackTrace()
                analytics.onTrackUploadEvent(
                    "Failed to register Video",
                    arrayOf("client_id = ${saved.clientId}", "failed_response = ${it.message}")
                )
                if (it.message.equals("HTTP 502 Bad Gateway")) {
                    onRegisterVideoWithServer(saved)
                }
            }
            .map {
                uploadId = it.video.id ?: return@map
                Log.d(TAG, "upload id received...")
            }
            .subscribe({
                analytics.onTrackUploadEvent(
                    "Registered Video",
                    arrayOf("client_id = ${saved.clientId}", "upload_id = ${uploadId}")
                )

                onUpdateUploadIdInDb(uploadId, saved)
            }, {
                it.printStackTrace()
            })
    }


    private var isFirstRun: Boolean = true
    @SuppressLint("CheckResult")
    override fun onResetCurrentVideo(currentVideo: SavedVideo) {
        Log.d(TAG, "Reset happened......")
        var trimPath = ""
        var medPath = ""
        var encodePath = ""
        val uploadId: String
        var state: UploadState = UploadState.QUEUED
        when (currentVideo.mediumUploaded) {
            true -> {
                state = UploadState.UPLOADED_MEDIUM
                medPath = currentVideo.mediumRes.toString()
                uploadId = currentVideo.uploadId.toString()
                trimPath = if (!currentVideo.trimmedVidPath.isNullOrEmpty()) {
                    currentVideo.trimmedVidPath.toString()
                } else {
                    ""
                }
                if (!currentVideo.encodedPath.isNullOrEmpty()) {
                    encodePath = ""
                    val encodeFile = File(currentVideo.encodedPath)
                    if (encodeFile.exists()) {
                        encodeFile.delete()
                    }
                }
            }
            else -> {
                uploadId = if (currentVideo.uploadId.isNullOrEmpty()) {
                    ""
                } else currentVideo.uploadId.toString()

                if (!currentVideo.mediumRes.isNullOrEmpty()) {
                    medPath = ""
                    val video = File(currentVideo.mediumRes)
                    if (video.exists()) {
                        video.delete()
                    }
                }
                if (!currentVideo.trimmedVidPath.isNullOrEmpty()) {
                    trimPath = ""
                    val trimFile = File(currentVideo.trimmedVidPath)
                    if (trimFile.exists()) {
                        trimFile.delete()
                    }
                }
            }
        }

        Single.fromCallable {
            with(videoDao) {
                this?.resetUploadDataForVideo(
                    uploadState = state,
                    uploadId = uploadId,
                    lastID = currentVideo.clientId,
                    trimmedVidPath = trimPath,
                    mediumVidPath = medPath,
                    encodedPath = encodePath
                )
            }
            with(videoDao) {
                this?.getVideoForUpload(currentVideo.clientId)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                println("Video that was reset... $it")
                if (it?.mediumUploaded == false) {
                    if (isVideoDurationLongerThanMaxTime(it)) {
                        onTrimVideo(it)
                    } else onTransCodeVideo(it, File(it.highRes))
                } else {
                    println("Else .. new video alert...")
                    newVideos.onNext(true)
                }
                if (it?.uploadId.isNullOrEmpty() && it?.mediumUploaded == true) {
                    onRegisterVideoWithServer(it)
                }

            }, {
                it.printStackTrace()
            })
    }


    override fun subscribeToVideoGallery(): Observable<List<SavedVideo>> {
        return subject
            .subscribeOn(Schedulers.single())
            .observeOn(AndroidSchedulers.mainThread())
    }

    override fun subscribeToVideoGallerySize(): Observable<Int> {
        return total
            .subscribeOn(Schedulers.single())
            .observeOn(AndroidSchedulers.mainThread())
    }


    var newFave: BehaviorSubject<SavedVideo> = BehaviorSubject.create()
    override fun subscribeToNewFavoriteVideoEvent(): Observable<SavedVideo> {
        return newFave
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }
}
