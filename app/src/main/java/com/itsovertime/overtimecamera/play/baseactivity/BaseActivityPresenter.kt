package com.itsovertime.overtimecamera.play.baseactivity

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class BaseActivityPresenter(val view: BaseActivity) {

    fun onCreate() {
        checkPermissions()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                view,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(
                view,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                view,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                view,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            view.displayAlert()
        } else {
            view.setUpAdapter()
        }
    }

    fun permissionsDenied() {

    }

    fun setUpAdapter() {
        view.setUpAdapter()
    }


}