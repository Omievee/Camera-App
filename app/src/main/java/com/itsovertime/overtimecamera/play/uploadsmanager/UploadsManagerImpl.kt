package com.itsovertime.overtimecamera.play.uploadsmanager

import android.annotation.SuppressLint
import com.itsovertime.overtimecamera.play.application.OTApplication
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.UploadState
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


    private val subject: BehaviorSubject<List<SavedVideo>> = BehaviorSubject.create()
    private var currentVideo: SavedVideo? = null

    override fun onProcessUploadQue(list: MutableList<SavedVideo>) {
        subject.onNext(list)
    }

    override fun resetUploadStateForCurrentVideo() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }


    fun md5(plaintext: ByteArray): String? {
        val m = MessageDigest.getInstance("MD5")
        m.reset()
        m.update(plaintext)
        val digest = m.digest()
        val bigInt = BigInteger(1, digest)
        var hashtext = bigInt.toString(16)
        while (hashtext.length < 32) {
            hashtext = "0$hashtext"
        }
        return hashtext
    }

    var upload: Upload? = null
    lateinit var request: RequestBody
    @SuppressLint("CheckResult")
    @Synchronized
    override fun uploadVideoToServer(
        upload: Upload,
        array: ByteArray,
        chunk: Int
    ): Observable<retrofit2.Response<VideoUploadResponse>> {
//        if (upload.id != savedVideo?.uploadId) {
//            println("Ids didnt match........")
//        } else println("matchy matchy...")
        request = RequestBody.create(
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
            .doOnNext {
                it.code()
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    override fun onCompleteUpload(uploadId: String): Single<CompleteResponse> {
        return api
            .checkStatusForComplete(uploadId, CompleteRequest(async = true))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    override fun writerToServerAfterComplete(): Single<ServerResponse> {
        return api
            .writeToSeverAfterComplete(uploadId = "", request = ServerRequest())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }


    override fun onUpdatedQue(): Observable<List<SavedVideo>> {
        return subject
            .observeOn(Schedulers.io())
            .subscribeOn(AndroidSchedulers.mainThread())
    }
}

class CurrentVideoUpload(val video: SavedVideo? = null, val state: UploadState? = null)
