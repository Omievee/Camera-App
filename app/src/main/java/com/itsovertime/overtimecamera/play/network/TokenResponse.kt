package com.itsovertime.overtimecamera.play.network

class TokenResponse(
        val AccessKeyId: String,
        val SecretAccessKey: String,
        val SessionToken: String,
        val Expiration: String,
        val S3Bucket: String,
        val S3Key: String
) {
}