package com.itsovertime.overtimecamera.play.model

import android.os.Parcelable
import androidx.room.Entity
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
@Entity(tableName = "Event")
data class Event(
    val id: Int,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val city: String,
    val zip: String,
    val name: String,
    val starts_at: Date?,
    val endsAt: Date?,
    val duration_in_hours: Int = 3,
    val mx_video_length: Int = 12,
    val created_at: String?,
    val updated_at: String?,
    val taggedUsers: List<TaggedUsers> = emptyList(),
    val tagged_team_ids: List<String> = emptyList(),
    val videographers: List<User> = emptyList(),
    val isVideographer: Boolean = false

) : Parcelable
