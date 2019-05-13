package com.overtime.camera.camera

import android.os.Environment
import java.io.File


class CameraPresenter(private val view: CameraFragment) {

    fun getVideoFilePath(photoFileName: String): File {
        val dirPath = Environment.getExternalStorageDirectory().absolutePath.toString()
        val myDir = File("$dirPath/OverTime")
        if (!myDir.exists()) {
            myDir.mkdirs()
        }
        return File(myDir, "$photoFileName.mp4")
    }

    fun startPreview() {
        view.startPreview()
    }
}