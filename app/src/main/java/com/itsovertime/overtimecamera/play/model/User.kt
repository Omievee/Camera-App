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
    val userName: String? = null,
    @ColumnInfo(name = "name")
    val name: String? = null,
    @ColumnInfo(name = "dynamodb_id")
    val dynamodb_id: String,
    @ColumnInfo(name = "linked_dynamodb_id")
    val linked_dynamodb_id: String? = null,
    @ColumnInfo(name = "is_suspended")
    val is_suspended: Boolean? = false,
    @ColumnInfo(name = "is_banned")
    val is_banned: Boolean? = false,
    @ColumnInfo(name = "last_login")
    val last_login: String? = null,
    @ColumnInfo(name = "pic")
    val pic: String? = null,
    @ColumnInfo(name = "fbid")
    val fbid: String? = null,
    @ColumnInfo(name = "gender")
    val gender: String? = null,
    @ColumnInfo(name = "rep_actions_blob")
    val rep_actions_blob: String? = null,
    @ColumnInfo(name = "rep_actions")
    val rep_actions: String? = null,
    @ColumnInfo(name = "device_secret")
    val device_secret: String? = null,
    @ColumnInfo(name = "device_ids")
    val device_ids: String? = null,
    @ColumnInfo(name = "sig")
    val sig: String? = null,
    @ColumnInfo(name = "sig_trk")
    val sig_trk: String? = null,
    @ColumnInfo(name = "location")
    val location: String? = null,
    @ColumnInfo(name = "phone")
    val phone: String? = null,
    @ColumnInfo(name = "phone_verified")
    val phone_verified: Boolean? = true,
    @ColumnInfo(name = "verify_code")
    val verify_code: String? = null,
    @ColumnInfo(name = "verify_ts")
    val deviverify_tsce_secret: String? = null,
    @ColumnInfo(name = "verify_tries")
    val verify_tries: Int? = 0,
    @ColumnInfo(name = "posts")
    val posts: String? = null,
    @ColumnInfo(name = "loops")
    val loops: String? = null,
    @ColumnInfo(name = "push_token")
    val push_token: String? = null,
    @ColumnInfo(name = "team")
    val team: String? = null,
    @ColumnInfo(name = "team_number")
    val team_number: String? = null,
    @ColumnInfo(name = "grad_year")
    val grad_year: String? = null,
    @ColumnInfo(name = "club")
    val club: String? = null,
    @ColumnInfo(name = "club_number")
    val club_number: String? = null,
    @ColumnInfo(name = "sport")
    val sport: String? = null,
    @ColumnInfo(name = "position")
    val position: String? = null,
    @ColumnInfo(name = "college")
    val college: String? = null,
    @ColumnInfo(name = "is_camera_authorized")
    val is_camera_authorized: Boolean? = false,
    @ColumnInfo(name = "is_camera_requested")
    val is_camera_requested: Boolean? = true,
    @ColumnInfo(name = "is_camera_rejected")
    val is_camera_rejected: Boolean? = false,
    @ColumnInfo(name = "is_verified_athlete")
    val is_verified_athlete: Boolean? = false,
    @ColumnInfo(name = "camera_tos_agreed_at")
    val camera_tos_agreed_at: String? = null,
    @ColumnInfo(name = "created_at")
    val created_at: String? = null,
    @ColumnInfo(name = "updated_at")
    val updated_at: String? = null

    ) : Parcelable
//TODO: add values from jSon below vvv