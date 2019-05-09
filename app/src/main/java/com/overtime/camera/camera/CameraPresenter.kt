package com.overtime.camera.camera

import android.os.Environment
import android.util.Size
import java.io.File

class CameraPresenter(val view: CameraFragment) {


    fun getVideoFilePath(photoFileName: String): File {
        val mediaStorageDir = File(view?.context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Camera_App.OT")

        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            println("Failed....")
        }
        println(">>>>>>>>  + " + File(mediaStorageDir.path + File.separator + photoFileName))
        return File(mediaStorageDir.path + File.separator + photoFileName)
    }

    fun startPreview() {
        view.startPreview()
    }


}