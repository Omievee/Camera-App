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
import com.itsovertime.overtimecamera.play.network.EncryptedResponse
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
import java.util.*


class VideosManagerImpl(val context: OTApplication, val manager: UploadsManager) : VideosManager {


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
                loadFromDB()
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
            .doFinally {

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
                println("FAILED TO TRANSCODE ::::: ${exception?.message} && ${exception?.cause}")
//                Toast.makeText(context, "Failed to transcode:: $exception", Toast.LENGTH_SHORT)
//                    .show()
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
                this?.updateVideoIsProcessed(isProcessed = true, lastID = savedVideo?.clientId)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally {
                loadFromDB()
            }
            .subscribe({}, {
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
                eventId = event?.id ?: "",
                eventName = event?.name,
                starts_at = event?.starts_at,
                address = event?.address,
                latitude = event?.latitude,
                longitude = event?.longitude,
                uploadState = UploadState.QUEUED,
                max_video_length = event?.max_video_length ?: 12
            )
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

    private var listOfVideos = mutableListOf<SavedVideo>()
    private var lastVideoMaxTime: String? = ""
    private var isFirstRun: Boolean = true

    @SuppressLint("CheckResult")
    override fun loadFromDB() {
        listOfVideos.clear()
        preppedVideos.clear()
        Single.fromCallable {
            db?.videoDao()?.getVideos()
        }.map {
            it.forEach {
                listOfVideos.add(0, it)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally {
                if (isFirstRun) {
                    loadFFMPEG()
                    isFirstRun = false
                }
                if (!listOfVideos.isNullOrEmpty()) {
                    lastVideoId = listOfVideos[0].clientId
                    listOfVideos.forEach {
                        if (!it.isProcessed) {
                            determineTrim(it)
                        } else {
                            when (it.is_favorite) {
                                true -> preppedVideos.add(0, it)
                                else -> preppedVideos.add(it)
                            }
                            prepVideosForUploading(preppedVideos)
                        }
                    }
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

    private var preppedVideos = mutableListOf<SavedVideo>()


    private fun prepVideosForUploading(preppedVideos: MutableList<SavedVideo>) {
        println("Videos Ready for que..... ${preppedVideos.size}")
        if (!preppedVideos.isNullOrEmpty()) {
            preppedVideos.forEach {
                if (it.uploadState != UploadState.COMPLETE) {
                    manager.onProcessUploadQue(preppedVideos)
                }
            }
        }
    }


    @SuppressLint("CheckResult")
    override fun resetUploadStateForCurrentVideo(currentVideo: SavedVideo) {
        Single.fromCallable {
            with(videoDao) {
                this?.resetUploadDataForVideo(
                    isProcessed = false,
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
