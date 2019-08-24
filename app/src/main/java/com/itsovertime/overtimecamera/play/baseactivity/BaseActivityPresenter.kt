package com.itsovertime.overtimecamera.play.baseactivity

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.itsovertime.overtimecamera.play.authmanager.AuthenticationManager
import io.reactivex.disposables.Disposable

class BaseActivityPresenter(val view: BaseActivity, val auth: AuthenticationManager) {

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

    fun submitClicked(number: String) {
        view.displayProgress()
        sendCodeToProvidedNumber(number)
    }


    var verifyDisposable: Disposable? = null
    var numberProvided: String? = ""
    private fun sendCodeToProvidedNumber(number: String) {
        numberProvided = number
        view.hideDisplayProgress()
        verifyDisposable?.dispose()
        verifyDisposable = auth
            .onRequestAccessCodeForNumber(numberProvided ?: "")
            .doOnSuccess {
                println("This is... $it")
              //  view.displayEnterResponseView(numberProvided ?: "")
            }
            .doOnError {
                println("Stack... ${it.message}")
               // view.displayErrorFromResponse()
            }
            .subscribe({

            }, {

            })
    }

    fun onDestroy() {
        verifyDisposable?.dispose()
    }

    fun resendAccessCode() {

    }

    fun resetViews() {
        view.resetViews()
    }


}