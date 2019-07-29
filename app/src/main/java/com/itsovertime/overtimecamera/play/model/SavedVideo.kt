package com.itsovertime.overtimecamera.play.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize
import java.util.*


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
    @ColumnInfo(name = "event_id")
    val event_id: Int? = null
) : Parcelable