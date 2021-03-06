package com.itsovertime.overtimecamera.play.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.squareup.moshi.JsonAdapter
import kotlinx.android.parcel.Parcelize


@Parcelize
@Entity(tableName = "SavedVideo")
data class SavedVideo(
    @PrimaryKey
    @ColumnInfo(name = "clientId")
    val clientId: String,
    @ColumnInfo(name = "uploadId")
    var videoId: String,
    @ColumnInfo(name = "highRes")
    val highRes: String?,
    @ColumnInfo(name = "mediumRes")
    var mediumRes: String? = null,
    @ColumnInfo(name = "trimmedVidPath")
    val trimmedVidPath: String? = null,
    @ColumnInfo(name = "encodedPath")
    var encodedPath: String? = null,
    @ColumnInfo(name = "md5")
    val md5: String? = null,
    @ColumnInfo(name = "is_favorite")
    var is_favorite: Boolean = false,
    @ColumnInfo(name = "is_funny")
    var is_funny: Boolean = false,
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
    @ColumnInfo(name = "filmed_at")
    val filmed_at: String? = null,
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
    val highUploaded: Boolean = false,
    @ColumnInfo(name = "tagged")
    val taggedUsers: ArrayList<String>? = arrayListOf()
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
}

class stringArrayConvertor {

    @TypeConverter
    fun fromString(value: String): ArrayList<String> {
        val listType = object : TypeToken<ArrayList<String>>() {
        }.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromArrayList(list: ArrayList<String>): String {
        return Gson().toJson(list)
    }
}

class taggedUserConvertor {
    @TypeConverter
    fun fromTaggedString(value: String): ArrayList<TaggedUsers> {
        val listType = object : TypeToken<ArrayList<String>>() {
        }.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromTaggedArray(list: ArrayList<TaggedUsers>): String {
        return Gson().toJson(list)
    }

}

//
//@ColumnInfo(name = "tagged")
//val taggedUsers: List<TaggedUsers> = emptyList(),
//@ColumnInfo(name = "videographers")
//val videographers: List<User> = emptyList()
//@ColumnInfo(name = "team_id")
//val tagged_team_ids: List<String> = emptyList()