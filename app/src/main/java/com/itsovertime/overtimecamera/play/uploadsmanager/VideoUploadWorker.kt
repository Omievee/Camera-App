package com.itsovertime.overtimecamera.play.uploadsmanager

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.Worker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.itsovertime.overtimecamera.play.db.AppDatabase
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.quemanager.QueManager
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class VideoUploadWorker(

    context: Context,
    workerParams: WorkerParameters
) :
    Worker(context, workerParams) {

    var db = AppDatabase.getAppDataBase(context = context)

    @Inject
    lateinit var uploadsManager: UploadsManager

    var disp: Disposable? = null
    @SuppressLint("CheckResult")
    override fun doWork(): Result {
        var video: SavedVideo? = null
        try {
            Single.fromCallable {
                db?.videoDao()?.getVideos()
            }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    disp = uploadsManager
                        .getVideoInstance(it?.get(0))
                        .doOnError {
                            println("Error.. .${it.message}")
                        }
                        .doOnSuccess {

                        }
                        .subscribe({

                        }, {

                        })

                }, {
                    it.printStackTrace()
                })








            return Result.success()
        } catch (throwable: Throwable) {
          println("THROWING.... ${ throwable.message}")
            return Result.failure()
        }
    }
}

class DaggerWorkerFactory(private val uploads: UploadsManager) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {

        val workerKlass = Class.forName(workerClassName).asSubclass(Worker::class.java)
        val constructor =
            workerKlass.getDeclaredConstructor(Context::class.java, WorkerParameters::class.java)
        val instance = constructor.newInstance(appContext, workerParameters)

        when (instance) {
            is VideoUploadWorker -> {
                instance.uploadsManager = uploads
            }

        }

        return instance
    }
}
