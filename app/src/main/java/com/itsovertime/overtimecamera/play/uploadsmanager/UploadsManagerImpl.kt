package com.itsovertime.overtimecamera.play.uploadsmanager

import android.annotation.SuppressLint
import com.itsovertime.overtimecamera.play.application.OTApplication
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.network.*
import com.itsovertime.overtimecamera.play.utils.Constants
import com.itsovertime.overtimecamera.play.wifimanager.WifiManager
import io.reactivex.Observable
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

    var vid = mutableListOf<SavedVideo>()
    override fun onProcessUploadQue(list: MutableList<SavedVideo>) {
        println("UPDATE QUE LIST --------- ${list.size}")
        if (list.size > vid.size) {
            vid = list
            subject.onNext(vid)
        }
    }

    @Synchronized
    override fun getVideoInstance(video: SavedVideo): Single<VideoInstanceResponse> {
        currentVideo = video
        return api
            .getVideoInstance(
                VideoInstanceRequest(
                    client_id = UUID.fromString(video.clientId),
                    is_favorite = video.is_favorite,
                    is_selfie = video.is_selfie,
                    latitude = video.latitude ?: 0.0,
                    longitude = video.longitude ?: 0.0
                )
            )
            .doOnSuccess {
                println("success.. .? ${it.video}")
            }
            .doOnError {
                println("error... ? ${it.message}")
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    @Synchronized
    override fun getAWSDataForUpload(): Single<TokenResponse> {
        return api
            .uploadToken(VideoSourceRequest(type = Constants.Source))
            .doOnError {
                println("token error ${it.message}")
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }


    @Synchronized
    override fun registerWithMD5(data: TokenResponse): Single<EncryptedResponse> {
        val md5 = md5(File(currentVideo?.mediumRes).readBytes())
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
                println("aws error ${it.message}")
            }
            .subscribeOn(Schedulers.io())
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
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun md5(array: ByteArray): String? {
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
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    override fun writerToServerAfterComplete(
        uploadId: String, S3Key: String, vidWidth: Int, vidHeight: Int, hq: Boolean, vid: SavedVideo
    ): Single<ServerResponse> {
        var path = ""
        val r: ServerRequest
        when (hq) {
            true -> {
                path = vid?.highRes ?: ""
                r = ServerRequest(
                    S3Key = S3Key,
                    source_medium_quality_path = path,
                    source_medium_quality_height = vidHeight,
                    source_medium_quality_width = vidWidth,
                    source_medium_quality_progress = 1.0
                )

            }
            else -> {
                path = vid?.mediumRes ?: ""
                r = ServerRequest(
                    S3Key = S3Key,
                    source_high_quality_path = path,
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
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }


    override fun onUpdateQue(): Observable<MutableList<SavedVideo>> {
        return subject
            .observeOn(Schedulers.io())
            .subscribeOn(AndroidSchedulers.mainThread())
    }
}