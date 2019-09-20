package com.itsovertime.overtimecamera.play.videomanager

import android.annotation.SuppressLint
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
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


class VideosManagerImpl(val context: OTApplication, val manager: UploadsManager) : VideosManager {
    @SuppressLint("CheckResult")
    override fun updateHighuploaded(qualityUploaded: Boolean, clientId: String) {
        Single.fromCallable {
            with(videoDao) {
                this?.updateHighUpload(qualityUploaded, clientId)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
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
                if (state == UploadState.UPLOADED_MEDIUM || state == UploadState.REGISTERED) {
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
    private fun loadFFMPEG() {
        try {
            ffmpeg.loadBinary(object : LoadBinaryResponseHandler() {
                override fun onSuccess() {
                    super.onSuccess()
                }

                override fun onFailure() {
                    super.onFailure()
                    loadFFMPEG()
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
            retriever.setDataSource(context, Uri.fromFile(File(file.highRes)));
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) ?: ""
            val timeInMillisec = time.toLong() / 1000
            retriever.release()
            return timeInMillisec > file.max_video_length

        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
        return false
    }

    private fun determineTrim(savedVideo: SavedVideo) {
        if (isVideoDurationLongerThanMaxTime(savedVideo)) {
            trimVideo(savedVideo)
        } else transcodeVideo(savedVideo, File(savedVideo.highRes))
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
                    transcodeVideo(savedVideo, newFile)
                }

                override fun onFailure(message: String?) {
                    super.onFailure(message)
                    Crashlytics.log("Failed to execute ffmpeg -- $message")
                }
            })


        } catch (e: FFmpegCommandAlreadyRunningException) {
            println("FFMPEG :: ${e.message}")
            Crashlytics.log("FFMPEG -- ${e.message}")
        }
    }

    @SuppressLint("CheckResult")
    private fun updateMediumFilePath(absolutePath: String) {
        Single.fromCallable {
            with(videoDao) {
                this?.updateMediumQualityPath(absolutePath, lastVideoId)
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
            File(context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "OverTime720")
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Crashlytics.log("Compress File Error")
        }
        updateMediumFilePath(File(mediaStorageDir.path + File.separator + file.name).absolutePath)
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
            .subscribe({
                loadFromDB()
            }, {
                it.printStackTrace()
            })
    }

    private val subject: BehaviorSubject<List<SavedVideo>> = BehaviorSubject.create()
    private val total: BehaviorSubject<Int> = BehaviorSubject.create()

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
                println("Failed Transcode:::: ${exception?.message}")
                resetUploadStateForCurrentVideo(savedVideo)
            }

            override fun onTranscodeCompleted() {
                updateVideoIsProcessed(savedVideo)
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

    @SuppressLint("CheckResult")
    private fun updateVideoIsProcessed(savedVideo: SavedVideo) {
        Single.fromCallable {
            with(videoDao) {
                this?.updateVideoIsProcessed(isProcessed = true, lastID = savedVideo.clientId)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                loadFromDB()
            }, {
                it.printStackTrace()
            })
    }


    private var lastVideoId: String = ""
    @SuppressLint("CheckResult")
    override fun saveHighQualityVideoToDB(event: Event?, filePath: String, isFavorite: Boolean) {
        this.lastVideoMaxTime = event?.max_video_length.toString()
        Single.fromCallable {
            val clientId = UUID.randomUUID().toString()
            val video = SavedVideo(
                clientId = clientId,
                highRes = filePath,
                is_favorite = isFavorite,
                event_id = event?.id ?: "",
                eventName = event?.name,
                starts_at = event?.starts_at,
                address = event?.address,
                latitude = event?.latitude,
                city = event?.city,
                duration_in_hours = event?.duration_in_hours ?: 0,
                longitude = event?.longitude,
                uploadState = UploadState.QUEUED,
                max_video_length = event?.max_video_length ?: 12,
                created_at = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            )
            with(videoDao) {
                this?.saveVideoData(video)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorReturn {
                it.printStackTrace()
            }
            .doFinally { loadFromDB() }
            .subscribe({
            }, {
                it.printStackTrace()
            })
    }

    private var videosList: List<SavedVideo>? = null
    private var lastVideoMaxTime: String? = ""
    private var isFirstRun: Boolean = true

    @Synchronized
    @SuppressLint("CheckResult")
    override fun loadFromDB() {
        preppedVideos.clear()
        Single.fromCallable {
            db?.videoDao()?.getVideos()
        }.map {
            if (it != videosList) {
                videosList = it.asReversed()
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                subject.onNext(videosList ?: emptyList())
                total.onNext(videosList?.size ?: 0)
                if (isFirstRun) {
                    loadFFMPEG()
                    isFirstRun = false
                }
                determineProcess(videosList ?: emptyList())
            },
                {
                    it.printStackTrace()
                }
            )
    }

    @Synchronized
    private fun determineProcess(list: List<SavedVideo>) {
        if (!list.isNullOrEmpty()) {
            lastVideoId = list[0].clientId
            list.sortedBy {
                it.is_favorite
            }
            list.forEach {
                if (!it.isProcessed) {
                    determineTrim(it)
                } else prepVideosForUploading(list.toMutableList())
            }
        }
    }

    private var preppedVideos = mutableListOf<SavedVideo>()
    @Synchronized
    private fun prepVideosForUploading(preppedVideos: MutableList<SavedVideo>) {
        if (!preppedVideos.isNullOrEmpty()) {
            preppedVideos.removeIf {
                it.uploadState == UploadState.COMPLETE
            }
            println("Prepping  videos ${preppedVideos.size}")
            manager.onProcessUploadQue(preppedVideos)
        } else println("Error prepping videos for que...")
    }

    @SuppressLint("CheckResult")
    override fun resetUploadStateForCurrentVideo(currentVideo: SavedVideo) {
        Single.fromCallable {
            with(videoDao) {
                this?.resetUploadDataForVideo(
                    uploadState = UploadState.QUEUED,
                    uploadId = "",
                    id = "",
                    mediumRes = "",
                    trimmedVidPath = "",
                    isProcessed = false,
                    lastID = currentVideo.clientId
                )
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                println("RESET DONE!")
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
