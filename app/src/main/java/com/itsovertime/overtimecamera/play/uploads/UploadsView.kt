package com.itsovertime.overtimecamera.play.uploads

import android.content.Context
import android.media.ThumbnailUtils
import android.provider.MediaStore
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

    init {
        View.inflate(context, R.layout.upload_item_view, this)
    }

    fun bind(savedVideo: SavedVideo, debug: Boolean, hd: Boolean) {
        this.savedVideo = savedVideo
        when (debug) {
            true -> {
                uploadedText.visibility = View.VISIBLE
                statusText2.visibility = View.VISIBLE
                clientText.visibility = View.VISIBLE
                pendingProgress.visibility = View.GONE
                serverText.visibility = View.VISIBLE
                medQProgressBar.visibility = View.INVISIBLE
                highQProgressBar.visibility = View.INVISIBLE
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
                serverText.text = "Server: ${savedVideo.uploadId}"

            }
            else -> {
                medQProgressBar.visibility = View.VISIBLE
                medQProgressBar.setProgress(0, false)
                highQProgressBar.visibility = View.VISIBLE
                highQProgressBar.setProgress(0, false)
                medQT.visibility = View.VISIBLE
                highQT.visibility = View.VISIBLE
                pendingProgress.visibility = when (hd) {
                    true -> View.GONE
                    else -> View.VISIBLE
                }
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
        val thumb = ThumbnailUtils.createVideoThumbnail(
            File(savedVideo.highRes).path,
            MediaStore.Video.Thumbnails.MINI_KIND
        )

        Glide.with(context)
            .load(thumb)
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