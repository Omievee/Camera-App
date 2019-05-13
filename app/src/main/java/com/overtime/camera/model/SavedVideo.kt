package com.overtime.camera.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


@Parcelize
data class SavedVideo(
    val uri: Uri


) : Parcelable {

}