package com.itsovertime.overtimecamera.play.network

import io.reactivex.Single
import retrofit2.http.Header
import retrofit2.http.POST

interface Api {

    companion object {
        const val HEADER = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyIjp7ImlkIjoiOTQ4OS5BIiwicm9sZXMiOltdLCJ1c2VybmFtZSI6InByZXNlbnQtc3F1aXJyZWwifSwiaWF0IjoxNTU3NzU2NzA0fQ.bG1818-QymNw6cTssZtbYo8E6jIHLxD42WpRYbx9vng"
    }

    @POST("videos")
    fun uploadVideo(@Header(HEADER) token: String): Single<UploadResponse>


}
