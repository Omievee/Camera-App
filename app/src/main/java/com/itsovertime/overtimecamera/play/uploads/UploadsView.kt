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
import com.itsovertime.overtimecamera.play.model.UploadState
import com.itsovertime.overtimecamera.play.progressbar.ProgressBarAnimation
import kotlinx.android.synthetic.main.upload_item_view.view.*
import java.io.File

class UploadsView(context: Context?, attrs: AttributeSet? = null) :
    ConstraintLayout(context, attrs) {


    init {
        View.inflate(context, R.layout.upload_item_view, this)
    }

    fun bind(savedVideo: SavedVideo, progress: ProgressData) {
        faveIcon.visibility = when (savedVideo.is_favorite) {
            true -> View.VISIBLE
            else -> View.INVISIBLE
        }
        println("------ELSE------ ${savedVideo.uploadState}")
        when (savedVideo.uploadState) {
            UploadState.UPLOADED_MEDIUM -> medQProgressBar.setProgress(100, false)
            UploadState.UPLOADED_HIGH -> highQProgressBar.setProgress(100, false)
            else -> {

                if (savedVideo.clientId == progress.id) {
                    val anim = ProgressBarAnimation(medQProgressBar, 0, progress.end)
                    anim.duration = (progress.end).toLong()
                    when (progress.isHighQuality) {
                        true -> {
                            highQProgressBar.max = progress.end
                            highQProgressBar.startAnimation(anim)
                        }
                        else -> {
                            medQProgressBar.max = progress.end
                            medQProgressBar.startAnimation(anim)
                        }
                    }
                }
            }
        }

        val uri = Uri.fromFile(File(savedVideo.highRes))
        val request = ImageRequestBuilder
            .newBuilderWithSource(uri)
            .setLowestPermittedRequestLevel(ImageRequest.RequestLevel.FULL_FETCH)
            .setProgressiveRenderingEnabled(true)
            .build()

        val controller = Fresco.newDraweeControllerBuilder()
            .setUri(uri)
            .setLowResImageRequest(request)
            .build()

        thumbNail.controller = controller
    }
}