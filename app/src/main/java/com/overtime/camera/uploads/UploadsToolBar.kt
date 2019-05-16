package com.overtime.camera.uploads

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.overtime.camera.R

class UploadsToolBar(context: Context, args: AttributeSet? = null) : ConstraintLayout(context, args) {
    init {
        View.inflate(context, R.layout.uploads_view_toolbar, this)
    }
}