package com.itsovertime.overtimecamera.play.videomanager

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import com.crashlytics.android.Crashlytics
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException
import com.itsovertime.overtimecamera.play.db.AppDatabase
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.uploadsmanager.UploadsManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import net.ypresto.androidtranscoder.MediaTranscoder
import net.ypresto.androidtranscoder.format.MediaFormatStrategyPresets
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit


class VideosManagerImpl(val manager: UploadsManager) : VideosManager {

    override fun trimVideo(file: File) {

    }

    lateinit var ffmpeg: FFmpeg
    private fun loadFFMPEG(context: Context) {
        ffmpeg = FFmpeg.getInstance(context)
        try {
            ffmpeg.loadBinary(object : LoadBinaryResponseHandler() {
                override fun onSuccess() {
                    super.onSuccess()
                }

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

    private fun executeFFMPEG(file: File) {
        val complexCommand = arrayOf(
            "-sseof",
            "-18",
            "-y",
            "-i",
            file.absolutePath,
            "-vcodec",
            "h264",
            "-c",
            "copy",
            fileForTrimmedVideo(file.name).absolutePath
        )
        try {

            ffmpeg.execute(complexCommand, object : ExecuteBinaryResponseHandler() {
                override fun onFinish() {
                    super.onFinish()
                    context?.let { transcodeVideo(it, file) }
                }

                override fun onFailure(message: String?) {
                    super.onFailure(message)
                    println("failed to execute:::::: $message")
                    Crashlytics.log("Failed to execute ffmpeg -- $message")
                }
            })


        } catch (e: FFmpegCommandAlreadyRunningException) {
            e.printStackTrace()
            Crashlytics.log("FFMPEG -- ${e.message}")
        }
    }

    private fun fileForTrimmedVideo(fileName: String): File {
        val mediaStorageDir = File(context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "OverTimeTrimmed")
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            println("Failed....")
        }
        return File(mediaStorageDir.path + File.separator + "$fileName.trim.mp4")
    }

    private fun compressedFile(file: File): File {
        val mediaStorageDir = File(context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "OverTime720")
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Crashlytics.log("Compress File Error")
        }
        return File(mediaStorageDir.path + File.separator + file.name)
    }

    @SuppressLint("CheckResult")
    override fun updateVideoFunny(isFunny: Boolean) {
        val db = context?.let { AppDatabase.getAppDataBase(context = it) }
        Observable.fromCallable {
            val videoDao = db?.videoDao()
            with(videoDao) {
                this?.setVideoAsFunny(isFunny = isFunny, lastID = lastVideoId)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally {
                context?.let { loadFromDB(it) }
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
        val db = context?.let { AppDatabase.getAppDataBase(context = it) }
        Observable.fromCallable {
            val videoDao = db?.videoDao()
            with(videoDao) {
                this?.setVideoAsFavorite(isFave = isFavorite, lastID = lastVideoId)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorReturn {
                it.printStackTrace()
            }
            .subscribe {
                context?.let {
                    loadFromDB(it)
                }
            }
    }

    override fun uploadVideo() {

    }

    var context: Context? = null
    private val subject: BehaviorSubject<List<SavedVideo>> = BehaviorSubject.create()

    override fun transcodeVideo(context: Context, videoFile: File) {
        this.context = context
        val file = Uri.fromFile(videoFile)

        val parcelFileDescriptor = context.contentResolver.openAssetFileDescriptor(file, "rw")
        val fileDescriptor = parcelFileDescriptor?.fileDescriptor

        val listener = object : MediaTranscoder.Listener {
            override fun onTranscodeProgress(progress: Double) {
            }

            override fun onTranscodeCanceled() {}
            override fun onTranscodeFailed(exception: Exception?) {
                transcodeVideo(context, videoFile)
                exception?.printStackTrace()
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


    private var lastVideoId: Int = 0
    @SuppressLint("CheckResult")
    override fun saveVideoToDB(context: Context, filePath: String, isFavorite: Boolean) {
        this.context = context
        val db = AppDatabase.getAppDataBase(context = context)
        Observable.fromCallable {
            val video = SavedVideo(vidPath = filePath, isFavorite = isFavorite)
            val videoDao = db?.videoDao()
            with(videoDao) {
                this?.saveVideo(video)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally {
                loadFromDB(context)
            }
            .onErrorReturn {
                it.printStackTrace()
            }
            .subscribe({
            }, {
                it.printStackTrace()
            })
    }


    var isFirstRun: Boolean = false
    @SuppressLint("CheckResult")
    override fun loadFromDB(context: Context) {
        isFirstRun = true
        this.context = context
        val listOfVideos = mutableListOf<SavedVideo>()
        val db = AppDatabase.getAppDataBase(context = context)
        Observable.fromCallable {
            db?.videoDao()?.getVideos()
        }.map {
            it.forEach {
                listOfVideos.add(0, it)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally {
                when (isFirstRun) {
                    true -> {
                        loadFFMPEG(context)
                        isFirstRun = false
                    }
                }
                when (listOfVideos.isNullOrEmpty()) {
                    true -> {
                    }
                    else -> {
                        lastVideoId = listOfVideos[0].id
                        executeFFMPEG(File(listOfVideos[0].vidPath))
//                        if (determineVideoLength(recentFile = File(listOfVideos[0].vidPath)) > 18) {
//                            trimVideo(File(listOfVideos[0].vidPath))
//                        } else {
//                            transcodeVideo(context = context, videoFile = File(listOfVideos[0].vidPath))
//                        }
                    }
                }
            }
            .subscribe({
                subject.onNext(listOfVideos)
            },
                {
                    it.printStackTrace()
                }
            )
    }

    private fun determineVideoLength(recentFile: File): Long {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, Uri.fromFile(recentFile))
        val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        retriever.release()

        return TimeUnit.MILLISECONDS.toSeconds(time.toLong()) % 60
    }

    override fun subscribeToVideoGallery(): Observable<List<SavedVideo>> {
        return subject
            .subscribeOn(Schedulers.single())
            .observeOn(AndroidSchedulers.mainThread())
    }
}

