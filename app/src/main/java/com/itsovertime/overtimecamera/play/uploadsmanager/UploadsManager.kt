package com.itsovertime.overtimecamera.play.uploadsmanager

import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.network.TokenResponse
import com.itsovertime.overtimecamera.play.network.UploadRequest
import com.itsovertime.overtimecamera.play.network.VideoInstanceRequest
import com.itsovertime.overtimecamera.play.network.VideoResponse
import io.reactivex.Single

interface UploadsManager {

    fun onUploadFavoriteMedQualityVideo(): Single<VideoInstanceRequest>
    fun uploadVideos(data: TokenResponse): Single<UploadRequest>

    fun onUploadMediumQualityVideo()
    fun onUploadHighQualityVideo()

    fun onReadyVideosForUpload(videoList: MutableList<SavedVideo>)

    fun getVideoInstance(): Single<VideoResponse>
    fun getTokenForLowQuality(response: VideoResponse): Single<TokenResponse>

}