package com.itsovertime.overtimecamera.play.baseactivity

import android.Manifest
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PowerManager
import android.view.OrientationEventListener
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.camera.CameraFragment
import com.itsovertime.overtimecamera.play.network.NetworkSchedulerService
import com.itsovertime.overtimecamera.play.uploads.UploadsFragment
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject


class BaseActivity : OTActivity(), BaseActivityInt, CameraFragment.UploadsButtonClick {


    override fun onUploadsButtonClicked() {
        viewPager.currentItem = 1
    }


    override fun setUpAdapter() {
        val viewPager = findViewById<ViewPager>(R.id.viewPager)
        viewPager.adapter = CustomViewPageAdapter(supportFragmentManager, true)
    }

    override fun displayDeniedPermissionsView() {

    }

    var orientation: OrientationEventListener? = null
    val PERMISSIONS_CODE = 0
    private val APP_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )


    override fun displayPermissions() {

    }

    override fun displayAlert() {
        AlertDialog.Builder(this, com.itsovertime.overtimecamera.play.R.style.CUSTOM_ALERT)
            .setTitle("Permissions Request")
            .setMessage("Allow overtimecamera..")
            .setPositiveButton("Continue") { _, _ ->
                displaySystemPermissionsDialog()
            }
            .setNegativeButton("Not Now") { _, _ ->
                presenter.permissionsDenied()
            }
            .setCancelable(false)
            .show()
    }

    private fun displaySystemPermissionsDialog() {
        requestPermissions(
            APP_PERMISSIONS,
            PERMISSIONS_CODE
        )
    }


    private var wl: PowerManager.WakeLock? = null
    private fun keepScreenUnlocked() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "screen_on:tag")

    }

    @Inject
    lateinit var presenter: BaseActivityPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidInjection.inject(this)
        setContentView(R.layout.activity_main)
        presenter.onCreate()
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
        wl?.release()
        super.onDestroy()
    }


    override fun onResume() {
        super.onResume()
        orientation?.let {
            if (it.canDetectOrientation()) {
                it.enable()
            }
        }
        wl?.acquire(5 * 60 * 1000L /*5 minutes*/)
//        if (viewPager.currentItem == 1) {
//            viewPager.currentItem = 0
//        }
    }

    private fun detectOrientation() {
        orientation = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                when (orientation) {
                    0 -> {
                        showWarnings()
                    }
                    180 -> {
                        showWarnings()
                    }
                    90 -> {
                        hideWarnings()
                    }
                    270 -> {
                        hideWarnings()
                    }
                    else -> {

                    }
                }
            }
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                presenter.setUpAdapter()
            } else {
                presenter.permissionsDenied()
            }
        }
    }

    fun showWarnings() {
        rotateView.visibility = View.VISIBLE
        rotateWarning.visibility = View.VISIBLE
        viewPager.visibility = View.GONE
    }

    fun hideWarnings() {
        rotateView.visibility = View.GONE
        rotateWarning.visibility = View.GONE
        viewPager.visibility = View.VISIBLE
    }

    override fun onPause() {
        orientation?.disable()
        wl?.release()
        super.onPause()

    }

    override fun onBackPressed() {
        supportFragmentManager.fragments.forEach {
            when (it) {
                is UploadsFragment -> {
                    if (it.childFragmentManager.backStackEntryCount > 0) {
                        it.childFragmentManager.popBackStack()
                    } else if (it.childFragmentManager.backStackEntryCount == 0 && viewPager.currentItem == 1) {
                        viewPager.currentItem = 0
                    } else {
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

class CustomViewPageAdapter(fragmentManager: FragmentManager, private val isMainViewPager: Boolean) :
    FragmentPagerAdapter(fragmentManager) {
    private var TABS: Int = 0
    override fun getCount(): Int {
        TABS = when (isMainViewPager) {
            true -> 2
            false -> 4
        }
        return TABS
    }

    //Onboarding viewpager while false
    override fun getItem(position: Int): Fragment? {
        return when (isMainViewPager) {
            true -> {
                when (position) {
                    0 -> CameraFragment()
                    1 -> UploadsFragment.newInstance("", "")
                    else -> null
                }
            }
            false -> {
                null
            }
        }
    }

    //    override fun getPageTitle(position: Int): CharSequence? {
//        return "Page $position"
////    }

}
