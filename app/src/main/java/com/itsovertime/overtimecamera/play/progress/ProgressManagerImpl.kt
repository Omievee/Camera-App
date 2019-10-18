package com.itsovertime.overtimecamera.play.progress

import com.itsovertime.overtimecamera.play.application.OTApplication
import com.itsovertime.overtimecamera.play.uploads.ProgressData
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

class ProgressManagerImpl(val context: OTApplication) : ProgressManager {
    override fun subscribeToUploadProgress(): Flowable<UploadProgress> {
        return progressSubject
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .toFlowable(BackpressureStrategy.LATEST)

    }

    data class UploadProgress(val id: String, val prog: Int, val isHD: Boolean)

    private var progressSubject: BehaviorSubject<UploadProgress> = BehaviorSubject.create()
    override fun onUpdateProgress(id: String, progress: Int, hd: Boolean) {

        println("progress...... $progress")
        progressSubject.onNext(UploadProgress(id, progress, hd))
    }

    private var qualitySubject: BehaviorSubject<Boolean> = BehaviorSubject.create()
    override fun subscribeToCurrentVideoQuality(): Observable<Boolean> {
        return qualitySubject
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    override fun onSetMessageToMediumUploads() {
        qualitySubject.onNext(false)
    }

    override fun onSetMessageToHDUploads() {
        qualitySubject.onNext(true)
    }

    override fun subscribeToPendingHQUploads(): Observable<Boolean> {
        return subject
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    val subject: BehaviorSubject<Boolean> = BehaviorSubject.create()


    override fun onNotifyPendingUploads() {
        subject.onNext(true)
    }


}