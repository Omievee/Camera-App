package com.itsovertime.overtimecamera.play.progress

import android.database.Observable
import com.itsovertime.overtimecamera.play.uploads.ProgressData
import io.reactivex.Flowable
import java.util.*

interface ProgressManager {

    fun onUpdateProgress(id:String,progress: Int, hd: Boolean)
    fun onNotifyPendingUploads()
    fun subscribeToPendingHQUploads(): io.reactivex.Observable<Boolean>
    fun onSetMessageToMediumUploads()
    fun onSetMessageToHDUploads()
    fun subscribeToCurrentVideoQuality(): io.reactivex.Observable<Boolean>
    fun subscribeToUploadProgress(): Flowable<ProgressManagerImpl.UploadProgress>
}