package com.overtime.camera.camera

import android.os.Environment
import java.io.File


class CameraPresenter(val view: CameraFragment) {


    fun getVideoFilePath(photoFileName: String): File {
//        val mediaStorageDir = File(view.context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Camera_App.OT")
//
//        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
//            println("Failed....")
//        }
//        println(">>>>>>>>  + " + File(mediaStorageDir.path + File.separator + photoFileName))
//
        val dirPath = Environment.getExternalStorageDirectory().absolutePath.toString()
        val myDir = File(dirPath + "/OverTime")
        if (!myDir.exists()) {
            myDir.mkdirs()
        }
        return File(myDir, "$photoFileName.mp4")
    }

    fun startPreview() {
        view.startPreview()
    }


}