package com.itsovertime.overtimecamera.play.splashscreen

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.Transformation
import androidx.constraintlayout.widget.ConstraintLayout
import com.itsovertime.overtimecamera.play.BuildConfig
import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.baseactivity.BaseActivity
import com.itsovertime.overtimecamera.play.baseactivity.OTActivity
import com.itsovertime.overtimecamera.play.userpreference.UserPreference
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_splash.*
import javax.inject.Inject

class SplashActivity : OTActivity(), SplashImpl {
    override fun displayNetworkError() {
        expand(noNetwork)
        noNetwork.visibility = View.VISIBLE
    }

    override fun authSuccessful() {

        Handler().postDelayed({
            startActivity(Intent(this, BaseActivity::class.java).putExtra("logIn", true))
            finish()
        }, SPLASH_TIME_OUT.toLong())

    }

    override fun logOut() {
        println("UN-SUCCESSFUL AUTH:: ")
        startActivity(Intent(this, BaseActivity::class.java).putExtra("logIn", false))
        finish()
    }


    @Inject
    lateinit var presenter: SplashPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        window.apply {
            setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }

        setContentView(R.layout.activity_splash)
        version.text = "Version: ${BuildConfig.VERSION_NAME}"
        presenter.refreshAuth()
    }

    companion object {
        // Splash screen timer
        private const val SPLASH_TIME_OUT = 1500
    }


    override fun onResume() {
        super.onResume()
        presenter.refreshAuth()
    }

    fun expand(v: View) {
        v.measure(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        val targetHeight = v.measuredHeight

        v.layoutParams.height = 1
        v.visibility = View.VISIBLE

        val animate = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                v.layoutParams.height =
                    if (interpolatedTime == 1f) ConstraintLayout.LayoutParams.WRAP_CONTENT else (targetHeight * interpolatedTime).toInt()
                v.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

        animate.duration = 500
        v.startAnimation(animate)
    }

    fun collapse(v: View) {
        val initialHeight = v.measuredHeight
        val a = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                if (interpolatedTime == 1f) {
                    v.visibility = View.GONE
                } else {
                    v.layoutParams.height =
                        initialHeight - (initialHeight * interpolatedTime).toInt()
                    v.requestLayout()
                }
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }
        a.duration = 500
        v.startAnimation(a)
    }

}
