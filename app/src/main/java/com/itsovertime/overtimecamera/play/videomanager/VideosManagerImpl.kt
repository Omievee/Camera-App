package com.itsovertime.overtimecamera.play.videomanager

import android.annotation.SuppressLint
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
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
import com.otaliastudios.transcoder.Transcoder
import com.otaliastudios.transcoder.TranscoderListener
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.io.File


class VideosManagerImpl(
    val context: OTApplication,
    val manager: UploadsManager
) : VideosManager {

    @SuppressLint("CheckResult")
    override fun updateHighuploaded(qualityUploaded: Boolean, clientId: String) {
        println("high uploaded?? $qualityUploaded && $clientId")
        Single.fromCallable {
            with(videoDao) {
                this?.updateHighUpload(qualityUploaded, clientId)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                println("SUCCESS!")
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


    private fun isVideoDurationLongerThanMaxTime(file: SavedVideo): Boolean {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.fromFile(File(file.highRes)))
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) ?: ""
            val timeInMillisec = time.toLong() / 1000
            retriever.release()
            println("Time in file... $timeInMillisec")
            return timeInMillisec > file.max_video_length
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
        return false
    }

    @Synchronized
    override fun determineTrim(savedVideo: SavedVideo) {
        if (isVideoDurationLongerThanMaxTime(savedVideo)) {
            synchronized(this) {
                println("True .. is longer")
                trimVideo(savedVideo)
            }
        } else transcodeVideo(savedVideo, File(savedVideo.highRes))
    }

    @Synchronized
    private fun trimVideo(savedVideo: SavedVideo) {
        val newFile = fileForTrimmedVideo(File(savedVideo.highRes).name, savedVideo.clientId)
        val maxVideoLengthFromEvent = "${savedVideo.max_video_length}"
        val command = arrayOf(
            "-ss",
            "0",
            "-i",
            File(savedVideo.highRes).absolutePath,
            "-t",
            maxVideoLengthFromEvent,
            "-c",
            "copy",
            newFile.absolutePath
        )

        try {
            synchronized(this) {
                ffmpeg.execute(command, object : ExecuteBinaryResponseHandler() {
                    override fun onFinish() {
                        super.onFinish()
                        loadFromDB()
                    }

                    override fun onProgress(message: String?) {
                        super.onProgress(message)
                        println("Trim progress -- $message")
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

    @Synchronized
    private fun compressedFile(file: File, video: SavedVideo): File {
        synchronized(this) {
            val mediaStorageDir =
                File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "OverTime720")
            if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            }

            val filePath = mediaStorageDir.path + File.separator + "720.${file.name}"

            updateMediumFilePath(
                File(filePath).absolutePath,
                video.clientId
            )
            return File(filePath)
        }
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
        synchronized(this) {
            Transcoder.into(compressedFile(videoFile, savedVideo).absolutePath)
                .addDataSource(context, Uri.fromFile(videoFile))
                .setListener(object : TranscoderListener {
                    override fun onTranscodeCompleted(successCode: Int) {
                        loadFromDB()
                    }

                    override fun onTranscodeProgress(progress: Double) {
                        println("Progress transcoding ........ OV ........ $progress")
                    }

                    override fun onTranscodeCanceled() {
                    }

                    override fun onTranscodeFailed(exception: Throwable) {
                        resetUploadStateForCurrentVideo(savedVideo)
                        println("Transcode failure... ${exception.message}")
                        println("Transcode failure... ${exception.cause}")
                        println("Transcode failure... ${exception.localizedMessage}")
                    }
                }).transcode()
        }

    }

    var lastVideoId: String = ""
    @SuppressLint("CheckResult")
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
                determineTrim(video)
            }, {
                it.printStackTrace()
            })
    }

    private var lastVideoMaxTime: String? = ""
    var videosList = mutableListOf<SavedVideo>()
    @Synchronized
    @SuppressLint("CheckResult")
    override fun loadFromDB() {
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
                    for (savedVideo in videosList) {
                        println("Med Res: ${savedVideo.mediumRes}")
                        if (savedVideo.mediumRes.isNullOrEmpty() || File(savedVideo.mediumRes).readBytes().isEmpty()) {
                            if (savedVideo.trimmedVidPath.isNullOrEmpty()) {
                                transcodeVideo(savedVideo, File(savedVideo.highRes))
                            } else transcodeVideo(savedVideo, File(savedVideo.trimmedVidPath))
                        }
                    }
                    if (isFirstRun) {
                        doWork()
                        isFirstRun = false
                    }
                }

            }, {
                it.printStackTrace()
            })
    }

    var isFirstRun: Boolean = true

    private fun doWork() {
        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<VideoUploadWorker>()
                .build()
        )
    }

    @SuppressLint("CheckResult")
    override fun resetUploadStateForCurrentVideo(currentVideo: SavedVideo) {
        val video = File(currentVideo.mediumRes ?: "")
        val trim = File(currentVideo.trimmedVidPath ?: "")
        if (video.exists()) {
            video.delete()
        }
        if (trim.exists()) {
            trim.delete()
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
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                println("RESET STATE>>>> LOAD DB")
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
