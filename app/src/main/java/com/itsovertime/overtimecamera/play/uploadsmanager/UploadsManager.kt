package com.itsovertime.overtimecamera.play.uploadsmanager

import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.UploadState
import com.itsovertime.overtimecamera.play.network.*
import io.reactivex.Observable
import io.reactivex.Single
import java.io.File

interface UploadsManager {
    fun onProcessUploadQue(list: MutableList<SavedVideo>)

    fun getVideoInstance(): Single<VideoInstanceResponse>
    fun registerWithMD5(data: TokenResponse): Single<EncryptedResponse>
    fun getAWSDataForUpload(response: VideoInstanceResponse): Single<TokenResponse>
    fun prepareVideoForUpload(upload: Upload)
    fun uploadVideoToServer(data: Array<ByteArray>, chunkToUpload: Int)

    fun resetUploadStateForCurrentVideo()
    fun beginUploadProcess()

    fun onCurrentFileBeingUploaded(): Observable<CurrentVideoUpload>
}