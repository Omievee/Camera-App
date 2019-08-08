package com.itsovertime.overtimecamera.play.network

class UploadRequest(
        val md5: String? = "",
        val AccessKeyId: String? = "",
        val SecretAccessKey: String? = "",
        val SessionToken: String? = "",
        val S3Bucket: String? = "",
        val S3Key: String? = ""
) {

}