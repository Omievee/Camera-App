package com.itsovertime.overtimecamera.play.progressbar

import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.ProgressBar
import androidx.databinding.adapters.SeekBarBindingAdapter.setProgress


class ProgressBarAnimation(private val progressBar: ProgressBar, val from: Int, private val to: Int) : Animation() {

    override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
        super.applyTransformation(interpolatedTime, t)
        val value = from + (to - from) * interpolatedTime
        progressBar.progress = value.toInt()
    }
}