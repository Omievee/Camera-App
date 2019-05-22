package com.itsovertime.overtimecamera.play.settings

import android.content.Intent

interface SettingsImpl {

    fun onLogOut()
    fun onTermsClicked(urlIntent:Intent)
    fun onContactUs(emailIntent: Intent)

}