package com.overtime.camera.baseactivity

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.OrientationEventListener
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.overtime.camera.R
import com.overtime.camera.camera.CameraFragment
import com.overtime.camera.uploads.UploadsFragment


class BaseActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val viewPager = findViewById<ViewPager>(R.id.mainViewPager)
        viewPager.adapter = CustomPageAdapter(supportFragmentManager)
    }

    override fun onResume() {
        super.onResume()
        detectOrientation()
    }

    private fun detectOrientation() {
        val mOrientationListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == 0 || orientation == 180) {
                    Toast.makeText(
                        applicationContext, "portrait",
                        Toast.LENGTH_LONG
                    ).show()
                } else if (orientation == 90 || orientation == 270) {
                    Toast.makeText(
                        applicationContext, "landscape",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        if (mOrientationListener.canDetectOrientation()) {
            mOrientationListener.enable()
        }
    }
}

class CustomPageAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager) {
    override fun getCount(): Int {
        return TABS
    }

    override fun getItem(position: Int): Fragment? {
        return when (position) {
            0 -> CameraFragment.newInstance("", "")
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