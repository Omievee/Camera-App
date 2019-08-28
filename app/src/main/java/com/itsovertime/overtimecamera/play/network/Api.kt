package com.itsovertime.overtimecamera.play.network

import android.net.Uri
import com.itsovertime.overtimecamera.play.model.SavedVideo
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.MultipartBody
import okhttp3.RequestBody
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
    @Headers("Content-Type: application/octet-stream")
    @POST("api/uploads/{videoId}/{uploadChunk}")
    fun uploadSelectedVideo(
        @Header("Content-MD5") md5Header: String, @Path(
            "videoId"
        ) videoId: String, @Path(
            "uploadChunk"
        ) uploadChunk: Int, @Body file: RequestBody
    ): Observable<VideoUploadResponse>

    /*Events endpoint*/
    @GET("api/events?")
    fun getEventData(@Query("starts_after=") time: Date): Single<EventsResponse?>

    @POST("api/auth/send_code")
    fun verifyNumberForAccessCode(@Body phoneNumber: VerifyNumberRequest): Single<LoginResponse>

    @POST("api/auth/verify_code")
    fun verifyAccessCode(@Body code: VerifyAccessCodeRequest): Single<AccessResponse>

    @POST("api/auth/resend_code")
    fun resendAccessCode(@Body code: VerifyAccessCodeRequest): Single<AccessResponse>

    @GET("api/users/{userId}?nocache=true")
    fun getUser(@Path("userId") userId: String): Single<ApplicationResponse>

    @PUT("api/writer/users/{userId}")
    fun submitApplication(@Path("userId") id: String, @Body request: ApplicationRequest): Single<ApplicationResponse>

    @GET("api/auth/refresh_token")
    fun validateTokenCheckRestrictions(): Single<RestrictionsResponse>

    @PUT("api/writer/users/{userId}/agree_to_tos/{bundleId}")
    fun acceptToTOS(@Path("userId") userId: String, @Path("bundleId") bundle: String): Single<TOSResponse>

}

