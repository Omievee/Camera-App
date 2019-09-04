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
import java.io.*
import java.util.*
import okhttp3.RequestBody
import java.nio.file.Files
import kotlin.math.ceil
import java.math.BigInteger
import java.security.MessageDigest


class UploadsManagerImpl(
    val context: OTApplication,
    val api: Api,
    val manager: WifiManager
) : UploadsManager {


    var faveList: MutableList<SavedVideo>? = mutableListOf()
    var standardList: MutableList<SavedVideo>? = mutableListOf()

    private var MIN_CHUNK_SIZE = 0.5 * 1024
    private var MAX_CHUNK_SIZE = 2 * 1024 * 1024
    private var chunkSize = 1 * 1024
    private var uploadRate: Double = 0.0
    private var time = System.currentTimeMillis()
    private val subject: BehaviorSubject<SavedVideo> = BehaviorSubject.create()
    private val subjectState: BehaviorSubject<UploadState> = BehaviorSubject.create()


    override fun onUpdateFavoriteVideosList(favoriteVideos: MutableList<SavedVideo>) {
        println("FAVE LIST UPDATE ::: $favoriteVideos")
        faveList = favoriteVideos
    }

    override fun onUpdateStandardVideosList(standardVideos: MutableList<SavedVideo>) {
        standardList = standardVideos
    }

    override fun beginUploadProcess() {
        println("Begin PRocess...")
        when (!faveList.isNullOrEmpty()) {
            true -> {
                faveList?.forEach {
                    synchronized(this) {
                        println("Begin PRocess... ${it.clientId}")
                        if (it.uploadState == UploadState.QUEUED) {
                            currentVideo = it
                            if (currentVideo != null) {
                                subject.onNext(it)
                                subjectState.onNext(UploadState.REGISTERING)
                            } else return
                        }
                    }
                }
            }
            false -> {
                standardList?.forEach {
                    //                                        synchronized(this) {
//                        getVideoInstance(it)
//                    }
                }
            }
        }
    }


    var currentVideo: SavedVideo? = null
    override fun getVideoInstance(): Single<VideoInstanceResponse> {
        println("current video... $currentVideo")
        if (currentVideo == null) {

        }
        return api
            .getVideoInstance(
                VideoInstanceRequest(
                    client_id = UUID.fromString(currentVideo?.clientId),
                    is_favorite = currentVideo?.is_favorite ?: false,
                    is_selfie = currentVideo?.is_selfie ?: false,
                    latitude = currentVideo?.latitude ?: 0.0,
                    longitude = currentVideo?.longitude ?: 0.0
                )
            )
            .doOnSuccess {
                subjectState.onNext(UploadState.REGISTERED)
                println("success from instance...")
            }
            .doOnError {
                println("instance error.... ${it.message}")
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    override fun getAWSDataForUpload(response: VideoInstanceResponse): Single<TokenResponse> {
        println("AWS Data response......")
        return api
            .uploadToken(response)
            .doOnSuccess {
                println("aws data success....")
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }


    override fun registerWithMD5(data: TokenResponse): Single<EncryptedResponse> {
        val md5 = hexToString(File(currentVideo?.mediumVidPath).readBytes())
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
            .doOnSuccess {
                println("register w/ md5 success")
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }


    var upload: Upload? = null
    var uploadChunkIndex: Int = 0
    var array: Array<ByteArray> = emptyArray()
    var offSet: Int = 0

    override fun prepareVideoForUpload(upload: Upload) {
        println("preparing upload... $upload...")
        if (currentVideo?.uploadState == UploadState.QUEUED) {
            subjectState.onNext(UploadState.UPLOADING_MEDIUM)
        }


        println("CUrrent id's :::: ${upload.id} ${currentVideo?.uploadId}")
//        if (upload.id != currentVideo?.uploadId) {
//            println("Ids didnt match........")
//            return
//        }

        this.upload = upload
        array = when (currentVideo?.uploadState) {
            UploadState.UPLOADING_MEDIUM -> breakFileIntoChunks(
                File(currentVideo?.mediumVidPath),
                chunkSize
            )
            UploadState.UPLOADED_MEDIUM -> breakFileIntoChunks(
                File(currentVideo?.trimmedVidPath),
                chunkSize
            )
            else -> {
                emptyArray()
            }
        }
        offSet = array.size
        println("THIS DATA IS..... $array + ${array[0]} ++ ${array[1]} ${currentVideo?.uploadState}")
        uploadVideoToServer(array, uploadChunkIndex)
    }

    lateinit var request: RequestBody

    @SuppressLint("CheckResult")
    @Synchronized
    override fun uploadVideoToServer(data: Array<ByteArray>, chunkToUpload: Int) {
        subjectState.onNext(UploadState.UPLOADING_MEDIUM)

        println("<<<<<<<<<<< UPLOADING TO SERVER >>>>>>>>>>>>>>>... $data")
        println("Offset ... $offSet")
//        do {
//            println("inside Do.... $$$$$$$$ ${data.size}")
//            data?.forEach { it ->
//                request = RequestBody.create(
//                    MediaType.parse("application/octet-stream"),
//                    it
//                )
//                synchronized(this) {
//                    Single.fromCallable {
//                        api.uploadSelectedVideo(
//                            md5Header = hexToString(it),
//                            videoId = upload?.id ?: return@fromCallable,
//                            uploadChunk = chunkToUpload,
//                            file = request
//                        ).doOnSuccess { response ->
//                            println("made the success...... ")
//                            println("Success From Video Upload....... ${response.success}")
//                        }
//                    }.subscribeOn(Schedulers.io())
//                        .observeOn(AndroidSchedulers.mainThread())
//
//                        .doOnError {
//                            println("Error from upload ${it.message}")
//                        }
//                }
//            }
//        } while (data.size in 1 until offSet - 1)

    }

    var isUploadComplete: Boolean? = false


    override fun onNotifyStateOfCurrentVideo(): Observable<UploadState> {
        return subjectState
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }


    override fun onCurrentFileBeingUploaded(): Observable<SavedVideo> {
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

    private fun hexToString(byte: ByteArray): String {
        val md5 = MessageDigest.getInstance("MD5")
        return BigInteger(
            1,
            md5.digest(byte)
        ).toString(16)
    }

}