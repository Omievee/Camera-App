package com.itsovertime.overtimecamera.play.network

import io.reactivex.Single
import okhttp3.MultipartBody
import retrofit2.http.*
import java.util.*

interface Api {

    @POST("/api/writer/videos")
    fun getVideoInstance(@Body request: VideoInstanceRequest): Single<VideoResponse>

    @POST("/api/media/upload_token")
    fun uploadToken(@Body request: VideoResponse): Single<TokenResponse>

    @POST("api/uploads")
    fun uploadVideo(@Body token: UploadRequest): Single<UploadResponse>

    @GET("api/events?")
    fun getEventData(@Query("starts_after=") time: Date): Single<EventsResponse?>
}

