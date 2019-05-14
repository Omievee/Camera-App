package com.overtime.camera.camera

import android.os.Environment
import com.overtime.camera.videomanager.VideosManager
import java.io.File


class CameraPresenter(private val view: CameraFragment, val manager: VideosManager) {

    fun getVideoFilePath(photoFileName: String): File {
        val mediaStorageDir = File(view.context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "OverTime")
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            println("Failed....")
        }

        println("path is: ${mediaStorageDir.path + File.separator + "$photoFileName.mp4"}")
        saveToDB(mediaStorageDir.path + File.separator + "$photoFileName.mp4")
        return File(mediaStorageDir.path + File.separator + "$photoFileName.mp4")
    }

    fun saveToDB(path: String) {
        manager.saveVideoToDB(view.context ?: return, path)
//        val dirPath = Environment.getExternalStorageDirectory().absolutePath.toString()
//        val myDir = File("$dirPath/OverTime")
//        if (!myDir.exists()) {
//            myDir.mkdirs()
//        }
//        return File(myDir, "$photoFileName.mp4")
    }

    fun startPreview() {
        view.startPreview()
    }
}

///storage/emulated/0/AzRecorderFree/2019_05_13_16_02_18.mp4
///storage/emulated/0/Android/data/com.overtime.camera/files/Pictures/OverTime/1557863277905.mp4