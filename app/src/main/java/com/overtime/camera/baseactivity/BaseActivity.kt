package com.overtime.camera.baseactivity

import android.content.Context
import android.os.Bundle
import android.view.OrientationEventListener
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.overtime.camera.R
import com.overtime.camera.camera.CameraFragment
import com.overtime.camera.uploads.UploadsFragment
import kotlinx.android.synthetic.main.activity_main.*


class BaseActivity : AppCompatActivity() {

    var orientation: OrientationEventListener? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val viewPager = findViewById<ViewPager>(R.id.mainViewPager)
        viewPager.adapter = CustomPageAdapter(supportFragmentManager)

        detectOrientation()
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

    fun showWarnings() {
        rotateView.visibility = View.VISIBLE
        rotateWarning.visibility = View.VISIBLE
        mainViewPager.visibility = View.GONE
    }

    fun hideWarnings() {
        rotateView.visibility = View.GONE
        rotateWarning.visibility = View.GONE
        mainViewPager.visibility = View.VISIBLE
    }

    override fun onPause() {
        super.onPause()
        orientation?.disable()
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