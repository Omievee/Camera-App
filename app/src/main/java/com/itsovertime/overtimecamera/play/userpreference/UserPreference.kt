package com.itsovertime.overtimecamera.play.userpreference

import android.content.Context
import android.content.SharedPreferences
import com.itsovertime.overtimecamera.play.model.User
import com.itsovertime.overtimecamera.play.utils.Constants
import com.squareup.moshi.Moshi

object UserPreference {
    private lateinit var sPrefs: SharedPreferences

    fun load(context: Context) {
        sPrefs = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE)
    }




    var accessAllowed: Boolean
        get() {
            return sPrefs.getBoolean(Constants.ACCESS, false)
        }
        set(value) {
            sPrefs.edit().putBoolean(Constants.ACCESS, value).apply()
        }


    var isSignUpComplete: Boolean
        get() {
            return sPrefs.getBoolean(Constants.COMPLETE, false)
        }
        set(value) {
            sPrefs.edit().putBoolean(Constants.COMPLETE, value).apply()
        }

    var userId: String
        get() {
            return sPrefs.getString(Constants.ID, "") ?: ""
        }
        set(value) {
            sPrefs.edit()
                .putString(Constants.ID, value).apply()
        }
    var authToken: String
        get() {
            return sPrefs.getString(Constants.TOKEN, "") ?: ""
        }
        set(token) {
            sPrefs.edit()
                .putString(Constants.TOKEN, token).apply()
        }


    var loggedIn: String
        get() {
            return sPrefs.getString(Constants.USER, "") ?: ""
        }
        set(value) {
            sPrefs.edit()
                .putString(Constants.USER, value).apply()
        }

}