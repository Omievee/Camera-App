package com.itsovertime.overtimecamera.play.analytics

import android.content.Context
import com.itsovertime.overtimecamera.play.model.Event
import com.itsovertime.overtimecamera.play.model.SavedVideo

interface OTAnalyticsManager {
    fun initMixpanel()
    fun onTrackDeviceThermalStatus(cntx: Context)
    fun onDestroyMixpanel()
    fun onTrackSelectedEvent(event: Event?)
    fun onTrackCameraRecording()
    fun onTrackFailedToCreateFile()
    fun onTrackVideoFileCreated(savedVideo: SavedVideo?)
    fun onTrackUploadEvent(event: String, uploadProperties: UploadProperties)
}