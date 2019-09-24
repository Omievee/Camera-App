package com.itsovertime.overtimecamera.play.baseactivity

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.itsovertime.overtimecamera.play.R

class RotatWarningView(context: Context, attributeSet: AttributeSet? = null) :
    ConstraintLayout(context, attributeSet) {

    init {
        View.inflate(context, R.layout.rotate_warning_view, this)
    }
}