package com.itsovertime.overtimecamera.play.uploadsmanager

import com.itsovertime.overtimecamera.play.model.SavedVideo

interface UploadsManager {

    fun onUploadFavoriteMedQualityVideo()
    fun onUploadFavoriteHighQualityVideo()
    fun onUploadMediumQualityVideo()
    fun onUploadHighQualityVideo()
    fun onUpdateQue(videoList: MutableList<SavedVideo>)

}