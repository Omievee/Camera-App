package com.itsovertime.overtimecamera.play.uploadsmanager

import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.network.*
import io.reactivex.Observable
import io.reactivex.Single

interface UploadsManager {
    fun onProcessUploadQue(list: MutableList<SavedVideo>)
    fun getVideoInstance(video: SavedVideo): Single<VideoInstanceResponse>
    fun registerWithMD5(data: TokenResponse): Single<EncryptedResponse>
    fun getAWSDataForUpload(response: VideoInstanceResponse): Single<TokenResponse>
    fun uploadVideoToServer(
        upload: Upload,
        array: ByteArray,
        chunk: Int
    ): Single<VideoUploadResponse>
    fun onCompleteUpload(uploadId: String): Single<CompleteResponse>


    fun resetUploadStateForCurrentVideo()
    fun onUpdatedQue(): Observable<List<SavedVideo>>
}