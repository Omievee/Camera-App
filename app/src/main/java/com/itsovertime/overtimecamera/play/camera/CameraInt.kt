package com.itsovertime.overtimecamera.play.camera

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
    fun displayUploadsFragment()
}