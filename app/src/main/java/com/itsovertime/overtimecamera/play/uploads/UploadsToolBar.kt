package com.itsovertime.overtimecamera.play.uploads

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.itsovertime.overtimecamera.play.R

class UploadsToolBar(context: Context, args: AttributeSet? = null) : ConstraintLayout(context, args) {

    init {
        View.inflate(context, R.layout.uploads_view_toolbar, this)
    }
}