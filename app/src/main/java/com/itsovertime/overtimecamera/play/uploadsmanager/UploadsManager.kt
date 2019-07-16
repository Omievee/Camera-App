package com.itsovertime.overtimecamera.play.uploadsmanager

import android.net.NetworkInfo
import com.itsovertime.overtimecamera.play.model.SavedVideo
import io.reactivex.Observable

interface UploadsManager {

    fun onUploadFavoriteMedQualityVideo()
    fun onUploadFavoriteHighQualityVideo()
    fun onUploadMediumQualityVideo()
    fun onUploadHighQualityVideo()
    fun onReadyVideosForUpload(videoList: MutableList<SavedVideo>)



}