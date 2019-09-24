package com.itsovertime.overtimecamera.play.videomanager

import android.annotation.SuppressLint
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.widget.Toast
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


class VideosManagerImpl(
    val context: OTApplication,
    val manager: UploadsManager,
    private val queManager: QueManager
) : VideosManager {
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
        println("STATE IS : $state")
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
                if (state == UploadState.UPLOADING_MEDIUM || state == UploadState.REGISTERED) {
                    updateMediumUploaded(true, video?.clientId)
                } else if (state == UploadState.UPLOADED_HIGH || state == UploadState.COMPLETE) updateHighuploaded(
                    true,
                    video.clientId
                )

            }, {
                it.printStackTrace()
            })
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


    private fun isVideoDurationLongerThanMaxTime(file: SavedVideo): Boolean {
        val retriever = MediaMetadataRetriever();
        try {
            println("URI : ${Uri.fromFile(File(file.highRes))}")
            retriever.setDataSource(context, Uri.fromFile(File(file.highRes)))
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) ?: ""
            val timeInMillisec = time.toLong() / 1000
            retriever.release()
            return timeInMillisec > file.max_video_length
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
        return false
    }

    override fun determineTrim(savedVideo: SavedVideo) {
        println("vido... $savedVideo")
        if (isVideoDurationLongerThanMaxTime(savedVideo)) {
            trimVideo(savedVideo)
        } else {
            transcodeVideo(savedVideo, File(savedVideo.highRes))
        }
    }

    private fun trimVideo(savedVideo: SavedVideo) {
        val newFile = fileForTrimmedVideo(File(savedVideo.highRes).name)
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

    private fun fileForTrimmedVideo(fileName: String): File {
        val mediaStorageDir =
            File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "OverTimeTrimmed")
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            println("Failed....")
        }
        updateTrimmedVidPathInDB(File(mediaStorageDir.path + File.separator + "trim.$fileName").absolutePath)
        return File(mediaStorageDir.path + File.separator + "trim.$fileName")
    }

    @SuppressLint("CheckResult")
    private fun updateTrimmedVidPathInDB(path: String) {
        Single.fromCallable {
            with(videoDao) {
                this?.updateTrimVideoPath(path, lastVideoId)
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

    private fun compressedFile(file: File): File {
        val mediaStorageDir =
            File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "OverTime720")
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Crashlytics.log("Compress File Error")
        }
        path = File(mediaStorageDir.path + File.separator + file.name).absolutePath
        return File(mediaStorageDir.path + File.separator + file.name)
    }


    @SuppressLint("CheckResult")
    override fun updateVideoFunny(isFunny: Boolean) {
        println("VIDEO UPDATE FUNNY :::::: $isFunny && $lastVideoId")
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
    var path: String = ""
    override fun transcodeVideo(savedVideo: SavedVideo, videoFile: File) {
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
                println("successful .. $savedVideo")
                saveHighQualityVideoToDB(savedVideo)
            }
        }
        try {
            MediaTranscoder.getInstance().transcodeVideo(
                fileDescriptor, compressedFile(videoFile).absolutePath,
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


    private var lastVideoId: String = ""
    @SuppressLint("CheckResult")
    override fun saveHighQualityVideoToDB(video: SavedVideo) {
        println("saving ....... video ? $video")
        this.lastVideoMaxTime = video.max_video_length.toString()
        video.mediumRes = path
        video.isProcessed = true
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

    @Synchronized
    @SuppressLint("CheckResult")
    override fun loadFromDB() {
        var videosList: List<SavedVideo> = emptyList()
        Single.fromCallable {
            db?.videoDao()?.getVideos()
        }.map {
            println("list from DB === ${it.size}")
            if (it != videosList) {
                videosList = it
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                subject.onNext(videosList)
                total.onNext(videosList.size)

                if (!videosList.isNullOrEmpty()) {
                    println("Inside IF :${videosList.size}")
                    queManager.onUpdateQueList(videosList)
                } else println("Error prepping videos for que...")
            },
                {
                    it.printStackTrace()
                }
            )
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
