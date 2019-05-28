package com.itsovertime.overtimecamera.play.camera

import android.os.Environment
import com.itsovertime.overtimecamera.play.videomanager.VideosManager
import java.io.File
import java.io.FileOutputStream
import java.util.*


class CameraPresenter(private val view: CameraFragment, val manager: VideosManager) {

    fun getVideoFilePath(photoFileName: String): File {
        val mediaStorageDir = File(view.context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "OverTime1080")
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            println("Failed....")
        }

        saveToDB(
            path = mediaStorageDir.path + File.separator + "$photoFileName.mp4",
            photoFileName = photoFileName,
            isFavorite = false
        )

        val f = File(mediaStorageDir.path + File.separator + "$photoFileName.mp4")
        return f
    }

    fun saveToDB(path: String, photoFileName: String, isFavorite: Boolean) {

        manager.saveVideoToDB(view.context ?: return, path, false)

        val dirPath = Environment.getExternalStorageDirectory().absolutePath.toString()
        val myDir = File("$dirPath/OverTime")
        if (!myDir.exists()) {
            myDir.mkdirs()
        }
        val file = File(myDir, "$photoFileName.mp4")
        try {
            val out = FileOutputStream(file)
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun recordingStopped() {
        manager.loadFromDB(view.context ?: return)

        view.startPreview()
    }
}