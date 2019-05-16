package com.overtime.camera.settings

import android.content.Intent
import android.net.Uri
import android.text.Html

class SettingsPresenter(val view: SettingsFragment) {


    fun clickedContactUs() {
        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.type = "message/rfc822"
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("hello@itsovertime.com"))
        view.onContactUs(emailIntent)
    }

    fun clickedTerms() {
        val url = "https://www.itsovertime.com/terms"
        val urlIntent = Intent(Intent.ACTION_VIEW)
        urlIntent.data = Uri.parse(url)
        view.onTermsClicked(urlIntent)
    }

    fun clickedLogOut() {
        //TODO: Login / Logout logic
    }


}