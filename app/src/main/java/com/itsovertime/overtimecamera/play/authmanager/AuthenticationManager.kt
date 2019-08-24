package com.itsovertime.overtimecamera.play.authmanager

import com.itsovertime.overtimecamera.play.network.AccessResponse
import com.itsovertime.overtimecamera.play.network.LoginResponse
import io.reactivex.Single

interface AuthenticationManager {

    fun onRequestAccessCodeForNumber(number: String): Single<LoginResponse>
    fun onVerifyAccessCodeRecieved(code: String): Single<AccessResponse>
}