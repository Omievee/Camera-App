package com.itsovertime.overtimecamera.play.uploads

enum class UploadState {
    QUEUED,
    REGISTERING,
    REGISTERED,
    UPLOADED_LOW,
    UPLOADED_MEDIUM,
    UPLOADED_HIGH,
    UPLOADING_LOW,
    UPLOADING_MEDIUM,
    UPLOADING_HIGH,
    COMPLETE,
    UNKNOWN
}