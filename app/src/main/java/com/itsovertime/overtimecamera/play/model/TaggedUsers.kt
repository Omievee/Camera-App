package com.itsovertime.overtimecamera.play.model

import android.os.Parcelable
import androidx.room.Entity
import kotlinx.android.parcel.Parcelize
import java.util.*


@Parcelize
@Entity(tableName = "TaggedUsers")
class TaggedUsers(
    val id: String?,
    val name: String?,
    val userName: String,
    val lastTaggedAt: Date?,
    val team: String?,
    val teamNumber: Int?,
    val club: String?,
    val clubNumber: Int


) : Parcelable {
}