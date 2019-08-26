package com.itsovertime.overtimecamera.play.network

import com.itsovertime.overtimecamera.play.model.User

class RestrictionsResponse(
    val token: String? = "",
    val data: Data
)

class Data(
    val user: User,
    val iat: Int
)