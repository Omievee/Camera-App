package com.itsovertime.overtimecamera.play.camera

import android.annotation.SuppressLint
import android.os.Environment
import android.widget.ProgressBar
import com.itsovertime.overtimecamera.play.progressbar.ProgressBarAnimation
import com.itsovertime.overtimecamera.play.videomanager.VideosManager
import io.reactivex.disposables.Disposable
import java.io.File
import java.util.*


class CameraPresenter(val view: CameraFragment, val manager: VideosManager) {

    var filePath: String? = null

    var totalDisposable: Disposable? = null

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
    }

    fun clearProgressAnimation() {
        view.stopProgressAnimation()
    }

    fun determineViewsForCameraId() {
        view.showOrHideViewsForCamera()
    }

}

//TODO  Selfie Cam has no live cam & options
//TODO: