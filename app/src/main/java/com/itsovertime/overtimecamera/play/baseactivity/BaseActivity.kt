package com.itsovertime.overtimecamera.play.baseactivity

import android.Manifest
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Parcelable
import android.os.PowerManager
import android.text.Editable
import android.text.TextWatcher
import android.view.OrientationEventListener
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.FragmentTransaction
import androidx.viewpager.widget.ViewPager
import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.camera.CameraFragment
import com.itsovertime.overtimecamera.play.network.NetworkSchedulerService
import com.itsovertime.overtimecamera.play.onboarding.OnBoardingFragment
import com.itsovertime.overtimecamera.play.onboarding.OnboardingActivity
import com.itsovertime.overtimecamera.play.uploads.UploadsFragment
import com.itsovertime.overtimecamera.play.userpreference.UserPreference
import dagger.android.AndroidInjection
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.permissions_view.*
import kotlinx.android.synthetic.main.phone_verification.*
import javax.inject.Inject


class BaseActivity : OTActivity(), BaseActivityInt, CameraFragment.UploadsButtonClick,
    View.OnClickListener {

    override fun beginPermissionsFlow() {
        permissions.visibility = View.VISIBLE
    }

    var orientation: OrientationEventListener? = null
    private val permissionsCode = 0
    private val requiredAppPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    var accessCodeSent: Boolean = false

    override fun displaySignUpPage() {
        startActivityForResult(Intent(this, OnboardingActivity::class.java), 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        println("Result.... $requestCode, $resultCode, $data")
        presenter.onCreate()
    }

    override fun resetViews() {
        accessCodeSent = false
        enterNumber.text.clear()
        changeNum.visibility = View.GONE
        resend.visibility = View.GONE
        enter.text = getString(R.string.auth_enter_phone)
        descrip.text = getString(R.string.auth_message)
    }

    override fun displayEnterResponseView(number: String) {
        accessCodeSent = true
        enterNumber.text.clear()
        changeNum.visibility = View.VISIBLE
        enter.text = getString(R.string.auth_enter_access_code)
        descrip.text = "We sent an access code to $number"
        resend.visibility = View.VISIBLE
    }

    override fun displayErrorFromResponse() {
        showToast(getString(R.string.error))
    }

    fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun displayProgress() {
        progress.visibility = View.VISIBLE
    }

    override fun hideDisplayProgress() {
        progress.visibility = View.GONE
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.submit -> {
                if (accessCodeSent) {
                    submitAccessCode()
                } else {
                    submitNumberForCode()
                }
            }
            R.id.resend -> presenter.resendAccessCode()
            R.id.changeNum -> presenter.resetViews()
            R.id.allowPermissions -> presenter.checkPermissions()
        }
    }

    private fun submitAccessCode() {
        presenter.submitAccessCode(code = enterNumber.text.toString())
        enterNumber.text.clear()
    }

    private fun submitNumberForCode() {
        if (enterNumber.text.toString() == "" || enterNumber.text.toString().length < 10) {
            showToast(getString(R.string.auth_invalid_num))
        } else {
            presenter.submitClicked(enterNumber.text.toString())
        }
    }


    override fun onUploadsButtonClicked() {
        viewPager.currentItem = 1
    }

    override fun setUpAdapter() {
        permissions.visibility = View.GONE
        val viewPager = findViewById<ViewPager>(R.id.viewPager)
        viewPager.adapter = CustomViewPageAdapter(supportFragmentManager, true)
    }

    override fun displayDeniedPermissionsView() {

    }


    override fun displayPermissions() {
        presenter.onCreate()
    }

    override fun displayAlert() {
        requestPermissions(
            requiredAppPermissions,
            permissionsCode
        )
    }


    private var wakeLock: PowerManager.WakeLock? = null
    private fun keepScreenUnlocked() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "screen_on:tag")
    }

    @Inject
    lateinit var presenter: BaseActivityPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        submit.setOnClickListener(this)
        resend.setOnClickListener(this)
        changeNum.setOnClickListener(this)
        allowPermissions.setOnClickListener(this)
        when (intent?.extras?.get("logIn")) {
            true -> {
                if (UserPreference.isSignUpComplete) {
                    presenter.retrieveFullUser()
                } else displaySignUpPage()

            }
            else -> {
                phoneVerificationView.visibility = View.VISIBLE
            }
        }
        scheduleJob()
        //detectOrientation()
        keepScreenUnlocked()
    }


    private fun scheduleJob() {

        val myJob = JobInfo.Builder(0, ComponentName(this, NetworkSchedulerService::class.java))
            .setRequiresCharging(false)
            .setMinimumLatency(1000)
            .setOverrideDeadline(2000)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPersisted(true)
            .build()

        val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.schedule(myJob)
    }

    override fun onStart() {
        super.onStart()
        startService(Intent(this, NetworkSchedulerService::class.java))
    }

    override fun onStop() {
        stopService(Intent(this, NetworkSchedulerService::class.java))
        super.onStop()
    }


    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, NetworkSchedulerService::class.java))
        presenter.onDestroy()
    }


    override fun onResume() {
        super.onResume()
        orientation?.let {
            if (it.canDetectOrientation()) {
                it.enable()
            }
        }
        wakeLockAcquire()
    }

//    private fun detectOrientation() {
//        orientation = object : OrientationEventListener(this) {
//            override fun onOrientationChanged(orientation: Int) {
//                when (orientation) {
//                    0 -> {
//                        showWarnings()
//                    }
//                    180 -> {
//                        showWarnings()
//                    }
//                    90 -> {
//                        hideWarnings()
//                    }
//                    270 -> {
//                        hideWarnings()
//                    }
//                    else -> {
//
//                    }
//                }
//            }
//        }
//
//    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionsCode) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                presenter.setUpAdapter()
            } else {
                presenter.permissionsDenied()
            }
        }
    }

//    fun showWarnings() {
//        rotateView.visibility = View.VISIBLE
//        rotateWarning.visibility = View.VISIBLE
//        viewPager.visibility = View.GONE
//    }
//
//    fun hideWarnings() {
//        rotateView.visibility = View.GONE
//        rotateWarning.visibility = View.GONE
//        viewPager.visibility = View.VISIBLE
//    }


    override fun onPause() {
        super.onPause()
        orientation?.disable()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }

    }

    private fun wakeLockAcquire() {
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire(5 * 60 * 1000L /*5 minutes*/)
        }
    }

    override fun onBackPressed() {
        supportFragmentManager.fragments.forEach {
            when (it) {
                is UploadsFragment -> {
                    println("uploads..... ${it.isVisible}")
                    if (it.childFragmentManager.backStackEntryCount > 0) {
                        it.childFragmentManager.popBackStack()
                    } else if (it.childFragmentManager.backStackEntryCount == 0 && viewPager.currentItem == 1) {
                        viewPager.currentItem = 0
                    }
                }
                is CameraFragment -> {
                    println("camera..... ${it.isVisible}")
                    if (it.childFragmentManager.backStackEntryCount > 0) {
                        it.childFragmentManager.popBackStack()
                    }
                    if (CameraFragment().fragmentIsVisibleToUser ?: return && it.childFragmentManager.backStackEntryCount == 0) {
                        finishAffinity()
                    }
                }
            }
        }
    }

    override fun onAttachFragment(fragment: Fragment?) {
        super.onAttachFragment(fragment)
        if (fragment is CameraFragment) {
            fragment.setUploadsClickListener(this)
        }
    }

    inner class CustomTextWatcher() : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }


}

class CustomViewPageAdapter(
    fragmentManager: FragmentManager,
    private val isMainViewPager: Boolean
) : FragmentPagerAdapter(fragmentManager) {
    var data = listOf(
        OnboardData(
            header = R.string.onboard_become_member,
            body = R.string.onboard_message1,
            buttonText = R.string.onboard_button_apply,
            displayEditTextFields = false,
            displayBottomText = true,
            bottomText = R.string.onboard_already_member
        ),
        OnboardData(
            header = R.string.onbarod_shooters,
            body = R.string.onboard_message2,
            buttonText = R.string.onbaord_button_submit,
            displayEditTextFields = true
        ),
        OnboardData(
            header = R.string.onboard_thank_you,
            body = R.string.onboard_message3,
            buttonText = R.string.onboard_status,
            displayEditTextFields = false,
            displayBottomText = true,
            bottomText = R.string.onboard_stay_tuned
        )
    )
    private var TABS: Int = 0

    override fun getCount(): Int {
        TABS = when (isMainViewPager) {
            true -> 2
            else -> data.size
        }
        return TABS
    }

    //Onboarding viewpager while false
    override fun getItem(position: Int): Fragment? {
        return when (isMainViewPager) {
            true -> {
                when (position) {
                    0 -> CameraFragment()
                    1 -> UploadsFragment()
                    else -> null
                }
            }
            false -> {
                println("false vewpager")
                OnBoardingFragment.newInstance(data[position])
            }
        }
    }
}

@Parcelize
class OnboardData(
    @StringRes val header: Int,
    @StringRes val body: Int,
    @StringRes val buttonText: Int,
    val displayEditTextFields: Boolean = false,
    val displayBottomText: Boolean = false,
    val bottomText: Int? = null
) : Parcelable


