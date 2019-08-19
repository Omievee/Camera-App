package com.itsovertime.overtimecamera.play.network

class EncryptedResponse(
        val upload: Upload?
)

class Upload(
        val md5: String? = "",
        val id: String? = ""
)