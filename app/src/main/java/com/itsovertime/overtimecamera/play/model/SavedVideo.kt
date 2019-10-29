package com.itsovertime.overtimecamera.play.model

import android.os.Parcelable
import androidx.room.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.android.parcel.Parcelize
import java.util.*
import kotlin.collections.ArrayList
import com.mixpanel.android.mpmetrics.Tweaks.TweakValue.fromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.lang.reflect.ParameterizedType


@Parcelize
@Entity(tableName = "SavedVideo")
data class SavedVideo(
    @PrimaryKey
    @ColumnInfo(name = "clientId")
    val clientId: String,
    @ColumnInfo(name = "id")
    var id: String? = "",
    @ColumnInfo(name = "uploadId")
    val uploadId: String? = "",
    @ColumnInfo(name = "highRes")
    val highRes: String?,
    @ColumnInfo(name = "mediumRes")
    var mediumRes: String? = null,
    @ColumnInfo(name = "trimmedVidPath")
    val trimmedVidPath: String? = null,
    @ColumnInfo(name = "md5")
    val md5: String? = null,
    @ColumnInfo(name = "is_favorite")
    var is_favorite: Boolean = false,
    @ColumnInfo(name = "is_funny")
    val is_funny: Boolean = false,
    @ColumnInfo(name = "is_selfie")
    val is_selfie: Boolean = false,
    @ColumnInfo(name = "event_id")
    val event_id: String? = "",
    @ColumnInfo(name = "lat")
    val latitude: Double?,
    @ColumnInfo(name = "long")
    val longitude: Double?,
    @ColumnInfo(name = "address")
    val address: String?,
    @ColumnInfo(name = "city")
    val city: String? = null,
    @ColumnInfo(name = "zip")
    val zip: String? = null,
    @ColumnInfo(name = "eventName")
    val eventName: String?,
    @ColumnInfo(name = "starts_at")
    val starts_at: String?,
    @ColumnInfo(name = "ends_at")
    val endsAt: String? = null,
    @ColumnInfo(name = "duration")
    val duration_in_hours: Int = 3,
    @ColumnInfo(name = "max_length")
    val max_video_length: Int = 12,
    @ColumnInfo(name = "created")
    val created_at: String? = null,
    @ColumnInfo(name = "updated")
    val updated_at: String? = null,
    @ColumnInfo(name = "isVideographer")
    val isVideographer: Boolean = false,
    @ColumnInfo(name = "uploadState")
    var uploadState: UploadState,
    @ColumnInfo(name = "isProcessed")
    var isProcessed: Boolean = false,
    @ColumnInfo(name = "mediumUploaded")
    val mediumUploaded: Boolean = false,
    @ColumnInfo(name = "highUploaded")
    val highUploaded: Boolean = false

) : Parcelable

enum class UploadState {
    QUEUED,
    REGISTERING,
    REGISTERED,
    UPLOADING_MEDIUM,
    UPLOADED_MEDIUM,
    UPLOADING_HIGH,
    UPLOADED_HIGH,
    COMPLETE
}


object customConverter {
    @TypeConverter
    @JvmStatic
    fun toOrdinal(type: UploadState): Int = type.ordinal

    @TypeConverter
    @JvmStatic
    fun toEnum(ordinal: Int): UploadState = UploadState.values().first { it.ordinal == ordinal }

//    val moshi = Moshi
//        .Builder()
//        .add(KotlinJsonAdapterFactory())
//        .build()
//
//    val mapOfStringsType: ParameterizedType =
//        Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
//    val mapOfStringsAdapter: JsonAdapter<Map<String, String>> =
//        moshi.adapter<Map<String, String>>(mapOfStringsType)
//
//
//    @TypeConverter
//    fun stringToMap(data: String): Map<String, String> {
//        return mapOfStringsAdapter.fromJson(data).orEmpty()
//    }
//
//    @TypeConverter
//    fun mapToString(map: Map<String, String>): String {
//        return mapOfStringsAdapter.toJson(map)
//    }
}


//@ColumnInfo(name = "tagged")
//val taggedUsers: List<TaggedUsers> = emptyList(),
//@ColumnInfo(name = "videographers")
//val videographers: List<User> = emptyList()
//        @ColumnInfo(name = "team_id")
//        val tagged_team_ids: List<String> = emptyList()