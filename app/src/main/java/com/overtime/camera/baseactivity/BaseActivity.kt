package com.overtime.camera.baseactivity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.FragmentManager
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