package com.itsovertime.overtimecamera.play.network

import android.net.Uri
import com.itsovertime.overtimecamera.play.model.SavedVideo
import io.reactivex.Single
import retrofit2.http.*
import java.io.File
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
    @POST("api/uploads/{videoId}/{uploadChunk}")
    fun uploadSelectedVideo(
        @Header("Content-Type") typeHeader: String, @Header("Content-MD5") md5Header: String, @Path("videoId") videoId: String, @Path(
            "uploadChunk"
        ) uploadChunk: Int
    ): Single<VideoUploadResponse>

    /*Events endpoint*/
    @GET("api/events?")
    fun getEventData(@Query("starts_after=") time: Date): Single<EventsResponse?>


}

