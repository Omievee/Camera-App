package com.itsovertime.overtimecamera.play.network

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize


@JsonClass(generateAdapter = true)
@Parcelize
class TokenResponse(
    val AccessKeyId: String? = "",
    val SecretAccessKey: String? = "",
    val SessionToken: String? = "",
    val Expiration: String? = "",
    val S3Bucket: String? = "",
    val S3Key: String? = ""
) : Parcelable