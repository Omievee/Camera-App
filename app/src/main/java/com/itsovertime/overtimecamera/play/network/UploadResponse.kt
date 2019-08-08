package com.itsovertime.overtimecamera.play.network

import java.util.*

class UploadResponse(
        val upload: Upload?
)

class Upload(
        val md5: String? = "",
        val id: String? = ""
)