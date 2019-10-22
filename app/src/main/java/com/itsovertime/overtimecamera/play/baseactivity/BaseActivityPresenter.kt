package com.itsovertime.overtimecamera.play.baseactivity

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.authmanager.AuthenticationManager
import com.itsovertime.overtimecamera.play.network.AccessResponse
import com.itsovertime.overtimecamera.play.userpreference.UserPreference
import io.reactivex.disposables.Disposable

class BaseActivityPresenter(val view: BaseActivity, val auth: AuthenticationManager) {

    fun displayPermission() {
        view.beginPermissionsFlow()
    }

    fun checkPermissions(): Boolean {
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
            return false
        }
        return true
    }

    fun permissionsDenied() {

        view.showToast(view.applicationContext.getString(R.string.permissions_required_msg))
    }

    fun setUpAdapter() {
        println("set up presenter..")
        if (!checkPermissions()) {
            view.beginPermissionsFlow()
        } else view.setUpAdapter()

    }

    fun submitClicked(number: String) {
        view.displayProgress()
        sendCodeToProvidedNumber(number)
    }


    private var verifyDisposable: Disposable? = null
    private var numberProvided: String? = ""
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
        authDis?.dispose()
        userDisposable?.dispose()
    }

    fun resendAccessCode() {
        verifyDisposable?.dispose()
        view.displayProgress()
        sendCodeToProvidedNumber(numberProvided ?: return)
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
            .doOnSuccess {
                UserPreference.authToken = it.token
                refreshAuth()
            }
            .doOnError {
                view.hideDisplayProgress()
                view.showToast(view.applicationContext.getString(R.string.auth_invalid_access_code))
            }
            .subscribe({

            }, {

            })
    }

    var authDis: Disposable? = null
    private fun refreshAuth() {
        authDis?.dispose()
        authDis = auth
            .onRefreshAuth()
            .doOnSuccess {
                println("Successful refresh... $it")
                UserPreference.userId = it.data.user.id
                retrieveFullUser()
            }
            .doOnError {
                println("ERROR ::: $it")
            }
            .subscribe({
                println("ID IS::: ${UserPreference.userId}")
            }, {

            })
    }

    var userDisposable: Disposable? = null
    var allowAccess: Boolean = false
    fun retrieveFullUser() {
        view.displayProgress()
        userDisposable?.dispose()
        userDisposable = auth
            .getFullUser()
            .doOnError {
                it.printStackTrace()
            }
            .doFinally {
                view.hideDisplayProgress()
                view.hideKeyboard()
            }
            .subscribe({
                auth.saveUserToDB(it.user)
                UserPreference.accessAllowed = it.user.is_camera_authorized ?: false
                if (!UserPreference.accessAllowed) {
                    if (it.user.is_banned == true || it.user.is_suspended == true || it.user.is_camera_rejected == true) {
                        logOut()
                    }
                    view.displaySignUpPage()
                } else if (!checkPermissions()) {
                    println("PERMISSIONS DENIED")
                    view.beginPermissionsFlow()
                }else if (checkPermissions()){
                    println("PERMISSIONS ALLOWED")
                    view.disregardPermissions()
                }
            }, {

            })

    }

    private fun logOut() {
        auth.logOut()
        view.logOut()
    }

}

