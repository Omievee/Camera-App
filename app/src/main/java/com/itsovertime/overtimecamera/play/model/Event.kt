package com.itsovertime.overtimecamera.play.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class Event(
    val id: String,
    val latitude: Double?,
    val longitude: Double?,
    val address: String?,
    val city: String?,
    val zip: String?,
    val name: String?,
    val starts_at: String?,
    val endsAt: String?,
    val duration_in_hours: Int = 3,
    val max_video_length: Int = 12,
    val created_at: String?="",
    val updated_at: String?="",
    val isVideographer: Boolean = false

) : Parcelable
