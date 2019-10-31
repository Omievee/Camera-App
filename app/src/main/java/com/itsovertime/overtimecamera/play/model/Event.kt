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
    val created_at: String? = "",
    val updated_at: String? = "",
    val isVideographer: Boolean = false,
    val tagged_users: Array<User> = emptyArray(),
    val tagged_teams: Array<Tagged_Teams> = emptyArray(),
    val videographer_ids: Array<String> = emptyArray()
) : Parcelable


@Parcelize
data class Tagged_Teams(
    val id: String?,
    val elasticsearch_id: String?,
    val unstructured_tag: String?,
    val display_name: String?,
    val team_name: String?,
    val organization_name: String?,
    val address: String,
//    val coordinates: Int? = 0,
    val longitude: Double? = 0.0,
    val latitude: Double? = 0.0,
    val item_most_recent: String?,
    val total_loops: Int? = 0,
    val colors: Array<String>? = emptyArray(),
    val resource_type: String? = null,
    val sport_type: String? = null,
    val taggable_athlete_ids: Array<String>? = emptyArray(),
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val taggable_athletes: Array<User>?

) : Parcelable


/*

 "dynamodb_id": "BANZ.F",
              "linked_dynamodb_id": null,
              "is_suspended": false,
              "is_banned": false,
              "last_login": "2018-06-01T20:00:34.000Z",
              "username": "pcorwinlamm",
              "name": "Corwin Lamm",
              "pic": "https://images.itsovertime.com/U9kuHdtTnwu227qa/1531160174315.pic",
              "image_path": null,
              "avatar_id": null,
              "fbid": "10204613652765616",
              "gender": null,
              "device_ids": [
                "-ADE.A",
                "BfAAc.A",
                "CAAO.A",
                "CAGG.A",
                "CAIb.A",
                "DAMT.A",
                "DANS.A",
                "EABM.A",
                "EACH.A",
                "EAGM.A",
                "EAHL.A",
                "EALu.A",
                "FAEf.A",
                "GAJJ.A",
                "HAFG.A",
                "HAIM.A",
                "HAJy.A",
                "HAOW.A",
                "HAOu.A",
                "IAB1.A",
                "IADO.A",
                "IAJD.A",
                "IALE.A",
                "KAC1.A",
                "KAKA.A",
                "MAKI.A",
                "QAIo.A",
                "RAPl.A",
                "WAHK.A"
              ],
              "sig": "https://images.itsovertime.com/U9kuHdtTnwu227qa/1520632087405.sig",
              "sig_trk": "https://images.itsovertime.com/U9kuHdtTnwu227qa/1520632086736.sig_trk",
              "location": "A quiet place with no conflict",
              "phone_verified": true,
              "posts": "43",
              "loops": "5409",
              "team": "overtimetech",
              "team_number": 8,
              "grad_year": 1918,
              "club": null,
              "club_number": null,
              "sport": "100",
              "position": "101",
              "college": null,
              "roles": [
                "mod",
                "useradmin",
                "eventadmin"
              ],
              "is_camera_authorized": true,
              "is_camera_requested": true,
              "is_camera_rejected": false,
              "is_verified_athlete": true,
              "camera_tos_agreed_at": "2019-02-25T18:29:40.671Z",
              "total_coins": 0,
              "total_wagered_coins": 0,
              "total_potential_coins": 0,
              "total_experience_points": 0,
              "current_experience_level_id": null,
              "created_at": "2014-05-09T19:48:10.000Z",
              "updated_at": "2019-09-30T15:40:42.205Z",
              "uuid": "b268bb2c-0c10-457d-82e3-559e84d4c9a2"
*/
