package com.itsovertime.overtimecamera.play.baseactivity

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.itsovertime.overtimecamera.play.authmanager.AuthenticationManager
import com.itsovertime.overtimecamera.play.userpreference.UserPreference
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
        verifyDisposable?.dispose()
        verifyDisposable = auth
            .onRequestAccessCodeForNumber(numberProvided ?: "")
            .doFinally {
                view.hideDisplayProgress()
            }
            .doOnSuccess {
                view.displayEnterResponseView(numberProvided ?: "")
            }
            .doOnError {
                view.displayErrorFromResponse()
            }
            .subscribe({

            }, {

            })
    }

    fun onDestroy() {
        verifyDisposable?.dispose()
        codeDisposable?.dispose()
    }

    fun resendAccessCode() {

    }

    fun resetViews() {
        view.resetViews()
    }

    var codeDisposable: Disposable? = null
    fun submitAccessCode(code: String) {
        view.displayProgress()
        codeDisposable?.dispose()
        codeDisposable = auth
            .onVerifyAccessCodeRecieved(code)
            .doFinally {
                view.hideDisplayProgress()
                refreshAuth()
            }
            .doOnSuccess {
                UserPreference.authToken = it.token
                view.displaySignUpPage()
            }
            .doOnError {
                println("Stacktrace :: ${it.message}")
            }
            .subscribe({

            }, {

            })
    }

    var authDis: Disposable? = null
    private fun refreshAuth() {
        println("REFRESHING>>>>>>>>>>>>...")
        authDis?.dispose()
        authDis = auth
            .onRefreshAuth()
            .doOnSuccess {
                println("WTFFF>>>>>>>.... ${it.data.user.id}")
                UserPreference.userId = it.data.user.id
            }
            .doOnError {
                println("ERROR ::: $it")
            }
            .subscribe({
                println("ID IS::: ${UserPreference.userId}")
            }, {

            })
    }


}

