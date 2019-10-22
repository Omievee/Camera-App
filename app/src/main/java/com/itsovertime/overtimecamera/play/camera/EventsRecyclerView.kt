package com.itsovertime.overtimecamera.play.camera

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.itsovertime.overtimecamera.play.R

class EventsRecyclerView(context: Context, attributeSet: AttributeSet? = null) :
    ConstraintLayout(context, attributeSet) {

    init {

        View.inflate(context, R.layout.events_recycler_view, this)
    }
}