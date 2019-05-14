package com.overtime.camera.model

import android.net.Uri
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize


@Parcelize
@Entity
data class SavedVideo(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val vidPath: String?,
    val isFavorite: Boolean? = false
) : Parcelable