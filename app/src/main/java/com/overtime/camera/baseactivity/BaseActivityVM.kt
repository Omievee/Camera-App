package com.overtime.camera.baseactivity

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel

class BaseActivityVM(val view: BaseActivity) : ViewModel() {

    fun onCreate() {
        checkPermissions()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(view, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(
                view,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {

        } else {

        }
    }
}