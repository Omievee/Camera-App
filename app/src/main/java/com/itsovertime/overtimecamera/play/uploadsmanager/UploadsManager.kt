package com.itsovertime.overtimecamera.play.uploadsmanager

import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.network.*
import io.reactivex.Observable
import io.reactivex.Single

interface UploadsManager {
    fun onProcessUploadQue(list: MutableList<SavedVideo>)
    fun getVideoInstance(video: SavedVideo): Single<VideoInstanceResponse>
    fun registerWithMD5(data: TokenResponse): Single<EncryptedResponse>
    fun getAWSDataForUpload(): Single<TokenResponse>
    fun uploadVideoToServer(
        upload: Upload,
        array: ByteArray,
        chunk: Int
    ): Observable<retrofit2.Response<VideoUploadResponse>>

    fun onCompleteUpload(uploadId: String): Observable<retrofit2.Response<CompleteResponse>>
    fun writerToServerAfterComplete(
        uploadId: String,
        S3Key: String,
        vidWidth: Int,
        vidHeight: Int,
        hq: Boolean,
        vid: SavedVideo
    ): Single<ServerResponse>

    fun onUpdateQue(): Observable<MutableList<SavedVideo>>
}