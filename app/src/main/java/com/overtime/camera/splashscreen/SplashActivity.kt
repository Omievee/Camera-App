package com.overtime.camera.splashscreen

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.overtime.camera.R

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)


        launchActivity()
    }


    fun launchActivity() {

        //TODO: logic for checking user / login....
        Handler().postDelayed({


        }, SPLASH_TIME_OUT.toLong())

    }

    companion object {
        // Splash screen timer
        private val SPLASH_TIME_OUT = 1000
    }

}