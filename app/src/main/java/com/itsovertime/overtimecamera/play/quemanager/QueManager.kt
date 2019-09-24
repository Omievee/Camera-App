package com.itsovertime.overtimecamera.play.quemanager

import com.itsovertime.overtimecamera.play.model.SavedVideo
import io.reactivex.Observable
import java.util.*

interface QueManager {
    fun onUpdateQueList(video: List<SavedVideo>)
    fun onGetNextVideo(): SavedVideo?
    fun onGetNextVideoFromMediumList() : SavedVideo?

    fun onIsQueReady() : Observable<Boolean>
}