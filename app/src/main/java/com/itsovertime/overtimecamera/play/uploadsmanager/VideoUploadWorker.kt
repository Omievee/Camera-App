package com.itsovertime.overtimecamera.play.uploadsmanager

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import javax.inject.Inject

class VideoUploadWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    @Inject
    lateinit var manager: UploadsManager

    override fun doWork(): Result {


        return Result.success()
    }



}
