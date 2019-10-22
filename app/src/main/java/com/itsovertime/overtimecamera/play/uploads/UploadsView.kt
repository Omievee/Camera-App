package com.itsovertime.overtimecamera.play.uploads

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.progressbar.ProgressBarAnimation
import kotlinx.android.synthetic.main.upload_item_view.view.*
import java.io.File


class UploadsView(context: Context?, attrs: AttributeSet? = null) :
    ConstraintLayout(context, attrs) {

    var savedVideo: SavedVideo? = null

    init {
        View.inflate(context, R.layout.upload_item_view, this)
    }

    fun bind(savedVideo: SavedVideo) {
        this.savedVideo = savedVideo

        faveIcon.visibility = when (savedVideo.is_favorite) {
            true -> View.VISIBLE
            else -> View.INVISIBLE
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

        println("HD?? ${savedVideo.highUploaded}")

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