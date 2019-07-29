package com.itsovertime.overtimecamera.play.uploadsmanager

import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.network.VideoResponse
import com.itsovertime.overtimecamera.play.network.VideoInstanceRequest
import io.reactivex.Single

interface UploadsManager {

    fun onUploadFavoriteMedQualityVideo(): Single<VideoInstanceRequest>
    fun onUploadFavoriteHighQualityVideo()

    fun onUploadMediumQualityVideo()
    fun onUploadHighQualityVideo()

    fun onReadyVideosForUpload(videoList: MutableList<SavedVideo>)
    fun getVideoInstance(): Single<VideoResponse>

}