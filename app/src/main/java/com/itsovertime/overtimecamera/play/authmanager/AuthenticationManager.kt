package com.itsovertime.overtimecamera.play.authmanager

import com.itsovertime.overtimecamera.play.model.User
import com.itsovertime.overtimecamera.play.network.AccessResponse
import com.itsovertime.overtimecamera.play.network.ApplicationResponse
import com.itsovertime.overtimecamera.play.network.LoginResponse
import com.itsovertime.overtimecamera.play.network.RestrictionsResponse
import io.reactivex.Single

interface AuthenticationManager {

    fun onRequestAccessCodeForNumber(number: String): Single<LoginResponse>
    fun onVerifyAccessCodeRecieved(code: String): Single<AccessResponse>
    fun onrResendAccessCode(code: String): Single<AccessResponse>
    fun submitApplication(name: String, city: String): Single<ApplicationResponse>
    fun onRefreshAuth(): Single<RestrictionsResponse>
    fun saveUserToDB(user: User)
}