package com.itsovertime.overtimecamera.play.network

import io.reactivex.Single
import retrofit2.http.POST

interface Api {
    @POST("//https://admin.itsovertime.com/videos/")
    fun uploadVideo(): Single<UploadResponse>

}