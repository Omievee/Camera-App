package com.overtime.camera.camera

import android.os.Environment
import android.util.Size
import java.io.File

class CameraPresenter(val view: CameraFragment) {

    fun openCamera(width: Int, height: Int) {
        view.openCamera(width, height)
    }

    fun chooseVideoSize(choices: Array<Size>) = choices.firstOrNull {
        it.width == it.height * 4 / 3 && it.width <= 1080
    } ?: choices[choices.size - 1]

    fun startPreview() {
        view.startPreview()
    }

    fun getVideoFilePath(photoFileName: String): File {
        val mediaStorageDir =
            File(view.context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Overtime_Technical")

        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            println("Failed....")
        }
        println(">>>>>>>>  + " + File(mediaStorageDir.path + File.separator + photoFileName))
        return File(mediaStorageDir.path + File.separator + photoFileName)
    }


}