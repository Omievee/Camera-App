package com.itsovertime.overtimecamera.play.analytics

interface OTAnalyticsManager {


    fun initMixpanel()
    fun onTrackDeviceThermalStatus()
    fun onDestroyMixpanel()
    fun onTrackUploadEvent(event: String, analyticsProperties: AnalyticsProperties)
}