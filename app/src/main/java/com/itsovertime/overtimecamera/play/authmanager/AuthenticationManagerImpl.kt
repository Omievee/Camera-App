package com.itsovertime.overtimecamera.play.authmanager

import com.itsovertime.overtimecamera.play.application.OTApplication
import com.itsovertime.overtimecamera.play.network.*
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class AuthenticationManagerImpl(
    val context: OTApplication,
    val api: Api
) : AuthenticationManager {
    override fun onVerifyAccessCodeRecieved(code: String): Single<AccessResponse> {
        val request = VerifyAccessCodeRequest(phone = num, code = code)
        return api
            .verifyAccessCode(request)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    }

    var num: String = ""
    override fun onRequestAccessCodeForNumber(number: String): Single<LoginResponse> {
        num = if (number.startsWith("1")) {
            "+$number"
        } else {
            "+1$number"
        }
        return api
            .verifyNumberForAccessCode(VerifyNumberRequest(phone = num))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

}