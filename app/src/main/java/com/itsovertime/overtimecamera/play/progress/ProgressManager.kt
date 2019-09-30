package com.itsovertime.overtimecamera.play.progress

import android.database.Observable
import java.util.*

interface ProgressManager {

    fun onProgressComplete()
    fun onUpdateProgress()
    fun onNotifyPendingUploads()
    fun subscribeToPendingHQUploads(): io.reactivex.Observable<Boolean>
    fun onSetMessageToMediumUploads()
    fun onSetMessageToHDUploads()
    fun subscribeToCurrentVideoQuality(): io.reactivex.Observable<Boolean>

}