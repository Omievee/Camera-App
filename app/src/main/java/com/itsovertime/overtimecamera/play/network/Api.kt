package com.itsovertime.overtimecamera.play.network

import io.reactivex.Single
import okhttp3.MultipartBody
import retrofit2.http.*

interface Api {

    @POST("/api/writer/videos")
    fun getVideoInstance(@Body request: VideoInstanceRequest): Single<VideoResponse>

    @Multipart
    @POST("/api/writer/videos")
    fun uploadVideo(@Part video: MultipartBody.Part): Single<VideoInstanceRequest>

    @POST("/api/media/upload_token")
    fun uploadToken(): Single<VideoInstanceRequest>
}

