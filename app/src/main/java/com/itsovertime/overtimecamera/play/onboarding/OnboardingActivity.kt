package com.itsovertime.overtimecamera.play.onboarding

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.authmanager.AuthenticationManager
import com.itsovertime.overtimecamera.play.baseactivity.CustomViewPageAdapter
import com.itsovertime.overtimecamera.play.baseactivity.OTActivity
import com.itsovertime.overtimecamera.play.camera.CameraFragment
import com.itsovertime.overtimecamera.play.network.Api
import com.itsovertime.overtimecamera.play.userpreference.UserPreference
import dagger.android.AndroidInjection
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_onboarding.*
import kotlinx.android.synthetic.main.fragment_uploads.*
import javax.inject.Inject

class OnboardingActivity : OTActivity(), OnBoardingFragment.NextPageClick {


    @Inject
    lateinit var auth: AuthenticationManager


    override fun onButtonClicked(name: String?, city: String?) {
        if (onboardinViewpager.currentItem == 0) {
            onboardinViewpager.currentItem = 1
        } else if (onboardinViewpager.currentItem == 1 && name.equals("") || city.equals("")) {
            Toast.makeText(this, "All Fields Required", Toast.LENGTH_SHORT).show()
        } else {
            progress.visibility = View.VISIBLE
            submitApplication(name ?: "", city ?: "")
        }
    }

    var submitDisposable: Disposable? = null
    private fun submitApplication(name: String, city: String) {
        submitDisposable?.dispose()
        submitDisposable = auth
            .submitApplication(name, city)
            .doFinally {
                progress.visibility = View.GONE
            }
            .doOnSuccess {
                auth.saveUserToDB(it.user)
                onboardinViewpager.currentItem = 2
                UserPreference.isSignUpComplete = true
            }
            .doOnError {
                println("throwable... ${it.message}")
            }
            .subscribe({

            }, {

            })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)


        onboardinViewpager.adapter =
            CustomViewPageAdapter(supportFragmentManager, isMainViewPager = false)
        if (UserPreference.isSignUpComplete)
            onboardinViewpager.currentItem = 2

    }


    override fun onAttachFragment(fragment: Fragment?) {
        super.onAttachFragment(fragment)
        if (fragment is OnBoardingFragment) {
            fragment.setUploadsClickListener(this)
        }
    }


    override fun onBackPressed() {
        if (!UserPreference.isSignUpComplete) {
            if (onboardinViewpager.currentItem == 2) {
                onboardinViewpager.currentItem = 1
                OnBoardingFragment().finalPage = false
            } else {
                onboardinViewpager.currentItem = 0
            }
        } else {
            finishAffinity()
        }
    }

}



