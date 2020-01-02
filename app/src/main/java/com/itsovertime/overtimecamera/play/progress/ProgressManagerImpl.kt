package com.itsovertime.overtimecamera.play.progress

import com.itsovertime.overtimecamera.play.application.OTApplication
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

class ProgressManagerImpl(val context: OTApplication) : ProgressManager {
    data class UploadProgress(val id: String, val prog: Int, val isHD: Boolean)

    private var uploadSubject = BehaviorSubject.create<UploadsMessage>()
    override fun onCurrentUploadProcess(msg: UploadsMessage) {
        uploadSubject.onNext(msg)
    }

    override fun onUpdateUploadMessage(): Observable<UploadsMessage> {
        return uploadSubject
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    override fun subscribeToUploadProgress(): Flowable<UploadProgress> {
        return progressSubject
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .toFlowable(BackpressureStrategy.LATEST)
    }


    private var progressSubject: BehaviorSubject<UploadProgress> = BehaviorSubject.create()
    override fun onUpdateProgress(id: String, progress: Int, hd: Boolean) {
        println("Progress!! $progress")
        progressSubject.onNext(UploadProgress(id, progress, hd))
    }

    val subject: BehaviorSubject<Boolean> = BehaviorSubject.create()
}

enum class UploadsMessage {
    Uploading_Medium,
    Uploading_High,
    Pending_Medium,
    Pending_High,
    Finished,
    NO_NETWORK
}