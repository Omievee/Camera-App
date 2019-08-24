package com.itsovertime.overtimecamera.play.authmanager

import com.itsovertime.overtimecamera.play.network.LoginResponse
import com.itsovertime.overtimecamera.play.network.VerifyNumberResponse
import io.reactivex.Single

interface AuthenticationManager {

    fun onRequestAccessCodeForNumber(number: String): Single<LoginResponse>
}