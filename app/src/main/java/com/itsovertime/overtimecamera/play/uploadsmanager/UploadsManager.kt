package com.itsovertime.overtimecamera.play.uploadsmanager

import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.network.*
import io.reactivex.Observable
import io.reactivex.Single

interface UploadsManager {
    fun getVideoInstance(video: SavedVideo): Observable<VideoInstanceResponse>
    fun onUpdateVideoInstance(id:String,isFavorite: Boolean?, isFunny: Boolean?): Observable<VideoInstanceResponse>
    fun registerWithMD5(data: TokenResponse, hdReady:Boolean,video:SavedVideo): Observable<EncryptedResponse>
    fun getAWSDataForUpload(): Observable<TokenResponse>
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
    ): Observable<ServerResponse>


}