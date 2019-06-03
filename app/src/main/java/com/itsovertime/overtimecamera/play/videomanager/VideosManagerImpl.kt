package com.itsovertime.overtimecamera.play.videomanager

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.ParcelFileDescriptor
import com.itsovertime.overtimecamera.play.model.SavedVideo
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import com.itsovertime.overtimecamera.play.db.AppDatabase
import com.itsovertime.overtimecamera.play.network.UploadResponse
import io.reactivex.Single
import java.io.File
import java.util.*
import android.widget.Toast
import android.R.attr.data
import android.util.Log
import net.ypresto.androidtranscoder.MediaTranscoder
import net.ypresto.androidtranscoder.format.MediaFormatStrategyPresets
import java.io.FileNotFoundException
import java.util.concurrent.Future
import android.R.attr.data
import android.content.ContentResolver
import android.net.Uri
import java.lang.Exception
import android.R.attr.data
import android.media.CamcorderProfile
import android.media.CameraProfile
import android.os.Environment
import androidx.lifecycle.LiveData
import androidx.work.*
import com.crashlytics.android.Crashlytics
import com.google.common.util.concurrent.ListenableFuture
import com.itsovertime.overtimecamera.play.network.Api
import io.reactivex.disposables.Disposable
import java.io.IOException
import java.lang.RuntimeException


class VideosManagerImpl(val api: Api) : VideosManager {
    override fun updateVideoFunny(isFunny: Boolean) {

    }

    var uploadResponse: UploadResponse? = null

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

    override fun uploadVideo(){
        //TODO
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
                println("progress ::: $progress")
            }

            override fun onTranscodeCanceled() {}
            override fun onTranscodeFailed(exception: Exception?) {
                transcodeVideo(context, videoFile)
                exception?.printStackTrace()
            }

            override fun onTranscodeCompleted() {
                //begin upload
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

    private fun compressedFile(file: File): File {
        val mediaStorageDir = File(context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "OverTime720")
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Crashlytics.log("Compress File Error")
        }
        return File(mediaStorageDir.path + File.separator + file.name)
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


    @SuppressLint("CheckResult")
    override fun loadFromDB(context: Context) {
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
                when (listOfVideos.isNullOrEmpty()) {
                    true -> {
                    }
                    else -> {
                        lastVideoId = listOfVideos[0].id
                        transcodeVideo(context = context, videoFile = File(listOfVideos[0].vidPath))
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

    override fun subscribeToVideoGallery(): Observable<List<SavedVideo>> {
        return subject
            .subscribeOn(Schedulers.single())
            .observeOn(AndroidSchedulers.mainThread())
    }
}


class VideoUploadWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        return Result.failure()
    }
}
