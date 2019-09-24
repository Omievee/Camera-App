package com.itsovertime.overtimecamera.play.network

import android.os.Parcel
import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import retrofit2.http.Field
import java.util.*


@Parcelize
data class VideoInstanceRequest(
    @field:Json(name = "client_id")
    val client_id: UUID? = null,
    val is_favorite: Boolean,
    val is_selfie: Boolean,
    val latitude: Double,
    val longitude: Double,
    val tagged_user_ids: String? = null,
    val event_id: String? = "",
    val address: String? = "",
    val duration_in_hours: Int? = 0,
    val max_video_length: Int? = 12


) : Parcelable

@Parcelize
data class VideoInstanceResponse(
    val video: Video
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class VideoSourceRequest(
    val type: String? = ""
) : Parcelable


@Parcelize
data class Video(
    val id: String? = "",
    val filmed_at: String? = "",
    val client_id: String? = "",
    val user_id: String? = "",
    val event_id: String? = "",
    val item_id: Int? = 0,
    val is_favorite: Boolean? = false,
    val is_funny: Boolean? = false,
    val is_selfie: Boolean? = false,
    val tagged_user_ids: Int? = 0,
    val latitude: Double? = 0.0,
    val longitude: Double? = 0.0,
    val source_high_quality_path: String? = "",
    val source_medium_quality_path: String? = "",
    val source_low_quality_path: String? = ""

//val source_high_quality_progress":-1,
//val source_medium_quality_progress":-1,
//val source_low_quality_progress":-1,
//val source_high_quality_width": null,
//val source_medium_quality_width": null,
//val source_low_quality_width": null,
//val source_high_quality_height": null,
//val source_medium_quality_height": null,
//val source_low_quality_height": null,
//val trim_start": null,
//val trim_end": null,
//val seen_at": null,
//val seen_by_id": null,
//val shared_at": null,
//val shared_by_id": null,
//val should_share_to": null,
//val created_at: "2019-07-29T15:52:06.681Z",
//val updated_at: "2019-07-29T15:52:06.681Z"
) : Parcelable
