package com.itsovertime.overtimecamera.play.uploadsmanager

import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.network.*
import io.reactivex.Single

interface UploadsManager {
    fun onReadyVideosForUpload(videoList: MutableList<SavedVideo>)
    fun getVideoInstance(): Single<VideoInstanceResponse>
    fun registerUploadForId(data: TokenResponse): Single<EncryptedResponse>
    fun getAWSDataForUpload(response: VideoInstanceResponse): Single<TokenResponse>
    fun uploadVideo(upload: Upload): Single<VideoUploadResponse>

    fun onUploadFavoriteMedQualityVideo(): Single<VideoInstanceRequest>
    fun onUploadMediumQualityVideo()
    fun onUploadHighQualityVideo()

}