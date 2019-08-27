package com.itsovertime.overtimecamera.play.onboarding

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.authmanager.AuthenticationManager
import com.itsovertime.overtimecamera.play.baseactivity.CustomViewPageAdapter
import com.itsovertime.overtimecamera.play.baseactivity.OTActivity
import com.itsovertime.overtimecamera.play.userpreference.UserPreference
import dagger.android.AndroidInjection
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_onboarding.*
import kotlinx.android.synthetic.main.fragment_tos.*
import javax.inject.Inject

class OnboardingActivity : OTActivity(), OnBoardingFragment.NextPageClick {
    override fun checkStatus() {
        progress.visibility = View.VISIBLE
        println("Retrieve....")
        userDisposable?.dispose()
        userDisposable = auth
            .getFullUser()
            .doOnError {
                it.printStackTrace()
            }
            .doFinally {
                progress.visibility = View.GONE
            }
            .subscribe({
                auth.saveUserToDB(it.user)
                if (it.user.is_camera_authorized != true) {
                    Toast.makeText(
                        this,
                        getString(R.string.auth_check_back_status),
                        Toast.LENGTH_SHORT
                    ).show()
                } else finish()
            }, {
            })
    }

    var userDisposable: Disposable? = null

    @Inject
    lateinit var auth: AuthenticationManager


    override fun onButtonClicked(name: String?, city: String?) {
        if (onboardinViewpager.currentItem == 0) {
            onboardinViewpager.currentItem = 1
        } else if (onboardinViewpager.currentItem == 1 && name.equals("") || city.equals("")) {
            Toast.makeText(this, getString(R.string.auth_all_fields_required), Toast.LENGTH_SHORT)
                .show()
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


        displayTOS()

        onboardinViewpager.adapter =
            CustomViewPageAdapter(supportFragmentManager, isMainViewPager = false)
        if (UserPreference.isSignUpComplete)
            onboardinViewpager.currentItem = 2
    }

    private fun displayTOS() {
        tos.visibility = View.VISIBLE

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
            } else {
                onboardinViewpager.currentItem = 0
            }
            supportFragmentManager.popBackStack()
        } else {
            finishAffinity()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        userDisposable?.dispose()
        submitDisposable?.dispose()
    }

}



