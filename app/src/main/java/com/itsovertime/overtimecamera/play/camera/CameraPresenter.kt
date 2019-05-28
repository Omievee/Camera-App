package com.itsovertime.overtimecamera.play.camera

import android.os.Environment
import com.itsovertime.overtimecamera.play.videomanager.VideosManager
import java.io.File
import java.io.FileOutputStream
import java.util.*


class CameraPresenter(private val view: CameraFragment, val manager: VideosManager) {

    var filePath: String? = null

    fun getVideoFilePath(photoFileName: String): File {
        val mediaStorageDir = File(view.context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "OverTime1080")
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            println("Failed....")
        }

        filePath = mediaStorageDir.path + File.separator + "$photoFileName.mp4"
        return File(mediaStorageDir.path + File.separator + "$photoFileName.mp4")
    }

    fun recordingStopped() {
        filePath?.let {
            manager.saveVideoToDB(
                    view.context ?: return,
                    filePath = it,
                    isFavorite = false
            )
        }
        manager.loadFromDB(view.context ?: return)
        view.startPreview()
    }


}