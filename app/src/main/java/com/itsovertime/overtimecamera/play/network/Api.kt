package com.itsovertime.overtimecamera.play.network

import io.reactivex.Single
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface Api {

    @POST("/api/writer/videos")
    fun getVideoInstance(request: VideoResponse): Single<VideoResponse>

    @Multipart
    @POST("/api/writer/videos")
    fun uploadVideo(@Part video: MultipartBody.Part): Single<VideoResponse>

}

