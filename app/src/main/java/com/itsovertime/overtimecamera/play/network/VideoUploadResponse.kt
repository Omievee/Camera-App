package com.itsovertime.overtimecamera.play.network

data class VideoUploadResponse(val success: Boolean) {
}

data class CompleteResponse(
    val status: String,
    val error: CompleteError? = null,
    val upload: Upload?
) {

}

data class CompleteError(
    val type: String,
    val data: Array<Int> = emptyArray()
)

class ServerResponse() {

}

 class ServerRequest() {}