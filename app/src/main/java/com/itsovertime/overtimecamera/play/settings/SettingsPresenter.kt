package com.itsovertime.overtimecamera.play.settings

import android.content.Intent
import android.net.Uri
import com.itsovertime.overtimecamera.play.authmanager.AuthenticationManager

class SettingsPresenter(val view: SettingsFragment, val auth: AuthenticationManager) {


    fun clickedContactUs() {
        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.type = "message/rfc822"
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("hi@itsovertime.com"))
        view.onContactUs(emailIntent)
    }

    fun clickedTerms() {
        val url = "https://www.itsovertime.com/terms"
        val urlIntent = Intent(Intent.ACTION_VIEW)
        urlIntent.data = Uri.parse(url)
        view.onTermsClicked(urlIntent)
    }

    fun clickedLogOut() {
        auth.logOut()
        view.onLogOut()
    }


}