package com.itsovertime.overtimecamera.play.camera

import android.annotation.SuppressLint
import android.os.Build
import android.os.CountDownTimer
import android.os.Environment
import android.os.StatFs
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.ProgressBar
import android.widget.TextView
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.itsovertime.overtimecamera.play.analytics.OTAnalyticsManager
import com.itsovertime.overtimecamera.play.authmanager.AuthenticationManager
import com.itsovertime.overtimecamera.play.eventmanager.EventManager
import com.itsovertime.overtimecamera.play.model.Event
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.UploadState
import com.itsovertime.overtimecamera.play.model.User
import com.itsovertime.overtimecamera.play.progressbar.ProgressBarAnimation
import com.itsovertime.overtimecamera.play.videomanager.VideosManager
import com.itsovertime.overtimecamera.play.workmanager.VideoUploadWorker
import io.reactivex.disposables.Disposable
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*


class CameraPresenter(
    val view: CameraFragment,
    val manager: VideosManager,
    val authManager: AuthenticationManager,
    private val eventsManager: EventManager,
    val analytics: OTAnalyticsManager
) {

    private var filePath: String? = null
    private var totalDisposable: Disposable? = null
    private var eventDisposable: Disposable? = null

    @Synchronized
    fun getVideoFilePath(photoFileName: String): File {
        synchronized(this) {
            val mediaStorageDir =
                File(view.context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Capture")
            if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
                analytics.onTrackFailedToCreateFile()
            }
            filePath = mediaStorageDir.path + File.separator + "$photoFileName.mp4"
            return File(mediaStorageDir.path + File.separator + "$photoFileName.mp4")
        }
    }

    var e: Event? = null
    var video: SavedVideo? = null
    var clientId: String = ""

    @Synchronized
    fun saveVideo(videoEvent: Event?) {
        e = videoEvent
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
                filmed_at = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                videoId = ""
            )
        }
        analytics.onTrackVideoFileCreated(video)
        manager.onSaveVideoToDb(video ?: return)
        view.engageCamera()

    }


    fun onCreate() {
        subscribeToGallerySize()
        manager.onLoadFFMPEG()
        manager.onLoadDb()
        user()
        startUploadWorkManager()

        if (getAvailableInternalMemory() < 5) {
            view.notifyOfLowStorage()
        }
    }

    fun getAvailableInternalMemory(): Int {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val availableBlocks = stat.availableBlocksLong
        return formatSize(availableBlocks * blockSize)
    }

    private fun formatSize(size: Long): Int {
        var size = size
        var suffix: String? = null
        if (size >= 1024) {
            suffix = "KB"
            size /= 1024
            if (size >= 1024) {
                suffix = "MB"
                size /= 1024
            }
        }
        return size.toInt()
//        val resultBuffer = StringBuilder(java.lang.Long.toString(size))
//        var commaOffset = resultBuffer.length - 3
//        while (commaOffset > 0) {
//            resultBuffer.insert(commaOffset, '.')
//            commaOffset -= 3
//        }
//        if (suffix != null) resultBuffer.append(suffix)
//        return resultBuffer.toString()
    }

    private fun startUploadWorkManager() {
        println("First run work manager ________________")
        val workRequest =
            OneTimeWorkRequestBuilder<VideoUploadWorker>().addTag("UploadWork").build()
        WorkManager.getInstance(view.context ?: return)
            .enqueueUniqueWork("UploadWork", ExistingWorkPolicy.KEEP, workRequest)
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
                    text.apply {
                        setText("                        ${maxTime}s")
                    }
                }
            }
        }.start()
    }

    fun updateFavoriteField() {
        video?.is_favorite = true
        manager.onVideoIsFavorite(true, video ?: return)
    }

    fun cameraSwitch() {
        view.switchCameras()
    }

    fun setUpClicks() {
        view.setUpClicks()
    }

    fun updateFunnyField() {
        video?.is_funny = true
        manager.onVideoIsFunny(isFunny = true, clientId = clientId)
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
                println("PRESENTER CALLED :::: $it")
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


    var taggedEvents: MutableList<Event> = arrayListOf()
    fun getEvents() {
        taggedEvents?.clear()
        var defaultEvent: Event? = null
        val eventsList = mutableListOf<Event>()
        eventDisposable?.dispose()
        eventDisposable = eventsManager
            .getEvents()
            .map { er ->
                er.events.forEachIndexed { i, event ->
                    event.videographer_ids.forEach { s ->
                        if (s == user?.id) {
                            taggedEvents.add(er.events[i])
                        }
                    }
                }
                eventsList.addAll(er.events)
                taggedEvents.asReversed().forEach {
                    eventsList.remove(it)
                    eventsList.add(0, it)
                }
            }
            .doOnError {
                it.printStackTrace()
                println("ERROR from events.... ${it.message}")
            }
            .subscribe({
                view.setUpEventViewData(eventsList)
                view.setUpDefaultEvent(taggedEvents[0])
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

    fun expand(v: View) {
        val matchParentMeasureSpec =
            View.MeasureSpec.makeMeasureSpec((v.parent as View).width, View.MeasureSpec.EXACTLY)
        val wrapContentMeasureSpec =
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        v.measure(matchParentMeasureSpec, wrapContentMeasureSpec)
        val targetHeight = v.measuredHeight

        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        v.layoutParams.width = 1
        v.visibility = View.VISIBLE
        val a = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                v.layoutParams.width = if (interpolatedTime == 1f)
                    ViewGroup.LayoutParams.MATCH_PARENT
                else
                    (targetHeight * interpolatedTime).toInt()
                v.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

        // Expansion speed of 1dp/ms
        a.duration = (targetHeight / v.context.resources.displayMetrics.density).toInt().toLong()
        v.startAnimation(a)
    }

    fun collapse(v: View) {
        val initialHeight = v.measuredWidth
        val a = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                if (interpolatedTime == 1f) {
                    v.visibility = View.GONE
                } else {
                    v.layoutParams.width =
                        initialHeight - (initialHeight * interpolatedTime).toInt()
                    v.requestLayout()
                }
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

        // Collapse speed of 1dp/ms
        a.duration = (initialHeight / v.context.resources.displayMetrics.density).toInt().toLong()
        v.startAnimation(a)
    }

    fun changeEvent(event: Event) {
        view.setUpDefaultEvent(event)
    }


    fun updateTaggedAthletesField(taggedAthletesArray: ArrayList<String>) {
        manager.onUpdatedTaggedAthletesInDb(
            taggedAthletesArray = taggedAthletesArray,
            clientId = clientId
        )
    }

    fun onTrackEvent(event: Event) {
        analytics.onTrackSelectedEvent(event)
    }

    fun onTrackStartedRecording() {
        analytics.onTrackCameraRecording()
    }


}
