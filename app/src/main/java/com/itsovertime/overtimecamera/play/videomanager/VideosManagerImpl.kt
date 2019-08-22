package com.itsovertime.overtimecamera.play.videomanager

import android.annotation.SuppressLint
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
import com.itsovertime.overtimecamera.play.uploads.UploadState
import com.itsovertime.overtimecamera.play.uploadsmanager.UploadsManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import net.ypresto.androidtranscoder.MediaTranscoder
import net.ypresto.androidtranscoder.format.MediaFormatStrategyPresets
import java.io.File
import java.io.IOException
import java.util.*


class VideosManagerImpl(val context: OTApplication, val manager: UploadsManager) : VideosManager {

    @SuppressLint("CheckResult")
    override fun updateUploadId(uploadId: String, clientId: String) {
        Observable.fromCallable {
            val videoDao = db?.videoDao()
            with(videoDao) {
                this?.updateUploadId(uploadId, clientId)
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
        Observable.fromCallable {
            val videoDao = db?.videoDao()
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
                    println("Success from ffmpeg")
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

    private fun executeFFMPEG(savedVideo: SavedVideo) {
        println("execute started...")
        val newFile = fileForTrimmedVideo(File(savedVideo.vidPath).name)
        val maxVideoLengthFromEvent = "-$lastVideoMaxTime"
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
            File(savedVideo.vidPath).absolutePath,
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
                    transcodeVideo(newFile)
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
        Observable.fromCallable {
            val videoDao = db?.videoDao()
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
        val mediaStorageDir = File(context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "OverTimeTrimmed")
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            println("Failed....")
        }
        updateTrimmedVidPathInDB(File(mediaStorageDir.path + File.separator + "trim.$fileName").absolutePath)
        return File(mediaStorageDir.path + File.separator + "trim.$fileName")
    }

    @SuppressLint("CheckResult")
    private fun updateTrimmedVidPathInDB(path: String) {
        Observable.fromCallable {
            val videoDao = db?.videoDao()
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
        val mediaStorageDir = File(context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "OverTime720")
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Crashlytics.log("Compress File Error")
        }
        updateMediumFilePath(File(mediaStorageDir.path + File.separator + file.name).absolutePath)
        return File(mediaStorageDir.path + File.separator + file.name)
    }

    var db = AppDatabase.getAppDataBase(context = context)
    @SuppressLint("CheckResult")
    override fun updateVideoFunny(isFunny: Boolean) {
        Observable.fromCallable {
            val videoDao = db?.videoDao()
            with(videoDao) {
                this?.setVideoAsFunny(is_funny = isFunny, lastID = lastVideoId)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally {
                loadFromDB()
            }
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
        Observable.fromCallable {
            val videoDao = db?.videoDao()
            with(videoDao) {
                this?.setVideoAsFavorite(is_favorite = isFavorite, lastID = lastVideoId)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorReturn {
                it.printStackTrace()
            }
            .subscribe {
                loadFromDB()
            }
    }

    private val subject: BehaviorSubject<List<SavedVideo>> = BehaviorSubject.create()
    private val total: BehaviorSubject<Int> = BehaviorSubject.create()

    override fun transcodeVideo(videoFile: File) {
        val file = Uri.fromFile(videoFile)
        val parcelFileDescriptor = context.contentResolver.openAssetFileDescriptor(file, "rw")
        val fileDescriptor = parcelFileDescriptor?.fileDescriptor

        val listener = object : MediaTranscoder.Listener {
            override fun onTranscodeProgress(progress: Double) {
            }

            override fun onTranscodeCanceled() {}
            override fun onTranscodeFailed(exception: Exception?) {
                Toast.makeText(context, "Failed to transcode:: $exception", Toast.LENGTH_SHORT).show()
            }


            override fun onTranscodeCompleted() {
                println("complete :::")
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
    override fun saveHighQualityVideoToDB(event: Event, filePath: String, isFavorite: Boolean) {
        this.lastVideoMaxTime = event?.max_video_length.toString()
        println("Max time::: $lastVideoMaxTime")
        Observable.fromCallable {
            val clientId = UUID.randomUUID().toString()
            val video = SavedVideo(
                clientId = clientId,
                vidPath = filePath,
                is_favorite = isFavorite,
                eventId = event.id,
                eventName = event.name,
                starts_at = event.starts_at,
                address = event.address,
                latitude = event.latitude,
                longitude = event.longitude,
                uploadState = UploadState.QUEUED
            )
            val videoDao = db?.videoDao()
            with(videoDao) {
                this?.saveVideoData(video)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally {
                loadFromDB()
            }
            .onErrorReturn {
                it.printStackTrace()
            }
            .subscribe({
            }, {
                it.printStackTrace()
            })
    }

    var listOfVideos = mutableListOf<SavedVideo>()
    var lastVideoMaxTime: String? = ""
    private var isFirstRun: Boolean = true

    @SuppressLint("CheckResult")
    override fun loadFromDB() {
        listOfVideos.clear()
        Observable.fromCallable {
            db?.videoDao()?.getVideos()
        }.map {
            it.forEach {
                listOfVideos.add(0, it)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally {
                println("is first run $isFirstRun")
                if (isFirstRun) {
                    loadFFMPEG()
                    isFirstRun = false
                }
                if (!listOfVideos.isNullOrEmpty()) {
                    lastVideoId = listOfVideos[0].clientId
                    lastVideoMaxTime = listOfVideos[0].max_video_length.toString()
                    executeFFMPEG(listOfVideos[0])
                    manager.onReadyVideosForUpload(listOfVideos)
                }
            }
            .subscribe({
                subject.onNext(listOfVideos)
                total.onNext(listOfVideos.size)
            },
                {
                    it.printStackTrace()
                }
            )
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
