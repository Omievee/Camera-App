package com.itsovertime.overtimecamera.play.videomanager

import android.annotation.SuppressLint
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import androidx.work.*
import com.crashlytics.android.Crashlytics
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException
import com.itsovertime.overtimecamera.play.application.OTApplication
import com.itsovertime.overtimecamera.play.db.AppDatabase
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.UploadState
import com.itsovertime.overtimecamera.play.uploadsmanager.UploadsManager
import com.itsovertime.overtimecamera.play.workmanager.VideoUploadWorker
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import net.ypresto.androidtranscoder.MediaTranscoder
import net.ypresto.androidtranscoder.format.MediaFormatStrategyPresets
import java.io.File
import java.io.IOException


class VideosManagerImpl(
    val context: OTApplication,
    val manager: UploadsManager
) : VideosManager {


    override fun onGetVideosForUploadScreen(): Single<List<SavedVideo>> {
        return db!!.videoDao()
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
    override fun updateTaggedAthleteField(
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
    override fun updateHighuploaded(qualityUploaded: Boolean, clientId: String) {
        Single.fromCallable {
            with(videoDao) {
                this?.updateHighUpload(qualityUploaded, clientId, UploadState.UPLOADED_HIGH)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                loadFromDB()
            }, {
                it.printStackTrace()
            })
    }

    @SuppressLint("CheckResult")
    override fun updateMediumUploaded(qualityUploaded: Boolean, clientId: String) {
        Single.fromCallable {
            with(videoDao) {
                this?.updateMediumUpload(qualityUploaded, clientId, UploadState.UPLOADED_MEDIUM)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                loadFromDB()
            }, {
                it.printStackTrace()
            })
    }

    var db = AppDatabase.getAppDataBase(context = context)
    private var videoDao = db?.videoDao()
    @SuppressLint("CheckResult")
    override fun updateUploadId(uplaodId: String, savedVideo: SavedVideo) {
        Single.fromCallable {
            with(videoDao) {
                this?.updateUploadId(uplaodId, savedVideo.clientId)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (isFirstRun) {
                    doWork()
                    isFirstRun = false
                }
                loadFromDB()
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
            }
            )
    }

    @SuppressLint("CheckResult")
    override fun updateVideoInstanceId(videoId: String, clientId: String) {
        Single.fromCallable {
            with(videoDao) {
                this?.updateVideoInstanceId(videoId, clientId)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorReturn {
                it.printStackTrace()
            }
            .subscribe({}, {
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
    override fun loadFFMPEG() {
        println("loading FFMPEG")
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


    override fun determineTrim(savedVideo: SavedVideo) {

    }

    private var seekToEndOf = "-sseof"
    private var videoCodec = "-vcodec"
    private var codecValue = "h264"
    private var commandYOverwrite = "-y"
    private var readInput = "-i"
    private var commandCCopy = "-c"
    private var copyVideo = "copy"


    override fun encodeHighQualityTrim(savedVideo: SavedVideo) {
        val newFile =
            fileForHDEncodedVideo(File(savedVideo.trimmedVidPath).name, savedVideo.clientId)
        val encodeCommand = arrayOf(
            // I command reads from designated input file
            readInput,
            // file input
            File(savedVideo.trimmedVidPath).absolutePath,
            // video codec to write to
            videoCodec,
            // value of codec - H264
            codecValue,
            "-preset",
            "ultrafast",
            "-crf",
            "28",
            "-maxrate",
            "16000k",
            "-bufsize",
            "16000k",
            // new file
            newFile.absolutePath
        )
        try {
            synchronized(this) {
                ffmpeg.execute(encodeCommand, object : ExecuteBinaryResponseHandler() {
                    override fun onSuccess(message: String?) {
                        super.onSuccess(message)

                    }

                    override fun onProgress(message: String?) {
                        super.onProgress(message)
                    }

                    override fun onFinish() {
                        super.onFinish()

                    }

                    override fun onFailure(message: String?) {
                        super.onFailure(message)

                        Crashlytics.log("Failed to execute ffmpeg -- $message")
                    }
                })
            }
        } catch (e: FFmpegCommandAlreadyRunningException) {
            println("FFMPEG :: ${e.message}")
            Crashlytics.log("FFMPEG -- ${e.message}")
        }

    }


    @Synchronized
    private fun trimVideo(savedVideo: SavedVideo) {
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
            codecValue,
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
                        println("SUCCESSFUL TRIM :$message")
                    }

                    override fun onProgress(message: String?) {
                        super.onProgress(message)
                        println("Trim progress -- $message")
                    }

                    override fun onFinish() {
                        super.onFinish()
                        transcodeVideo(savedVideo, newFile)
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
                loadFromDB()
                it.printStackTrace()
            }
            .subscribe({
            }, {
                it.printStackTrace()
            })
    }

    private fun fileForHDEncodedVideo(fileName: String, clientId: String): File {
        val mediaStorageDir =
            File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "UploadHD")
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            println("Failed....")
        }
        updateEncodedPath(
            File(mediaStorageDir.path + File.separator + "1080.trim.$fileName").absolutePath,
            clientId
        )
        return File(mediaStorageDir.path + File.separator + "1080.trim.$fileName")
    }

    @SuppressLint("CheckResult")
    private fun updateEncodedPath(path: String, clientId: String) {
        Single.fromCallable {
            with(videoDao) {
                this?.updateEncodedPath(path, clientId)
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
    override fun updateVideoFunny(isFunny: Boolean, clientId: String) {
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

    @SuppressLint("CheckResult")
    override fun updateVideoFavorite(isFavorite: Boolean, clientId: String) {
        Single.fromCallable {
            with(videoDao) {
                this?.setVideoAsFavorite(is_favorite = isFavorite, lastID = clientId)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorReturn {
                it.printStackTrace()
            }
            .doOnSuccess {
            }
            .subscribe({
                loadFromDB()
            }, {
                it.printStackTrace()
            })
    }

    private val subject: BehaviorSubject<List<SavedVideo>> = BehaviorSubject.create()
    private val total: BehaviorSubject<Int> = BehaviorSubject.create()
    @Synchronized
    override fun transcodeVideo(savedVideo: SavedVideo, videoFile: File) {
        val file = Uri.fromFile(videoFile)
        val parcelFileDescriptor = context.contentResolver.openAssetFileDescriptor(file, "rw")
        val fileDescriptor = parcelFileDescriptor?.fileDescriptor
//
//        Transcoder.into(compressedFile(videoFile, savedVideo).absolutePath)
//            .addDataSource(videoFile.path)
//
//            .setListener(object : TranscoderListener {
//                override fun onTranscodeCompleted(successCode: Int) {
//                    loadFromDB()
//                }
//
//                override fun onTranscodeProgress(progress: Double) {
//                    println("prog :::: $progress")
//                }
//
//                override fun onTranscodeCanceled() {
//
//                }
//
//                override fun onTranscodeFailed(exception: Throwable) {
//
//                }
//
//            }).transcode()


        val listener = object : MediaTranscoder.Listener {
            override fun onTranscodeProgress(progress: Double) {}
            override fun onTranscodeCanceled() {}
            override fun onTranscodeFailed(exception: Exception?) {
                exception?.printStackTrace()
                resetUploadStateForCurrentVideo(savedVideo)
            }

            override fun onTranscodeCompleted() {
                loadFromDB()
            }
        }
        try {
            MediaTranscoder.getInstance().transcodeVideo(
                fileDescriptor, compressedFile(videoFile, savedVideo).absolutePath,
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

    var lastVideoId: String = ""
    @SuppressLint("CheckResult")
    @Synchronized
    override fun saveHighQualityVideoToDB(video: SavedVideo) {
        this.lastVideoMaxTime = video.max_video_length.toString()
        lastVideoId = video.clientId
        Single.fromCallable {
            with(videoDao) {
                this?.saveVideoData(video)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorReturn {
                it.printStackTrace()
            }
            .subscribe({
                trimVideo(video)
            }, {
                it.printStackTrace()
            })
    }

    private var lastVideoMaxTime: String? = ""
    var videosList = mutableListOf<SavedVideo>()
    @Synchronized
    @SuppressLint("CheckResult")
    override fun loadFromDB() {
        videosList.clear()
        Single.fromCallable {
            db?.videoDao()?.getVideos()
        }.map {
            videosList.addAll(it.asReversed())
            val totalUploaded = mutableListOf<SavedVideo>()
            totalUploaded.addAll(it)
            totalUploaded.removeIf {
                it.mediumUploaded
            }
            total.onNext(totalUploaded.size)
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                println("----------------------------------------LOADED!!!!")
                val uploadVid = videosList.find {
                    it.id.isNullOrEmpty()
                }
                if (uploadVid != null) {
                  //  registerVideo(uploadVid)
                }
            }, {
                it.printStackTrace()
            })
    }

    var disp: Disposable? = null
    override fun registerVideo(saved: SavedVideo) {
        var uploadId = ""
        disp?.dispose()
        disp = manager
            .getVideoInstance(saved)
            .doOnError {
                it.printStackTrace()
            }
            .map {
                uploadId = it.video.id ?: return@map
            }
            .subscribe({
                updateUploadId(uploadId, saved)
            },
                {
                    it.printStackTrace()
                })
    }

    override fun onNotifyWorkIsDone() {
        val vid = videosList.find { !it.mediumUploaded }
        if (vid != null) {
            doWork()
        }
    }

    private var isFirstRun: Boolean = true
    private var workRequest: OneTimeWorkRequest? = null
    var work: Operation? = null
    private fun doWork() {
        workRequest = OneTimeWorkRequestBuilder<VideoUploadWorker>().addTag("UploadWork").build()
        work = WorkManager.getInstance(context)
            .enqueueUniqueWork("UploadWork", ExistingWorkPolicy.KEEP, workRequest ?: return)
        println("Work run........ ${work!!.state}")
    }

    @SuppressLint("CheckResult")
    override fun resetUploadStateForCurrentVideo(currentVideo: SavedVideo) {
        if (!currentVideo.mediumRes.isNullOrEmpty()) {
            val video = File(currentVideo.mediumRes)
            if (video.exists()) {
                video.delete()
            }
        }
        if (!currentVideo.trimmedVidPath.isNullOrEmpty()) {
            val trim = File(currentVideo.trimmedVidPath)
            if (trim.exists()) {
                trim.delete()
            }
        }
        Single.fromCallable {
            with(videoDao) {
                this?.resetUploadDataForVideo(
                    uploadState = UploadState.QUEUED,
                    uploadId = "",
                    id = "",
                    lastID = currentVideo.clientId,
                    trimmedVidPath = "",
                    mediumVidPath = ""
                )
            }
            with(videoDao) {
                this?.getVideoForUpload(currentVideo.clientId)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                println("successful reset... starting transcode")
                it?.let { it1 -> determineTrim(it1) }
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

}
