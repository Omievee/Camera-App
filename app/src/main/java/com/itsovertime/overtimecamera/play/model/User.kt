package com.itsovertime.overtimecamera.play.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize


@Parcelize
@Entity(tableName = "User")
class User(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "userName")
    val userName: String?=null,
    @ColumnInfo(name = "name")
    val name: String?=null,
    @ColumnInfo(name = "dynamodb_id")
    val dynamodb_id: String,
    @ColumnInfo(name = "uuid")
    val uuid: String?=""
) : Parcelable

//TODO: add values from jSon below vvv