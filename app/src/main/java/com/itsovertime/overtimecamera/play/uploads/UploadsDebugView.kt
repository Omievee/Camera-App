package com.itsovertime.overtimecamera.play.uploads

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.model.SavedVideo
import kotlinx.android.synthetic.main.upload_item_view.view.*
import java.io.File

class UploadsDebugView(context: Context, attributeSet: AttributeSet? = null) :
    ConstraintLayout(context, attributeSet) {

    init {
        View.inflate(context, R.layout.upload_debug_view, this)
    }

    fun bind(savedVideo: SavedVideo) {

        Glide.with(context)
            .load(File(savedVideo.highRes).path)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(thumbNail)

    }
}