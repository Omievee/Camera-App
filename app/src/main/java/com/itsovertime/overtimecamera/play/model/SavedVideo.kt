package com.itsovertime.overtimecamera.play.model

import android.os.Parcelable
import androidx.room.*
import com.itsovertime.overtimecamera.play.uploads.UploadState
import kotlinx.android.parcel.Parcelize


@Parcelize
@Entity(tableName = "SavedVideo")
data class SavedVideo(
        @PrimaryKey
        @ColumnInfo(name = "id")
        val id: String,
        @ColumnInfo(name = "vidPath")
        val vidPath: String?,
        @ColumnInfo(name = "mediumVidPath")
        val mediumVidPath: String? = null,
        @ColumnInfo(name = "trimmedVidPath")
        val trimmedVidPath: String? = null,
        @ColumnInfo(name = "is_favorite")
        val is_favorite: Boolean = false,
        @ColumnInfo(name = "is_funny")
        val is_funny: Boolean = false,
        @ColumnInfo(name = "is_selfie")
        val is_selfie: Boolean = false,
        @ColumnInfo(name = "eventID")
        val eventId: String,
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
        val uploadState: UploadState
) : Parcelable

enum class UploadState {
    QUEUED,
    PAUSED,
    REGISTERED,
    UPLOADED_MEDIUM,
    UPLOADED_HIGH,
    COMPLETE,
    UNKONWN
}


object enumConverter {


    @TypeConverter
    @JvmStatic
    fun toOrdinal(type: UploadState): Int = type.ordinal

    @TypeConverter
    @JvmStatic
    fun toEnum(ordinal: Int): UploadState = UploadState.values().first { it.ordinal == ordinal }
}


//@ColumnInfo(name = "tagged")
//val taggedUsers: List<TaggedUsers> = emptyList(),
//@ColumnInfo(name = "videographers")
//val videographers: List<User> = emptyList()
//        @ColumnInfo(name = "team_id")
//        val tagged_team_ids: List<String> = emptyList()