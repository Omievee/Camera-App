package com.itsovertime.overtimecamera.play.network

class EncryptedResponse(
        val upload: Upload?
)

class Upload(
        val md5: String? = "",
        val id: String? = "",
        val S3Bucket: String? = "",
        val S3Key: String? = "",
        val AccessKeyId: String? = "",
        val SecretAccessKey: String? = "",
        val SessionToken: String? = ""
)
