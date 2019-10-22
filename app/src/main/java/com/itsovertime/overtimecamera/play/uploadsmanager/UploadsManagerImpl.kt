package com.itsovertime.overtimecamera.play.uploadsmanager

import android.annotation.SuppressLint
import android.icu.text.SimpleDateFormat
import com.itsovertime.overtimecamera.play.application.OTApplication
import com.itsovertime.overtimecamera.play.model.Event
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.network.*
import com.itsovertime.overtimecamera.play.utils.Constants
import com.itsovertime.overtimecamera.play.wifimanager.WifiManager
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import okhttp3.MediaType
import okhttp3.RequestBody
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*


class UploadsManagerImpl(
    val context: OTApplication,
    val api: Api,
    val manager: WifiManager
) : UploadsManager {

    private val subject: BehaviorSubject<MutableList<SavedVideo>> = BehaviorSubject.create()
    private var currentVideo: SavedVideo? = null

    @Synchronized
    override fun getVideoInstance(video: SavedVideo?): Observable<VideoInstanceResponse> {
        currentVideo = video
        println("Getting insacneeeeeee...")
        return api
            .getVideoInstance(
                VideoInstanceRequest(
                    client_id = UUID.fromString(video?.clientId),
                    is_favorite = video?.is_favorite ?: false,
                    is_selfie = video?.is_selfie ?: false,
                    latitude = video?.latitude ?: 0.0,
                    longitude = video?.longitude ?: 0.0,
                    event_id = video?.event_id,
                    address = video?.address,
                    duration_in_hours = video?.duration_in_hours,
                    max_video_length = video?.max_video_length
                )
            ).observeOn(AndroidSchedulers.mainThread())
    }

    @Synchronized
    override fun getAWSDataForUpload(): Observable<TokenResponse> {
        return api
            .uploadToken(VideoSourceRequest(type = Constants.Source))
            .doOnError {
                println("token error ${it.message}")
            }
            .observeOn(AndroidSchedulers.mainThread())
    }


    @Synchronized
    override fun registerWithMD5(
        data: TokenResponse,
        hdReady: Boolean
    ): Observable<EncryptedResponse> {
        val md5: String = when (hdReady) {
            true -> {
                when (currentVideo?.trimmedVidPath) {
                    null -> md5(File(currentVideo?.highRes).readBytes()) ?: ""
                    "" -> md5(File(currentVideo?.highRes).readBytes()) ?: ""
                    else -> md5(File(currentVideo?.trimmedVidPath).readBytes()) ?: ""
                }
            }
            false -> md5(File(currentVideo?.mediumRes).readBytes()) ?: ""
        }
        return api
            .uploadDataForMd5(
                UploadRequest(
                    md5,
                    data.AccessKeyId,
                    data.SecretAccessKey,
                    data.SessionToken,
                    data.S3Bucket,
                    data.S3Key
                )
            )
            .doOnError {

            }
            .observeOn(AndroidSchedulers.mainThread())
    }


    var upload: Upload? = null
    @SuppressLint("CheckResult")
    @Synchronized
    override fun uploadVideoToServer(
        upload: Upload,
        array: ByteArray,
        chunk: Int
    ): Observable<retrofit2.Response<VideoUploadResponse>> {
        val request = RequestBody.create(
            MediaType.parse("application/octet-stream"),
            array
        )
        return api
            .uploadSelectedVideo(
                md5Header = md5(array) ?: "",
                videoId = upload.id ?: "",
                uploadChunk = chunk,
                file = request
            )
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun md5(array: ByteArray): String? {
        println("MD5 HASH STARTED")
        val m = MessageDigest.getInstance("MD5")
        m.reset()
        m.update(array)
        val digest = m.digest()
        val bigInt = BigInteger(1, digest)
        var hashtext = bigInt.toString(16)
        while (hashtext.length < 32) {
            hashtext = "0$hashtext"
        }
        return hashtext
    }

    override fun onCompleteUpload(uploadId: String): Observable<retrofit2.Response<CompleteResponse>> {
        return api
            .checkStatusForComplete(uploadId, CompleteRequest(async = true))
            .observeOn(AndroidSchedulers.mainThread())
    }

    override fun writerToServerAfterComplete(
        uploadId: String, S3Key: String, vidWidth: Int, vidHeight: Int, hq: Boolean, vid: SavedVideo
    ): Observable<ServerResponse> {
        val r: ServerRequest
        when (hq) {
            false -> {
                r = ServerRequest(
                    source_medium_quality_path = S3Key,
                    source_medium_quality_height = vidHeight,
                    source_medium_quality_width = vidWidth,
                    source_medium_quality_progress = 1.0
                )
            }
            else -> {
                r = ServerRequest(
                    source_high_quality_path = S3Key,
                    source_high_quality_height = vidHeight,
                    source_high_quality_width = vidWidth,
                    source_high_quality_progress = 1.0
                )
            }
        }
        return api
            .writeToSeverAfterComplete(
                uploadId = uploadId,
                request = r
            )
            .observeOn(AndroidSchedulers.mainThread())
    }

}