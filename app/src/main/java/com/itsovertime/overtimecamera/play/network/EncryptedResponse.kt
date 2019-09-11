package com.itsovertime.overtimecamera.play.network

import android.os.Parcelable
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
@JsonClass(generateAdapter = true)
@Parcelize
class EncryptedResponse(
        val upload: Upload?
):Parcelable
@JsonClass(generateAdapter = true)
@Parcelize
class Upload(
        val md5: String? = "",
        val id: String? = "",
        val S3Bucket: String? = "",
        val S3Key: String? = "",
        val AccessKeyId: String? = "",
        val SecretAccessKey: String? = "",
        val SessionToken: String? = ""
) : Parcelable

class UploadData(
        val md5: String,
        val video: SavedVideo
)
