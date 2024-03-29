package com.itsovertime.overtimecamera.play.network

import com.itsovertime.overtimecamera.play.model.Event

data class VideoUploadResponse(val success: Boolean) {
}

data class CompleteResponse(
    val status: String,
    val error: String? = "",
    val upload: Upload?
) {

}


data class ServerResponse(
    val video: Video
)

class ServerRequest(
    val source_high_quality_path: String? = null,
    val source_medium_quality_path: String? = null,
    val source_medium_quality_progress: Double = 1.0,
    val source_high_quality_progress: Double = 1.0,
    val source_medium_quality_width: Int? = null,
    val source_medium_quality_height: Int? = null,
    val source_high_quality_width: Int? = null,
    val source_high_quality_height: Int? = null
) {

}