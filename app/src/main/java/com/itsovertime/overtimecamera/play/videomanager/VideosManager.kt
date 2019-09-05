package com.itsovertime.overtimecamera.play.videomanager

import com.itsovertime.overtimecamera.play.model.Event
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.UploadState
import io.reactivex.Observable
import java.io.File

interface VideosManager {

    fun subscribeToVideoGallery(): Observable<List<SavedVideo>>
    fun subscribeToVideoGallerySize(): Observable<Int>
    fun loadFromDB()
    fun saveHighQualityVideoToDB(event: Event? = null, filePath: String, isFavorite: Boolean)
    fun transcodeVideo(videoFile: File)
    fun updateVideoFavorite(isFavorite: Boolean)
    fun updateVideoFunny(isFunny: Boolean)
    fun updateVideoMd5(md5: String, clientId: String)
    fun updateUploadId(uplaodId:String, clientId: String)
    fun updateVideoInstanceId(videoId: String, clientId: String)
    fun updateVideoStatus(video: SavedVideo, state: UploadState)
    fun resetUploadState()

}