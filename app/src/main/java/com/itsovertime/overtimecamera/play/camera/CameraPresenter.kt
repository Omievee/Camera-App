package com.itsovertime.overtimecamera.play.camera

import android.annotation.SuppressLint
import android.icu.text.SimpleDateFormat
import android.os.CountDownTimer
import android.os.Environment
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import com.itsovertime.overtimecamera.play.authmanager.AuthenticationManager
import com.itsovertime.overtimecamera.play.eventmanager.EventManager
import com.itsovertime.overtimecamera.play.model.Event
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.UploadState
import com.itsovertime.overtimecamera.play.model.User
import com.itsovertime.overtimecamera.play.progressbar.ProgressBarAnimation
import com.itsovertime.overtimecamera.play.videomanager.VideosManager
import com.itsovertime.overtimecamera.play.workmanager.VideoUploadWorker
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList


class CameraPresenter(
    val view: CameraFragment,
    val manager: VideosManager,
    val authManager: AuthenticationManager,
    private val eventsManager: EventManager
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
    var video: SavedVideo? = null
    var clientId: String = ""
    fun saveVideo(videoEvent: Event?, arrayOfTaggedUsers: ArrayList<String>) {
        e = videoEvent
        e?.tagged_teams?.forEach {
            it.taggable_athletes?.forEach {
                println("athletes.. ${it.id}")
            }
        }
        clientId = UUID.randomUUID().toString()
        filePath?.let {
            video = SavedVideo(
                clientId = clientId,
                highRes = it,
                is_favorite = false,
                event_id = e?.id ?: "",
                eventName = e?.name ?: "",
                starts_at = e?.starts_at ?: "",
                address = e?.address ?: "",
                latitude = e?.latitude ?: 0.0,
                city = e?.city ?: "",
                duration_in_hours = e?.duration_in_hours ?: 0,
                longitude = e?.longitude ?: 0.0,
                uploadState = UploadState.QUEUED,
                max_video_length = e?.max_video_length ?: 12,
                created_at = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                taggedUsers = arrayOfTaggedUsers
            )
        }
        manager.saveHighQualityVideoToDB(video ?: return)

        view.engageCamera()
    }


    fun onCreate() {
        subscribeToGallerySize()
        manager.loadFFMPEG()
        manager.loadFromDB()
        user()
    }


    fun onResume() {

    }


    private var countDownTimer: CountDownTimer? = null
    @SuppressLint("CheckResult")
    fun animateProgressBar(text: TextView, progressBar: ProgressBar, maxTime: Int) {
        maxTime + 1
        val anim = ProgressBarAnimation(progressBar, 0, maxTime * 1000)
        anim.duration = (maxTime * 1000).toLong()
        progressBar.max = maxTime * 1000
        progressBar.startAnimation(anim)

        countDownTimer = object : CountDownTimer(maxTime * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                view.activity?.runOnUiThread {
                    text.text =
                        ("Save the last ${(maxTime) - millisUntilFinished / 1000}s").toString()
                }
            }

            override fun onFinish() {
                view.activity?.runOnUiThread {
                    text.text = "                        ${maxTime}s"
                }
            }
        }.start()
    }

    fun updateFavoriteField() {
        manager.updateVideoFavorite(true, clientId)
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


    private fun subscribeToGallerySize() {
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
        countDownTimer?.cancel()
        view.stopProgressAnimation()
    }

    fun determineViewsForCameraId() {
        view.showOrHideViewsForCamera()
    }


    fun getEvents() {
        var eventName: String? = "Unknown Event"
        val eventsList = mutableListOf<Event>()
        eventDisposable?.dispose()
        eventDisposable = eventsManager
            .getEvents()
            .map { er ->
                println("User ID Is ${user?.id}")
                er.events.forEachIndexed { i, event ->
                    event.videographer_ids.forEach { s ->
                        if (s == user?.id) {
                            eventName = er.events[i].name
                        }
                    }
                }
                eventsList.addAll(er.events)
            }
            .doOnError {
                println("ERROR from events.... ${it.message}")
            }
            .subscribe({
                view.setUpEventViewData(eventsList)
                view.updateEventTitle(eventName)
            }, {
            })
    }


    private var authdisp: Disposable? = null
    var user: User? = null
    fun user() {
        authdisp?.dispose()
        authdisp = authManager
            .getUserId()
            ?.doOnError {
                it.printStackTrace()
            }
            ?.subscribe({
                user = it
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
