package com.itsovertime.overtimecamera.play.splashscreen

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.itsovertime.overtimecamera.play.BuildConfig
import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.baseactivity.BaseActivity
import com.itsovertime.overtimecamera.play.userpreference.UserPreference
import kotlinx.android.synthetic.main.activity_splash.*

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)


        version.text = "Version: ${BuildConfig.VERSION_NAME}"
        launchActivity()
    }

    private fun launchActivity() {
        Handler().postDelayed({
            when (UserPreference.authToken) {
                "" -> {
                    startActivity(Intent(this, BaseActivity::class.java).putExtra("logIn", false))
                    finish()
                }
                else -> {
                    startActivity(Intent(this, BaseActivity::class.java).putExtra("logIn", true))
                    finish()
                }
            }


        }, SPLASH_TIME_OUT.toLong())

    }

    companion object {
        // Splash screen timer
        private const val SPLASH_TIME_OUT = 1500
    }

}
