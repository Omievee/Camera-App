package com.itsovertime.overtimecamera.play.progress

import com.itsovertime.overtimecamera.play.application.OTApplication
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

class ProgressManagerImpl(val context: OTApplication) : ProgressManager {

    var qualitySubject: BehaviorSubject<Boolean> = BehaviorSubject.create()
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
    override fun onProgressComplete() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onUpdateProgress() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onNotifyPendingUploads() {
        subject.onNext(true)
    }


}