package com.itsovertime.overtimecamera.play.camera

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.itsovertime.overtimecamera.play.R
import kotlinx.android.synthetic.main.event_view.view.*

class EventPickVIew(context: Context, attributeSet: AttributeSet? = null) : ConstraintLayout(context, attributeSet) {

    init {
        View.inflate(context, R.layout.event_view, this)
    }

}