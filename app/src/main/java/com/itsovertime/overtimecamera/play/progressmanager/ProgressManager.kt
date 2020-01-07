package com.itsovertime.overtimecamera.play.progressmanager

import io.reactivex.Flowable

interface ProgressManager {

    fun onUpdateProgress(id: String, progress: Int, hd: Boolean)
    fun onUpdateUploadMessage(): io.reactivex.Observable<UploadsMessage>
    fun onCurrentUploadProcess(msg: UploadsMessage)
    fun subscribeToUploadProgress(): Flowable<ProgressManagerImpl.UploadProgress>
}