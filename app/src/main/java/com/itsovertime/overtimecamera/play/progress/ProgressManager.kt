package com.itsovertime.overtimecamera.play.progress

import android.database.Observable
import java.util.*

interface ProgressManager {

    fun onProgressComplete()
    fun onUpdateProgress()
    fun onNotifyPendingUploads()
    fun subcribeToPendingHQUploads(): io.reactivex.Observable<Boolean>
}