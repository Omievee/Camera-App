package com.itsovertime.overtimecamera.play.userpreference

import android.content.Context
import android.content.SharedPreferences
import com.itsovertime.overtimecamera.play.utils.Constants

object UserPreference {
    private lateinit var sPrefs: SharedPreferences

    fun load(context: Context) {
        sPrefs = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE)
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