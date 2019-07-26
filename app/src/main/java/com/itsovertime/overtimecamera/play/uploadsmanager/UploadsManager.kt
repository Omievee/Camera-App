package com.itsovertime.overtimecamera.play.uploadsmanager

import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.network.Video
import io.reactivex.Single

interface UploadsManager {

    fun onUploadFavoriteMedQualityVideo(): Single<Video>
    fun onUploadFavoriteHighQualityVideo()

    fun onUploadMediumQualityVideo()
    fun onUploadHighQualityVideo()

    fun onReadyVideosForUpload(videoList: MutableList<SavedVideo>)
    fun getVideoInstance() : Single<Video>

}