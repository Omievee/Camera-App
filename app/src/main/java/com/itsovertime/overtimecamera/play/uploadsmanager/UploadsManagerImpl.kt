package com.itsovertime.overtimecamera.play.uploadsmanager

import android.annotation.SuppressLint
import com.itsovertime.overtimecamera.play.application.OTApplication
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.UploadState
import com.itsovertime.overtimecamera.play.network.*
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
import java.nio.file.Files
import java.security.MessageDigest
import java.util.*
import kotlin.math.ceil


class UploadsManagerImpl(
    val context: OTApplication,
    val api: Api,
    val manager: WifiManager
) : UploadsManager {
    private var MIN_CHUNK_SIZE = 0.5 * 1024
    private var MAX_CHUNK_SIZE = 2 * 1024 * 1024
    private var chunkSize = 1 * 1024
    private var uploadRate: Double = 0.0
    private var time = System.currentTimeMillis()


    override fun onProcessUploadQue(list: MutableList<SavedVideo>) {
        println("Updating Que..... ${list.size}")
        subject.onNext(list)
    }

    override fun resetUploadStateForCurrentVideo() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    private val subject: BehaviorSubject<List<SavedVideo>> = BehaviorSubject.create()

    override fun beginUploadProcess() {

    }


    private var currentVideo: SavedVideo? = null


    @Synchronized
    override fun getVideoInstance(video: SavedVideo): Single<VideoInstanceResponse> {
        currentVideo = video
        return api
            .getVideoInstance(
                VideoInstanceRequest(
                    client_id = UUID.fromString(video?.clientId),
                    is_favorite = video?.is_favorite,
                    is_selfie = video?.is_selfie,
                    latitude = video?.latitude ?: 0.0,
                    longitude = video?.longitude ?: 0.0
                )
            )
            .doOnSuccess {
                //                subject.onNext(
//                    CurrentVideoUpload(
//                        currentVideo,
//                        UploadState.REGISTERED
//                    )
//                )
            }
            .doOnError {
                println("instance error.... ${it.message}")
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    @Synchronized
    override fun getAWSDataForUpload(response: VideoInstanceResponse): Single<TokenResponse> {
        println("AWS Data response......")
        return api
            .uploadToken(response)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    @Synchronized
    override fun registerWithMD5(data: TokenResponse): Single<EncryptedResponse> {
        println("register... $currentVideo")

        val md5 = hexToString(File(currentVideo?.mediumRes).readBytes())
        println("Register with MD5 $md5...")
        return api
            .uploadDataForMd5(
                UploadRequest(
                    md5,
                    data.S3Bucket,
                    data.S3Key,
                    data.AccessKeyId,
                    data.SecretAccessKey,
                    data.SessionToken
                )
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }


    var upload: Upload? = null
    var uploadChunkIndex: Int = 0
    var array: Array<ByteArray> = emptyArray()
    var offSet: Int = 0

    @Synchronized
    override fun prepareVideoForUpload(upload: Upload, savedVideo: SavedVideo) {
        //uploadVideoToServer(array, uploadChunkIndex)
    }

    //        array = when (savedVideo?.uploadState) {
//            UploadState.UPLOADING_MEDIUM ->
//            UploadState.UPLOADED_MEDIUM -> breakFileIntoChunks(
//                File(savedVideo?.trimmedVidPath),
//                chunkSize
//            )
//            else -> {
//                emptyArray()
//            }
//        }
    lateinit var request: RequestBody

    @SuppressLint("CheckResult")
    @Synchronized
    override fun uploadVideoToServer(
        upload: Upload,
        savedVideo: SavedVideo
    ): Single<VideoUploadResponse> {
//        if (upload.id != savedVideo?.uploadId) {
//            println("Ids didnt match........")
//        } else println("matchy matchy...")

        array = breakFileIntoChunks(
            File(savedVideo?.mediumRes),
            chunkSize
        )
        offSet = array.size

        request = RequestBody.create(
            MediaType.parse("application/octet-stream"),
            array[0]
        )
        return api
            .uploadSelectedVideo(
                md5Header = hexToString(array[0]),
                videoId = upload?.id ?: "",
                uploadChunk = 0,
                file = request
            )
            .doOnSuccess {
                println("SUCCES FROM UPLOAD::::::: $it")
            }
            .doOnError {
                println("FAILURE FROM UPLOAD ::::: ${it.message}")
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
//        synchronized(this) {
//            Single.fromCallable {
//                api.uploadSelectedVideo(
//
//                ).doOnSuccess { response ->
//                    println("made the success...... ")
//                    println("Success From Video Upload....... ${response.success}")
//                }.doOnError {
//                    println("Error from upload ${it.message}")
//                }
//            }.subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe({
//
//                }, {
//                    it.printStackTrace()
//
//                })
//
//        }
    }

    override fun onUpdatedQue(): Observable<List<SavedVideo>> {
        return subject
            .observeOn(Schedulers.io())
            .subscribeOn(AndroidSchedulers.mainThread())
    }

    private fun breakFileIntoChunks(file: File, size: Int): Array<ByteArray> {
        return divideArray(
            Files.readAllBytes(file.toPath()),
            size
        )
    }

    @Synchronized
    private fun divideArray(source: ByteArray, chunksize: Int): Array<ByteArray> {
        val ret =
            Array(ceil(source.size / chunksize.toDouble()).toInt()) { ByteArray(chunksize) }
        var start = 0
        for (i in ret.indices) {
            if (start + chunksize > source.size) {
                System.arraycopy(source, start, ret[i], 0, source.size - start)
            } else {
                System.arraycopy(source, start, ret[i], 0, chunksize)
            }
            start += chunksize
        }
        return ret
    }

    @Synchronized
    private fun hexToString(byte: ByteArray): String {
        val md5 = MessageDigest.getInstance("MD5")
        return BigInteger(
            1,
            md5.digest(byte)
        ).toString(16)
    }

}

class CurrentVideoUpload(val video: SavedVideo? = null, val state: UploadState? = null)
