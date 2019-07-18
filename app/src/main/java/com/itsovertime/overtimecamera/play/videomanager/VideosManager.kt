package com.itsovertime.overtimecamera.play.videomanager

import com.itsovertime.overtimecamera.play.model.SavedVideo
import io.reactivex.Observable
import java.io.File

interface VideosManager {

    fun subscribeToVideoGallery(): Observable<List<SavedVideo>>
    fun subscribeToVideoGallerySize() :Observable<Int>
    fun loadFromDB()
    fun saveHighQualityVideoToDB(filePath: String, isFavorite: Boolean)
    fun transcodeVideo(videoFile: File)
    fun updateVideoFavorite(isFavorite: Boolean)
    fun updateVideoFunny(isFunny: Boolean)
}