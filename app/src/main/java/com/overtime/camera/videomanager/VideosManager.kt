package com.overtime.camera.videomanager

import android.content.Context
import com.overtime.camera.model.SavedVideo
import io.reactivex.Observable
import io.reactivex.Single
import java.io.File

interface VideosManager {

    fun subscribeToVideoGallery(): Observable<SavedVideo>
    fun loadVideosFromGallery(context: Context)
}