package com.itsovertime.overtimecamera.play.authmanager

import com.itsovertime.overtimecamera.play.model.User
import com.itsovertime.overtimecamera.play.network.*
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.Response

interface AuthenticationManager {

    fun onRequestAccessCodeForNumber(number: String): Single<LoginResponse>
    fun onVerifyAccessCodeRecieved(code: String): Single<AccessResponse>
    fun onrResendAccessCode(code: String): Single<AccessResponse>
    fun submitApplication(name: String, city: String): Single<ApplicationResponse>
    fun onRefreshAuth(): Observable<Response<RestrictionsResponse>>
    fun saveUserToDB(user: User)
    fun getFullUser(): Single<ApplicationResponse>
    fun onUserAgreedToTOS(): Single<TOSResponse>
    fun getUserId(): Single<User>?
    fun logOut()
}