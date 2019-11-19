package com.itsovertime.overtimecamera.play.workmanager

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class VideoRegistrationWorker(
    val context: Context,
    val workerParams: WorkerParameters
) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}