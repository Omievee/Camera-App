package com.itsovertime.overtimecamera.play.uploads

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.UploadState
import com.itsovertime.overtimecamera.play.progressbar.ProgressBarAnimation
import kotlinx.android.synthetic.main.upload_item_view.view.*
import java.io.File


class UploadsView(context: Context?, attrs: AttributeSet? = null) :
    ConstraintLayout(context, attrs) {

    var savedVideo: SavedVideo? = null

    init {
        View.inflate(context, R.layout.upload_item_view, this)
    }

    fun bind(savedVideo: SavedVideo, debug: Boolean) {
        this.savedVideo = savedVideo
        when (debug) {
            true -> {
                uploadedText.visibility = View.VISIBLE
                statusText2.visibility = View.VISIBLE
                clientText.visibility = View.VISIBLE
                pendingProgress.visibility = View.GONE
                serverText.visibility = View.VISIBLE

                uploadedText.text = when (savedVideo.uploadState) {
                    UploadState.UPLOADING_MEDIUM -> "Uploading medium quality"
                    UploadState.UPLOADED_MEDIUM -> "Uploaded medium quality"
                    UploadState.UPLOADING_HIGH -> "Uploading high quality"
                    UploadState.UPLOADED_HIGH -> "Uploaded high quality"
                    else -> "Waiting to upload"
                }
                statusText2.text = when (savedVideo.mediumUploaded) {
                    true -> "Waiting to upload HD"
                    else -> "Waiting to upload"
                }
                clientText.text = "Client: ${savedVideo.clientId}"
                serverText.text = "Server: ${savedVideo.uploadId}"

            }
            else -> {
                medQProgressBar.visibility = View.VISIBLE
                highQProgressBar.visibility = View.VISIBLE
                medQT.visibility = View.VISIBLE
                highQT.visibility = View.VISIBLE
                pendingProgress.visibility = View.VISIBLE

                if (savedVideo.mediumUploaded) {
                    medQProgressBar.setProgress(100, false)
                    check1.visibility = View.VISIBLE
                }

                if (savedVideo.highUploaded) {
                    highQProgressBar.setProgress(100, false)
                    check2.visibility = View.VISIBLE
                }
                if (savedVideo.highUploaded && savedVideo.mediumUploaded) {
                    pendingProgress.visibility = View.GONE
                }
            }
        }


        faveIcon.visibility = when (savedVideo.is_favorite) {
            true -> View.VISIBLE
            else -> View.INVISIBLE
        }

        Glide.with(context)
            .load(File(savedVideo.highRes).path)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(thumbNail)
    }

    fun updateMediumProgress(value: Int) {
        medQProgressBar.setProgress(value, true)
    }

    fun updateHighProgress(value: Int) {
        highQProgressBar.setProgress(value, true)
    }
}