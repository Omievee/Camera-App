package com.itsovertime.overtimecamera.play.network

import io.reactivex.Single
import retrofit2.http.*
import java.util.*

interface Api {


    /*  Step 1: Post for Video Instance */
    @POST("/api/writer/videos")
    fun getVideoInstance(@Body request: VideoInstanceRequest): Single<VideoInstanceResponse>

    /*Step 2: Post for video upload token*/
    @POST("/api/media/upload_token")
    fun uploadToken(@Body request: VideoInstanceResponse): Single<TokenResponse>

    /*Step 3:  md5 for given video*/
    @POST("api/uploads")
    fun uploadDataForMd5(@Body token: UploadRequest): Single<EncryptedResponse>

    /*Step 4: Upload for selected video*/

    @POST("api/uploads/{id}/{uploadPartIndex}")
    fun uploadSelectedVideo(@Path("id") id: String, @Path("uploadPartIndex") chunk: Int, @Body data: VideoUploadRequest): Single<VideoUploadResponse>

    /*Events endpoint*/
    @GET("api/events?")
    fun getEventData(@Query("starts_after=") time: Date): Single<EventsResponse?>
}

