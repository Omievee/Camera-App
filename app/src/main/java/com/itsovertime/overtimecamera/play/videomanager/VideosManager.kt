package com.itsovertime.overtimecamera.play.videomanager

import android.content.Context
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.network.UploadResponse
import io.reactivex.Observable
import io.reactivex.Single
import java.io.File
import java.util.*

interface VideosManager {

    fun subscribeToVideoGallery(): Observable<List<SavedVideo>>
    fun loadFromDB()
    fun saveVideoToDB(filePath: String, isFavorite: Boolean)
    fun transcodeVideo(videoFile: File)
    fun updateVideoFavorite(isFavorite: Boolean)
    fun updateVideoFunny(isFunny: Boolean)
    fun trimVideo(file: File)
}