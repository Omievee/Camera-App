package com.itsovertime.overtimecamera.play.camera

import android.annotation.SuppressLint
import android.os.Environment
import android.widget.ProgressBar
import com.itsovertime.overtimecamera.play.eventmanager.EventManager
import com.itsovertime.overtimecamera.play.model.Event
import com.itsovertime.overtimecamera.play.progressbar.ProgressBarAnimation
import com.itsovertime.overtimecamera.play.videomanager.VideosManager
import io.reactivex.disposables.Disposable
import java.io.File


class CameraPresenter(val view: CameraFragment, val manager: VideosManager, val eventsManager: EventManager) {

    private var filePath: String? = null
    private var totalDisposable: Disposable? = null
    private var eventDisposable: Disposable? = null

    fun getVideoFilePath(photoFileName: String): File {
        val mediaStorageDir = File(view.context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "OverTime1080")
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
        }

        filePath = mediaStorageDir.path + File.separator + "$photoFileName.mp4"
        return File(mediaStorageDir.path + File.separator + "$photoFileName.mp4")
    }

    fun saveRecordingToDataBase() {
        println("Saving............")

        filePath?.let {
            manager.saveHighQualityVideoToDB(
                    filePath = it,
                    isFavorite = false
            )
        }
        view.startPreview()
    }

    fun onCreate() {
        checkGallerySize()
    }

    @SuppressLint("CheckResult")
    fun animateProgressBar(progressBar: ProgressBar) {
        val anim = ProgressBarAnimation(progressBar, 0, 12000)
        anim.duration = 12000
        progressBar.max = 12000
        progressBar.startAnimation(anim)
    }

    fun updateFavoriteField() {
        manager.updateVideoFavorite(true)
    }

    fun cameraSwitch() {
        view.switchCameras()
    }

    fun setUpClicks() {
        view.setUpClicks()
    }

    fun updateFunnyField() {
        manager.updateVideoFunny(isFunny = true)
    }

    fun deletePreviousFile() {
        val previousFile = File(filePath ?: return)
        if (previousFile.exists()) {
            previousFile.delete()
        }
        filePath = null
    }


    fun checkGallerySize() {
        totalDisposable?.dispose()
        totalDisposable = manager
                .subscribeToVideoGallerySize()
                .subscribe({
                    view.updateUploadsIconCount(it.toString())
                }, {

                })
    }

    fun onDestroy() {
        totalDisposable?.dispose()
        eventDisposable?.dispose()
    }

    fun clearProgressAnimation() {
        view.stopProgressAnimation()
    }

    fun determineViewsForCameraId() {
        view.showOrHideViewsForCamera()
    }

    var ev: List<Event>? = null
    var eventName: String? = ""
    fun getEvents() {
        eventDisposable?.dispose()
        eventDisposable = eventsManager
                .getEvents()
                .map {
                    eventName = it?.events?.get(0).name ?: ""
                    ev = it.events
                }
                .subscribe({
                    println("size from disp:... ${ev?.size}")
                    view.setUpEventViewData(eventName, ev)
                }, {

                })


    }

    fun displayEventsFragment(evList: List<Event>) {
        view.openEvents(evList)
    }

}

//TODO  Selfie Cam has no live cam & options
//TODO: