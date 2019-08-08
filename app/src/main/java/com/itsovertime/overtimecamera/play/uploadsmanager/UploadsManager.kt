package com.itsovertime.overtimecamera.play.uploadsmanager

import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.network.*
import io.reactivex.Single

interface UploadsManager {

    fun onUploadFavoriteMedQualityVideo(): Single<VideoInstanceRequest>
    fun uploadVideos(data: TokenResponse): Single<UploadResponse>

    fun onUploadMediumQualityVideo()
    fun onUploadHighQualityVideo()

    fun onReadyVideosForUpload(videoList: MutableList<SavedVideo>)

    fun getVideoInstance(): Single<VideoResponse>
    fun getTokenForLowQuality(response: VideoResponse): Single<TokenResponse>

}