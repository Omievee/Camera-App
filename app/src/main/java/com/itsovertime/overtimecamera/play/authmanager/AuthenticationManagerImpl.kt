package com.itsovertime.overtimecamera.play.authmanager

import android.annotation.SuppressLint
import com.itsovertime.overtimecamera.play.BuildConfig
import com.itsovertime.overtimecamera.play.application.OTApplication
import com.itsovertime.overtimecamera.play.db.AppDatabase
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.User
import com.itsovertime.overtimecamera.play.network.*
import com.itsovertime.overtimecamera.play.uploads.UploadState
import com.itsovertime.overtimecamera.play.userpreference.UserPreference
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*

class AuthenticationManagerImpl(
    val context: OTApplication,
    val api: Api
) : AuthenticationManager {

    override fun onUserAgreedToTOS(): Single<TOSResponse> {

        return api
            .acceptToTOS(UserPreference.userId, BuildConfig.APPLICATION_ID)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    override fun getFullUser(): Single<ApplicationResponse> {
        return api
            .getUser(UserPreference.userId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    }

    var db = AppDatabase.getAppDataBase(context = context)
    @SuppressLint("CheckResult")
    override fun saveUserToDB(user: User) {
        Observable.fromCallable {

            val userDao = db?.userDao()
            with(userDao) {
                this?.saveUserData(user)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally {
            }
            .onErrorReturn {
                it.printStackTrace()
            }
            .subscribe({
            }, {
                it.printStackTrace()
            })
    }

    override fun onRefreshAuth(): Single<RestrictionsResponse> {
        return api
            .validateTokenCheckRestrictions()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    }

    var userName: String? = ""
    override fun submitApplication(name: String, city: String): Single<ApplicationResponse> {
        userName = name
        val request =
            ApplicationRequest(
                is_camera_requested = true,
                name = name,
                username = userName?.length?.let { getRandomString(it) },
                location = city
            )
        return api
            .submitApplication(UserPreference.userId, request)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    private fun getRandomString(sizeOfRandomString: Int): String {
        val ALLOWED_CHARACTERS = "${userName} 0123456789qwertyuiopasdfghjklzxcvbnm"
        val random = Random()
        val sb = StringBuilder(sizeOfRandomString)
        for (i in 0 until sizeOfRandomString)
            sb.append(ALLOWED_CHARACTERS[random.nextInt(ALLOWED_CHARACTERS.length)])
        return sb.toString()
    }

    override fun onrResendAccessCode(code: String): Single<AccessResponse> {
        val request = VerifyAccessCodeRequest(phone = num, code = code)
        return api
            .resendAccessCode(request)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

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