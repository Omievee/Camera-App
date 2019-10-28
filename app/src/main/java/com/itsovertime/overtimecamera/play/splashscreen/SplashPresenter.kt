package com.itsovertime.overtimecamera.play.splashscreen

import com.itsovertime.overtimecamera.play.authmanager.AuthenticationManager
import com.itsovertime.overtimecamera.play.userpreference.UserPreference
import io.reactivex.disposables.Disposable

class SplashPresenter(val view: SplashActivity, val auth: AuthenticationManager) {

    private var authDis: Disposable? = null
    fun refreshAuth() {
        authDis?.dispose()
        authDis = auth
            .onRefreshAuth()
            .doOnNext {
                if (it.code() != 200) {
                    view.logOut()
                } else {
                    println("????? ${it.body()?.data?.user}")
                    val response = it.body() ?: return@doOnNext
                    UserPreference.authToken = it.body()?.token ?: ""
                    UserPreference.userId = it.body()?.data?.user?.id ?: ""
                    //UserPreference.accessAllowed = response.data.user.is_camera_authorized

                    if (response?.data?.user.is_suspended || response.data.user.is_banned || response.data.user.is_camera_rejected) {
                        view.logOut()
                    }
                    if (UserPreference.authToken != "") {
                        view.authSuccessful()
                    } else view.logOut()
                }
            }
            .doOnError {
                println("Error.... ${it.message}")
                view.displayNetworkError()
            }
            .subscribe({
                println("ID IS::: ${UserPreference.userId}")
            }, {

            })
    }

    var userDisposable: Disposable? = null
//    fun retrieveFullUser() {
//        userDisposable?.dispose()
//        userDisposable = auth
//            .getFullUser()
//            .doOnError {
//                it.printStackTrace()
//            }
//            .subscribe({
//                auth.saveUserToDB(it.user)
//                UserPreference.accessAllowed = it.user.is_camera_authorized ?: false
//                if (!UserPreference.accessAllowed) {
//                    if (it.user.is_banned == true || it.user.is_suspended == true || it.user.is_camera_rejected == true) {
//                        view.logOut()
//                    }
//                    view.displaySignUpPage()
//                } else if (!checkPermissions()) {
//                    println("PERMISSIONS DENIED")
//                    view.beginPermissionsFlow()
//                }else if (checkPermissions()){
//                    println("PERMISSIONS ALLOWED")
//                    view.disregardPermissions()
//                }
//            }, {
//
//            })
//
//    }

}