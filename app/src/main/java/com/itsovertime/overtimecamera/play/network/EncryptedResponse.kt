package com.itsovertime.overtimecamera.play.network

import com.itsovertime.overtimecamera.play.model.SavedVideo

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

class UploadData(
        val md5: String,
        val video: SavedVideo
)
