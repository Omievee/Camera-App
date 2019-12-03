package com.itsovertime.overtimecamera.play.devid_id

import android.content.Context
import android.provider.Settings

object DeviceId {
    fun getID(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }
}