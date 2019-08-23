package com.itsovertime.overtimecamera.play.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize


@Parcelize
@Entity(tableName = "User")
class User(
    @PrimaryKey
    val id: String? = "",
    val userName: String? = "",
    val name: String? = "",
    val gradYear: Int? = 0,
    val sig_trk: String? = ""
) : Parcelable