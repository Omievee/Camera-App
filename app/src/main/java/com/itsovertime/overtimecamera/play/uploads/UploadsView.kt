package com.itsovertime.overtimecamera.play.uploads

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.model.SavedVideo
import kotlinx.android.synthetic.main.upload_item_view.view.*
import java.io.File

class UploadsView(context: Context?, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {


    init {
        View.inflate(context, R.layout.upload_item_view, this)
    }


    fun bind(video: SavedVideo) {
        faveIcon.visibility = when (video.isFavorite) {
            true -> View.VISIBLE
            else -> View.INVISIBLE
        }

        val uri = Uri.fromFile(File(video.vidPath))
        val request = ImageRequestBuilder
                .newBuilderWithSource(uri)
                .setLowestPermittedRequestLevel(ImageRequest.RequestLevel.FULL_FETCH)
                .setProgressiveRenderingEnabled(true)
                .build()

        val controller = Fresco.newDraweeControllerBuilder()
                .setUri(uri)
                .setImageRequest(request)
                .build()

        thumbNail.controller = controller
    }
}