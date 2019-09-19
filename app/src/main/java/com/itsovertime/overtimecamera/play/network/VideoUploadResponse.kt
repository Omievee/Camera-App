package com.itsovertime.overtimecamera.play.network

data class VideoUploadResponse(val success: Boolean) {
}

data class CompleteResponse(
    val status: String,
    val error: String? = "",
    val upload: Upload?
) {

}


class ServerResponse(
)

class ServerRequest(
    val S3Key: String = "",
    val source_high_quality_path: String? = "",
    val source_medium_quality_path: String = "",
    val source_medium_quality_progress: Double = 1.0,
    val source_high_quality_progress: Double = 1.0,
    val source_medium_quality_width: Int = 0,
    val source_medium_quality_height: Int = 0,
    val source_high_quality_width: Int = 0,
    val source_high_quality_height: Int = 0
) {}