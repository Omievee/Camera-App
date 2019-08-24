package com.itsovertime.overtimecamera.play.authmanager

import com.itsovertime.overtimecamera.play.application.OTApplication
import com.itsovertime.overtimecamera.play.network.Api
import com.itsovertime.overtimecamera.play.network.LoginResponse
import com.itsovertime.overtimecamera.play.network.VerifyNumberRequest
import com.itsovertime.overtimecamera.play.network.VerifyNumberResponse
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class AuthenticationManagerImpl(
    val context: OTApplication,
    val api: Api
) : AuthenticationManager {
    override fun onRequestAccessCodeForNumber(number: String): Single<LoginResponse> {
        return api
            .verifyNumberForAccessCode(VerifyNumberRequest(phone = "+1$number"))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())


    }

}