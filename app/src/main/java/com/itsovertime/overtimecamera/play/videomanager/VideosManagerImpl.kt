package com.itsovertime.overtimecamera.play.videomanager

import android.annotation.SuppressLint
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.crashlytics.android.Crashlytics
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException
import com.itsovertime.overtimecamera.play.application.OTApplication
import com.itsovertime.overtimecamera.play.db.AppDatabase
import com.itsovertime.overtimecamera.play.model.Event
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.UploadState
import com.itsovertime.overtimecamera.play.quemanager.QueManager
import com.itsovertime.overtimecamera.play.uploadsmanager.UploadsManager
import com.itsovertime.overtimecamera.play.uploadsmanager.VideoUploadWorker
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import net.ypresto.androidtranscoder.MediaTranscoder
import net.ypresto.androidtranscoder.format.MediaFormatStrategyPresets
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit


class VideosManagerImpl(
    val context: OTApplication,
    val manager: UploadsManager,
    private val queManager: QueManager
) : VideosManager {
    var queSub: BehaviorSubject<Boolean> = BehaviorSubject.create()
    override fun subToDbUpdates(): Observable<Boolean> {
        return queSub
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    @SuppressLint("CheckResult")
    override fun updateHighuploaded(qualityUploaded: Boolean, clientId: String) {
        Single.fromCallable {
            with(videoDao) {
                this?.updateHighUpload(qualityUploaded, clientId)
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
                this?.updateMediumUpload(qualityUploaded, clientId)
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
    override fun updateUploadId(uplaodId: String, clientId: String) {
        Single.fromCallable {
            with(videoDao) {
                this?.updateUploadId(uplaodId, clientId)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
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
                override fun onSuccess() {
                    super.onSuccess()
                }

                override fun onFailure() {
                    super.onFailure()
                    loadFFMPEG()
                    println("Failed to Load....")
                    Crashlytics.log("FFMPEG -- LOAD FAILURE")
                }
            })
        } catch (e: FFmpegNotSupportedException) {
            e.printStackTrace()
            Crashlytics.log("FFMPEG not supported -- ${e.message}")
        }
    }

    var seekToEndOf = "-sseof"
    var videoCodec = "-vcodec"
    var codecValue = "h264"
    var commandYOverwrite = "-y"
    var readInput = "-i"
    var commandCCopy = "-c"
    var copyVideo = "copy"


    private fun trimVideo(savedVideo: SavedVideo) {
        println("VIDEO BEFORE TRIM!! $savedVideo")
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
            ffmpeg.execute(complexCommand, object : ExecuteBinaryResponseHandler() {
                override fun onSuccess(message: String?) {
                    super.onSuccess(message)
                    println("Success from trim....")
                    transcodeVideo(savedVideo, newFile)
                }

                override fun onFailure(message: String?) {
                    super.onFailure(message)
                    println("TRIM FAILURE $message")
                    Crashlytics.log("Failed to execute ffmpeg -- $message")
                }
            })

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
                println("MEDIUM SAVED:: ")
                loadFromDB()
            }, {
                it.printStackTrace()
            })

    }

    private fun fileForTrimmedVideo(fileName: String, clientId: String): File {
        val mediaStorageDir =
            File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "OverTimeTrimmed")
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
            File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "OverTime720")
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Crashlytics.log("Compress File Error")
        }
        println("saving medium... ")
        updateMediumFilePath(
            File(mediaStorageDir.path + File.separator + file.name).absolutePath,
            video.clientId
        )
        return File(mediaStorageDir.path + File.separator + file.name)
    }


    @SuppressLint("CheckResult")
    override fun updateVideoFunny(isFunny: Boolean) {
        Single.fromCallable {
            with(videoDao) {
                this?.setVideoAsFunny(is_funny = isFunny, lastID = lastVideoId)
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
    override fun updateVideoFavorite(isFavorite: Boolean) {
        Single.fromCallable {
            with(videoDao) {
                this?.setVideoAsFavorite(is_favorite = isFavorite, lastID = lastVideoId)
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
    override fun transcodeVideo(savedVideo: SavedVideo, videoFile: File) {
        println("VIDEO AFTER TRIM!! : $savedVideo")
        val file = Uri.fromFile(videoFile)
        val parcelFileDescriptor = context.contentResolver.openAssetFileDescriptor(file, "rw")
        val fileDescriptor = parcelFileDescriptor?.fileDescriptor


        val listener = object : MediaTranscoder.Listener {
            override fun onTranscodeProgress(progress: Double) {
            }

            override fun onTranscodeCanceled() {}
            override fun onTranscodeFailed(exception: Exception?) {
                exception?.printStackTrace()
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

    var processedVideos = mutableListOf<SavedVideo>()
    var lastVideoId: String = ""
    @SuppressLint("CheckResult")
    override fun saveHighQualityVideoToDB(video: SavedVideo) {
        println("saving ....... video ")
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
            .doFinally {
                loadFromDB()
            }
            .subscribe({
            }, {
                it.printStackTrace()
            })
    }


    private var lastVideoMaxTime: String? = ""
    var videosList = mutableListOf<SavedVideo>()
    @Synchronized
    @SuppressLint("CheckResult")
    override fun loadFromDB() {
        println("Loading DB!")
        videosList = mutableListOf()
        Single.fromCallable {
            db?.videoDao()?.getVideos()
        }.map {
            subject.onNext(it.asReversed())
            total.onNext(it.size)
            videosList.addAll(it.asReversed())
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (!videosList.isNullOrEmpty()) {
                    queSub.onNext(true)
                    videosList.forEach {
                        if (it.mediumRes.isNullOrEmpty()) {
                            trimVideo(it)
                        } else processedVideos.add(it)
                    }
                }
                println("Subscribe hit...")

                if (!processedVideos.isNullOrEmpty()) {
                    println("Running worker...")
                    WorkManager.getInstance(context).enqueue(
                        OneTimeWorkRequestBuilder<VideoUploadWorker>()
                            .build()
                    )
                }

            }, {
                it.printStackTrace()
            })
    }

    @SuppressLint("CheckResult")
    override fun resetUploadStateForCurrentVideo(currentVideo: SavedVideo) {
        Single.fromCallable {
            with(videoDao) {
                this?.resetUploadDataForVideo(
                    uploadState = UploadState.QUEUED,
                    uploadId = "",
                    id = "",
                    lastID = currentVideo.clientId
                )
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                loadFromDB()
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
