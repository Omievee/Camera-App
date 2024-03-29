package com.itsovertime.overtimecamera.play.baseactivity

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.analytics.OTAnalyticsManager
import com.itsovertime.overtimecamera.play.authmanager.AuthenticationManager
import com.itsovertime.overtimecamera.play.model.User
import com.itsovertime.overtimecamera.play.network.AccessResponse
import com.itsovertime.overtimecamera.play.notifications.NotificationManager
import com.itsovertime.overtimecamera.play.userpreference.UserPreference
import com.mixpanel.android.mpmetrics.MixpanelAPI
import io.reactivex.disposables.Disposable

class BaseActivityPresenter(
    val view: BaseActivity,
    val auth: AuthenticationManager,
    val analytics: OTAnalyticsManager,
    val notifications: NotificationManager
) {

    fun onCreate() {
        analytics.initMixpanel(cntx = view, userId = UserPreference.userId)
    }

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
        println("SETTING ADAPTER")
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
            .onRequestAccessCodeForNumber(number)
            .doFinally {
                view.hideDisplayProgress()
            }
            .doOnSuccess {
                view.displayEnterResponseView(number)
            }
            .doOnError {
                it.printStackTrace()
                view.displayErrorFromResponse()
            }
            .subscribe({

            }, {

            })
    }

    fun onDestroy() {
        analytics.onDestroyMixpanel()
        verifyDisposable?.dispose()
        codeDisposable?.dispose()
        authDis?.dispose()
        userDisposable?.dispose()
        notifications.onClearNotifications()
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
        println("access code.... $code")
        codeDisposable?.dispose()
        codeDisposable = auth
            .onVerifyAccessCodeRecieved(code)
            .doOnSuccess {
                UserPreference.authToken = it.token
                refreshAuth()
            }
            .doOnError {
                println("error... ? ${it.message}")
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
            .doOnNext {
                val res = it.body() ?: return@doOnNext
                UserPreference.userId = res.data.user.id
                println("user id is ...${UserPreference.userId}")

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
                    if (it.user.is_banned || it.user.is_suspended || it.user.is_camera_rejected) {
                        logOut()
                    }
                    view.displaySignUpPage()
                } else if (!checkPermissions()) {
                    println("PERMISSIONS DENIED")
                    view.beginPermissionsFlow()
                } else if (checkPermissions()) {
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

