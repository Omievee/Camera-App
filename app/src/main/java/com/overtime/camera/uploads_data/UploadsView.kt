package com.overtime.camera.uploads_data

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.overtime.camera.R
import com.overtime.camera.model.SavedVideo

class UploadsView(context: Context?, attrbs: AttributeSet?=null) : ConstraintLayout(context, attrbs) {


    init {
            View.inflate(context, R.layout.upload_item_view, this)
    }


    fun bind(video:SavedVideo?) {

    }
}