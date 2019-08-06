package com.itsovertime.overtimecamera.play.camera

import com.itsovertime.overtimecamera.play.model.Event

interface CameraInt {
    fun openCamera(width: Int, height: Int, camera: Int)
    fun closeCamera()
    fun startPreview()
    fun updatePreview()
    fun startRecording()
    fun stopRecording(isPaused: Boolean)
    fun closePreviewSession()
    fun switchCameras()
    fun setUpClicks()
    fun updateUploadsIconCount(count: String)
    fun stopProgressAnimation()
    fun showOrHideViewsForCamera()
    fun setUpEventViewData(event: String?, eventList: List<Event>?)
    fun openEvents(evList: List<Event>)

}