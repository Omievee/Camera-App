package com.itsovertime.overtimecamera.play.uploadsmanager

import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.UploadState
import com.itsovertime.overtimecamera.play.network.*
import io.reactivex.Observable
import io.reactivex.Single
import java.io.File

interface UploadsManager {
    fun onCurrentFileBeingUploaded(): Observable<CurrentVideoUpload>
    fun getVideoInstance(): Single<VideoInstanceResponse>
    fun registerWithMD5(data: TokenResponse): Single<EncryptedResponse>
    fun getAWSDataForUpload(response: VideoInstanceResponse): Single<TokenResponse>
    fun prepareVideoForUpload(upload: Upload)
    fun uploadVideoToServer(data: Array<ByteArray>, chunkToUpload: Int)
    fun onUpdateFavoriteVideosList(favoriteVideos: MutableList<SavedVideo>)
    fun onUpdateStandardVideosList(standardVideos: MutableList<SavedVideo>)
    fun beginUploadProcess()

}
//
//return api
//.uploadSelectedVideo(
//md5Header = hexToString(array[0]),
//videoId = upload.id ?: "",
//uploadChunk = 0,
//file = request
//)
//.doOnError {
//}
//.doOnSuccess {
//    println("SUCCESS ? ${it.success}")
//}
//.subscribeOn(Schedulers.io())
//.observeOn(AndroidSchedulers.mainThread())