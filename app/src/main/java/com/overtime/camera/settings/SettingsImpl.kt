package com.overtime.camera.settings

import android.content.Intent

interface SettingsImpl {

    fun onLogOut()
    fun onTermsClicked(urlIntent:Intent)
    fun onContactUs(emailIntent: Intent)

}