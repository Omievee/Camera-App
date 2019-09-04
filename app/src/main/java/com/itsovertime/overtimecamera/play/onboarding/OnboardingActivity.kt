package com.itsovertime.overtimecamera.play.onboarding

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
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

class OnboardingActivity : OTActivity(), OnBoardingFragment.NextPageClick, View.OnClickListener {
    var clickedMore: Boolean = false
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.accept -> {
                when (clickedMore) {
                    true -> {
                        progress.visibility = View.VISIBLE
                        userAgreedToTOS()
                    }
                    false -> {
                        deny.visibility = View.VISIBLE
                        webview.flingScroll(0, 17500)
                        clickedMore = true
                        accept.text = getString(R.string.auth_tos_accept)
                    }
                }
            }
            R.id.deny -> {
                finishAffinity()
            }
            R.id.resend ->{

            }
        }
    }

    var agreeDisp: Disposable? = null
    private fun userAgreedToTOS() {
        agreeDisp?.dispose()
        agreeDisp =
            auth
                .onUserAgreedToTOS()
                .doFinally {
                    progress.visibility = View.GONE
                }
                .doOnSuccess {
                    tos.visibility = View.GONE
                }
                .doOnError {
                    makeToast(getString(R.string.auth_try_again))
                    it.printStackTrace()
                }
                .subscribe({

                }, {

                })
    }


    override fun checkStatus() {
        progress.visibility = View.VISIBLE
        userDisposable?.dispose()
        userDisposable = auth
            .getFullUser()
            .doOnError {
                it.printStackTrace()
            }
            .doFinally {
                progress.visibility = View.GONE
                if (UserPreference.isSignUpComplete) {
                    onboardinViewpager.currentItem = 2
                }
            }
            .subscribe({
                auth.saveUserToDB(it.user)
                if (it.user.camera_tos_agreed_at == null) {
                    displayTOS()
                }
                if (it.user.is_camera_authorized == false) {
                    println("auth camera : ${it.user.is_camera_authorized}")
                    makeToast(getString(R.string.auth_check_back_status))
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
            makeToast(getString(R.string.auth_all_fields_required))
        } else {
            progress.visibility = View.VISIBLE
            submitApplication(name ?: "", city ?: "")
        }
    }

    fun makeToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
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
                UserPreference.isSignUpComplete = true
                onboardinViewpager.currentItem = 2
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

        accept.setOnClickListener(this)
        deny.setOnClickListener(this)


        onboardinViewpager.adapter =
            CustomViewPageAdapter(supportFragmentManager, isMainViewPager = false)

    }

    override fun onResume() {
        super.onResume()
        checkStatus()

    }

    @SuppressLint("SetJavaScriptEnabled")
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



