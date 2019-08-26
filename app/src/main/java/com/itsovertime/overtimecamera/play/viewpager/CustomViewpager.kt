package com.itsovertime.overtimecamera.play.viewpager

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

class CustomViewpager(context: Context, attrs: AttributeSet? = null) :
    ViewPager(context, attrs) {

    var canSwipe: Boolean = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        return if (canSwipe) {
            super.onTouchEvent(ev)
        } else {
            false
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return if (this.canSwipe) {
            super.onInterceptTouchEvent(event)
        } else false

    }
}