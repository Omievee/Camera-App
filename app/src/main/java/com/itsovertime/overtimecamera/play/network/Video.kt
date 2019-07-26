package com.itsovertime.overtimecamera.play.network

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import retrofit2.http.Field
import java.util.*


@JsonClass(generateAdapter = true)
@Parcelize
data class Video(
    @field:Json(name = "client_id")
    val client_id: UUID,
    val is_favorite: Boolean,
    val is_selfie: Boolean,
    val latitude: Double,
    val longitude: Double
//    val event_id: Int,
//    val tagged_user_ids: String,
//    val source_high_quality_path: String


) : Parcelable