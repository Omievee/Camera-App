package com.itsovertime.overtimecamera.play.network

import com.itsovertime.overtimecamera.play.model.User

class LoginResponse(val phone: String) {
}

class AccessResponse(val token: String) {
}

class ApplicationResponse(val user: User) {}

class ApplicationRequest(
    val is_camera_requested: Boolean = true,
    val name: String? = "",
    val username: String? = "",
    val location: String? = ""
) {}