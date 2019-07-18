package com.itsovertime.overtimecamera.play.network

import android.os.Parcel
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class VideoResponse(
    val client_id: Int,
    val is_favorite: Boolean,
    val is_selfie: Boolean,
    val latitude: Double,
    val longitude: Double

) : Parcelable {

}