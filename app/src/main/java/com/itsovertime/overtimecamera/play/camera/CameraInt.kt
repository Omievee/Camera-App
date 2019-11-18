package com.itsovertime.overtimecamera.play.camera

import com.itsovertime.overtimecamera.play.model.Event

interface CameraInt {
    fun openCamera(width: Int, height: Int, camera: Int)
    fun closeCamera()
    fun prepareCameraForRecording()
    fun stopRecording(isPaused: Boolean)
    fun switchCameras()
    fun setUpClicks()
    fun updateUploadsIconCount(count: String)
    fun stopProgressAnimation()
    fun showOrHideViewsForCamera()
    fun setUpEventViewData(eventList: MutableList<Event>?)
    fun setUpDefaultEvent(event: Event?)



}