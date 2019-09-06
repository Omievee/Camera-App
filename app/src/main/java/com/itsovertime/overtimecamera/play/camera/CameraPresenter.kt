package com.itsovertime.overtimecamera.play.camera

import android.annotation.SuppressLint
import android.os.Environment
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout
import com.itsovertime.overtimecamera.play.eventmanager.EventManager
import com.itsovertime.overtimecamera.play.model.Event
import com.itsovertime.overtimecamera.play.progressbar.ProgressBarAnimation
import com.itsovertime.overtimecamera.play.videomanager.VideosManager
import io.reactivex.disposables.Disposable
import java.io.File


class CameraPresenter(
    val view: CameraFragment,
    val manager: VideosManager,
    val eventsManager: EventManager
) {

    private var filePath: String? = null
    private var totalDisposable: Disposable? = null
    private var eventDisposable: Disposable? = null

    fun getVideoFilePath(photoFileName: String): File {
        val mediaStorageDir =
            File(view.context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "OverTime1080")
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
        }

        filePath = mediaStorageDir.path + File.separator + "$photoFileName.mp4"
        return File(mediaStorageDir.path + File.separator + "$photoFileName.mp4")
    }

    var e: Event? = null
    fun saveRecordingToDataBase(videoEvent: Event?) {
        e = videoEvent
        filePath?.let {
            manager.saveHighQualityVideoToDB(
                event = e,
                filePath = it,
                isFavorite = false
            )
        }
        view.engageCamera()
    }

    fun onCreate() {
        checkGallerySize()
    }

    @SuppressLint("CheckResult")
    fun animateProgressBar(progressBar: ProgressBar, maxTime: Int) {
        val anim = ProgressBarAnimation(progressBar, 0, maxTime * 1000)
        anim.duration = (maxTime * 1000).toLong()
        progressBar.max = maxTime * 1000
        progressBar.startAnimation(anim)
    }

    fun updateFavoriteField() {
        manager.updateVideoFavorite(isFavorite = true)
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
                view.setUpEventViewData(ev)
                view.updateEventTitle(eventName ?: "")
            }, {

            })


    }

    fun displayHiddenView() {
        view.openEvents()
    }

    fun expand(v: View) {
        v.measure(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        val targetHeight = v.measuredHeight

        v.layoutParams.height = 1
        v.visibility = View.VISIBLE

        val animate = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                v.layoutParams.height =
                    if (interpolatedTime == 1f) ConstraintLayout.LayoutParams.WRAP_CONTENT else (targetHeight * interpolatedTime).toInt()
                v.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

        animate.duration = 2000
        v.startAnimation(animate)
    }

    fun collapse(v: View) {
        val initialHeight = v.measuredHeight
        val a = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                if (interpolatedTime == 1f) {
                    v.visibility = View.GONE
                } else {
                    v.layoutParams.height =
                        initialHeight - (initialHeight * interpolatedTime).toInt()
                    v.requestLayout()
                }
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

        a.duration = 2000
        v.startAnimation(a)
    }

    fun changeEvent(event: String) {
        view.updateEventTitle(event)
    }

    fun hideEvents() {
        view.hideEventsRV()
    }

}

//TODO  Selfie Cam has no live cam & options
//TODO: