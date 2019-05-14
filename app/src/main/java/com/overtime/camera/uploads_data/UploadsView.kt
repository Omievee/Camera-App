package com.overtime.camera.uploads_data

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.overtime.camera.R
import com.overtime.camera.model.SavedVideo
import kotlinx.android.synthetic.main.upload_item_view.view.*
import java.io.File

class UploadsView(context: Context?, attrbs: AttributeSet? = null) : ConstraintLayout(context, attrbs) {


    init {
        View.inflate(context, R.layout.upload_item_view, this)
    }


    fun bind(video: SavedVideo?) {

        if(video?.isFavorite!!){

        }


        val uri = Uri.fromFile(File(video.vidPath))
        val request = ImageRequestBuilder
            .newBuilderWithSource(uri)
            .setLowestPermittedRequestLevel(ImageRequest.RequestLevel.FULL_FETCH)
            .setProgressiveRenderingEnabled(false)
            .build()

        val controller = Fresco.newDraweeControllerBuilder()
            .setUri(uri)
            .setImageRequest(request)
            .build()


        thumbNail.controller = controller

    }
}