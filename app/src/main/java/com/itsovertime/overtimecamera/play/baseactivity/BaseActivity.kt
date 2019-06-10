package com.itsovertime.overtimecamera.play.baseactivity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.OrientationEventListener
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.camera.CameraFragment
import com.itsovertime.overtimecamera.play.uploads.UploadsFragment
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_uploads.*
import javax.inject.Inject

class BaseActivity : OTActivity(), BaseActivityInt, CameraFragment.UploadsButtonClick {
    override fun onUploadsButtonClicked() {
        viewPager.currentItem = 1
    }


    override fun setUpAdapter() {
        val viewPager = findViewById<ViewPager>(R.id.viewPager)
        viewPager.adapter = CustomPageAdapter(supportFragmentManager)
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
        AlertDialog.Builder(this, R.style.CUSTOM_ALERT)
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


    @Inject
    lateinit var presenter: BaseActivityPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidInjection.inject(this)
        setContentView(R.layout.activity_main)

        presenter.onCreate()
        // detectOrientation()
    }


    override fun onResume() {
        super.onResume()
        orientation?.let {
            if (it.canDetectOrientation()) {
                it.enable()
            }
        }
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
        super.onPause()
        orientation?.disable()
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

class CustomPageAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager) {
    override fun getCount(): Int {
        return TABS
    }

    override fun getItem(position: Int): Fragment? {
        return when (position) {
            0 -> {
                CameraFragment()

            }
            1 -> UploadsFragment.newInstance("", "")
            else -> null
        }
    }

    //    override fun getPageTitle(position: Int): CharSequence? {
//        return "Page $position"
////    }
    companion object {
        private const val TABS = 2
    }

}