package com.itsovertime.overtimecamera.play.uploads

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.UploadState
import kotlinx.android.synthetic.main.upload_item_view.view.*
import java.io.File


class UploadsView(context: Context?, attrs: AttributeSet? = null) :
    ConstraintLayout(context, attrs) {

    var savedVideo: SavedVideo? = null
    var isUploading: Boolean? = null

    init {
        View.inflate(context, R.layout.upload_item_view, this)
    }

    fun bind(savedVideo: SavedVideo, debug: Boolean, hd: Boolean) {
        this.savedVideo = savedVideo
        //medQProgressBar.setProgress(0, false)
        //highQProgressBar.setProgress(0, false)
        check1.visibility = View.GONE
        check2.visibility = View.GONE

        when (debug) {
            true -> {
                check1.visibility = View.GONE
                check2.visibility = View.GONE
                uploadedText.visibility = View.VISIBLE
                statusText2.visibility = View.VISIBLE
                clientText.visibility = View.VISIBLE
                pendingProgress.visibility = View.GONE
                serverText.visibility = View.VISIBLE
                medQProgressBar.visibility = View.GONE
                highQProgressBar.visibility = View.GONE

                uploadedText.text = when (savedVideo.uploadState) {
                    UploadState.UPLOADING_MEDIUM -> "Uploading medium quality"
                    UploadState.UPLOADED_MEDIUM -> "Uploaded medium quality"
                    UploadState.UPLOADING_HIGH -> "Uploading high quality"
                    UploadState.UPLOADED_HIGH -> "Uploaded high quality"
                    else -> "Waiting to upload"
                }
                if (savedVideo.mediumUploaded) {
                    statusText2.text = "Waiting to upload HD"
                }
                if (savedVideo.highUploaded) {
                    statusText2.text = "Finished"
                }
                clientText.text = "Client: ${savedVideo.clientId}"
                serverText.text = when (savedVideo.videoId.isNullOrEmpty()) {
                    true -> "Server: Pending"
                    else -> "Server: ${savedVideo.videoId}"
                }
            }
            else -> {
                medQProgressBar.visibility = View.VISIBLE
                highQProgressBar.visibility = View.VISIBLE
                medQT.visibility = View.VISIBLE
                highQT.visibility = View.VISIBLE
                pendingProgress.visibility = when (hd) {
                    true -> View.GONE
                    else -> View.VISIBLE
                }

                check1.visibility = when (savedVideo.mediumUploaded) {
                    true -> View.VISIBLE
                    else -> View.GONE
                }
                check2.visibility = when (savedVideo.highUploaded) {
                    true -> View.VISIBLE
                    else -> View.GONE
                }

                if (savedVideo.highUploaded && savedVideo.mediumUploaded) {
                    pendingProgress.visibility = View.GONE
                }
            }
        }
        if (savedVideo.highUploaded && savedVideo.mediumUploaded) {
            pendingProgress.visibility = View.GONE
        }
        faveIcon.visibility = when (savedVideo.is_favorite) {
            true -> View.VISIBLE
            else -> View.INVISIBLE
        }

        when (savedVideo.mediumUploaded) {
            true -> medQProgressBar.setProgress(100, false)
            else -> medQProgressBar.setProgress(0, false)
        }
        when (savedVideo.highUploaded) {
            true -> highQProgressBar.setProgress(100, false)
            else -> highQProgressBar.setProgress(0, false)
        }

        Glide.with(context)
            .load(File(savedVideo.highRes).path)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(thumbNail)
    }

}