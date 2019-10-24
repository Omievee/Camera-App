package com.itsovertime.overtimecamera.play.progress

import android.database.Observable
import io.reactivex.Flowable
import java.util.*

interface ProgressManager {

    fun onUpdateProgress(id: String, progress: Int, hd: Boolean)
    fun onUpdateUploadMessage(): io.reactivex.Observable<UploadsMessage>
    fun onCurrentUploadProcess(msg: UploadsMessage)
    fun subscribeToUploadProgress(): Flowable<ProgressManagerImpl.UploadProgress>
}