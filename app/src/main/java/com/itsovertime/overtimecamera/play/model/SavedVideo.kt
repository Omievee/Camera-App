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
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,
    @ColumnInfo(name = "vidPath")
    val vidPath: String?,
    @ColumnInfo(name = "isFavorite")
    val isFavorite: Boolean = false,
    @ColumnInfo(name = "isFunny")
    val isFunny: Boolean = false
) : Parcelable