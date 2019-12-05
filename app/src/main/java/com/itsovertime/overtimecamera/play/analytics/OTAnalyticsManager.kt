package com.itsovertime.overtimecamera.play.analytics

import android.content.Context
import com.itsovertime.overtimecamera.play.model.Event
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.User

interface OTAnalyticsManager {

    fun initMixpanel(cntx: Context, userId:String?)
    fun onTrackDeviceThermalStatus(cntx: Context)
    fun onDestroyMixpanel()
    fun onTrackSelectedEvent(event: Event?)
    fun onTrackFirstAppOpen()
    fun onTrackCameraRecording()
    fun onTrackFailedToCreateFile()
    fun onTrackVideoFileCreated(savedVideo: SavedVideo?)
    fun onTrackUploadEvent(event: String, uploadProperties: Array<String>)
    fun onTrackTrim(properties: Array<String>)

}