package com.overtime.camera.camera

interface CameraInt {
    fun openCamera(width: Int, height: Int)
    fun closeCamera()
    fun startPreview()
    fun updatePreview()
    fun startRecording()
    fun stopRecording()
    fun closePreviewSession()
}