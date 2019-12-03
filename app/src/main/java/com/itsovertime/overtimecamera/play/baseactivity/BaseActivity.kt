package com.itsovertime.overtimecamera.play.baseactivity

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.os.PowerManager
import android.provider.Settings
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.itsovertime.overtimecamera.play.BuildConfig
import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.camera.CameraFragment
import com.itsovertime.overtimecamera.play.network.NetworkSchedulerService
import com.itsovertime.overtimecamera.play.onboarding.OnBoardingFragment
import com.itsovertime.overtimecamera.play.onboarding.OnboardingActivity
import com.itsovertime.overtimecamera.play.userpreference.UserPreference
import com.mixpanel.android.mpmetrics.MixpanelAPI
import dagger.android.AndroidInjection
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.permissions_view.*
import kotlinx.android.synthetic.main.phone_verification.*
import javax.inject.Inject


class BaseActivity : OTActivity(), BaseActivityInt, CameraFragment.UploadsButtonClick,
    View.OnClickListener {

    override fun disregardPermissions() {
        phoneVerificationView.visibility = View.GONE
//        setUpAdapter()
    }

    override fun onRefreshFragmentFromDisconnect() {
        supportFragmentManager.beginTransaction()
            .detach(CameraFragment())
            .attach(CameraFragment())
            .commit();
    }


    override fun hideKeyboard() {
        val imm = this.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = currentFocus
        if (view == null) {
            view = View(this);
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun logOut() {
        viewPager.visibility = View.GONE
        viewPager.adapter = null
        //  showToast(getString(R.string.auth_logout_not_authorized))
        phoneVerificationView.visibility = View.VISIBLE
//        finishAffinity()
    }

    override fun beginPermissionsFlow() {
        permissions.visibility = View.VISIBLE
        viewPager.visibility = View.VISIBLE
    }


    private val permissionsCode = 0
    private val requiredAppPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
//        Manifest.permission.ACCESS_COARSE_LOCATION,
//        Manifest.permission.ACCESS_FINE_LOCATION
    )
    var accessCodeSent: Boolean = false
    override fun displaySignUpPage() {
        startActivityForResult(Intent(this, OnboardingActivity::class.java), 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == 0 && presenter.checkPermissions()) {
            presenter.setUpAdapter()
        } else presenter.displayPermission()
    }

    override fun resetViews() {
        accessCodeSent = false
        enterNumber.text.clear()
        changeNum.visibility = View.GONE
        resend.visibility = View.GONE
        enter.text = getString(R.string.auth_enter_phone)
        descrip.text = getString(R.string.auth_message)
    }

    var num: String? = ""
    override fun displayEnterResponseView(number: String) {

        accessCodeSent = true
        enterNumber.text.clear()
        changeNum.visibility = View.VISIBLE
        changeNum.apply {
            alpha = .6F
        }
        enter.text = getString(R.string.auth_enter_access_code)

        val span = SpannableStringBuilder()
        span.append("We sent an access code to:  ")
        span.append(number.color)

        descrip.text = span

        resend.visibility = View.VISIBLE
    }

    private val String.color: SpannableString
        get() {
            val spanstr = SpannableString(this)
            spanstr.setSpan(
                ForegroundColorSpan(resources.getColor(R.color.OT_Orange)),
                0,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            return spanstr
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
            R.id.resend -> {
                presenter.resendAccessCode()
            }
            R.id.changeNum -> presenter.resetViews()
            R.id.allowPermissions -> if (!presenter.checkPermissions()) {
                displayAlert()
            } else {
                setUpAdapter()
            }
        }
    }

    private fun submitAccessCode() {
        if (enterNumber.text.toString() == "" || enterNumber.text.toString().length < 3) {
            showToast(getString(R.string.auth_invalid_access_code))
        } else {
            presenter.submitAccessCode(code = enterNumber.text.toString())
            enterNumber.text.clear()
        }

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
        println("setting up adapter..")
        permissions.visibility = View.GONE
        phoneVerificationView.visibility = View.GONE
        viewPager.visibility = View.VISIBLE
        viewPager.adapter = CustomViewPageAdapter(supportFragmentManager, true)
    }

    override fun displayAlert() {
        requestPermissions(
            requiredAppPermissions,
            permissionsCode
        )
    }


    @Inject
    lateinit var presenter: BaseActivityPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        presenter.onCreate()
        submit.setOnClickListener(this)
        resend.setOnClickListener(this)
        changeNum.setOnClickListener(this)
        allowPermissions.setOnClickListener(this)


        window.apply {
            setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        when (intent?.extras?.get("logIn")) {
            true -> {
                if (UserPreference.accessAllowed) {
                    presenter.setUpAdapter()
                } else {
                    displaySignUpPage()
                }
            }
            else -> {
                logOut()
            }
        }

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val currentStatus = powerManager

        scheduleJob()
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
        UserPreference.isChecked = false
        stopService(Intent(this, NetworkSchedulerService::class.java))
        super.onStop()
    }


    override fun onDestroy() {
        UserPreference.isChecked = false
        stopService(Intent(this, NetworkSchedulerService::class.java))
        presenter.onDestroy()

        super.onDestroy()

    }


    override fun onResume() {
        super.onResume()
        presenter.retrieveFullUser()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionsCode) {
            grantResults.forEachIndexed { index, i ->
                val permission = permissions[index]
                if (i == PackageManager.PERMISSION_GRANTED) {
                    println("PERMISSION GRANDTED")
                    setUpAdapter()
                } else if (i == PackageManager.PERMISSION_DENIED) {
                    val rational = shouldShowRequestPermissionRationale(permission)
                    if (!rational) {
                        val uri = Uri.fromParts("package", packageName, null);
                        AlertDialog.Builder(this, R.style.CUSTOM_ALERT).apply {
                            setTitle("Please enable all permissions to continue")
                            setCancelable(false)
                        }.setPositiveButton("Ok") { _, _ ->
                            startActivityForResult(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = uri
                                },
                                0
                            )
                        }.show()
                    }
                    presenter.permissionsDenied()
                }
            }
        }
    }

    override fun onBackPressed() {
        supportFragmentManager.fragments.forEach {
            when (it) {
                is CameraFragment -> {
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
            true -> {
                1
            }
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
                    else -> null
                }
            }
            false -> {
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


