package com.itsovertime.overtimecamera.play.uploadsmanager

import android.annotation.SuppressLint
import android.net.Uri
import com.itsovertime.overtimecamera.play.application.OTApplication
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.network.*
import com.itsovertime.overtimecamera.play.wifimanager.WifiManager
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import okhttp3.MediaType
import java.io.*
import java.security.NoSuchAlgorithmException
import java.util.*
import okhttp3.RequestBody
import okhttp3.MultipartBody
import com.facebook.common.file.FileUtils


class UploadsManagerImpl(
    val context: OTApplication,
    val api: Api,
    val manager: WifiManager
) : UploadsManager {


    override fun onCurrentVideoId(): Observable<String> {
        return subject
            .observeOn(Schedulers.io())
            .subscribeOn(AndroidSchedulers.mainThread())
    }

    private var MIN_CHUNK_SIZE = 0.5 * 1024
    private var MAX_CHUNK_SIZE = 2 * 1024 * 1024
    private var chunkSize = 1 * 1024
    private var uploadRate: Double = 0.0
    private var time = System.currentTimeMillis()
    private val subject: BehaviorSubject<String> = BehaviorSubject.create()

    override fun getVideoInstance(): Single<VideoInstanceResponse> {
        subject.onNext(favoriteVideos[0].clientId)
        return api
            .getVideoInstance(
                VideoInstanceRequest(
                    client_id = UUID.fromString(favoriteVideos[0].clientId),
                    is_favorite = favoriteVideos[0].is_favorite,
                    is_selfie = favoriteVideos[0].is_selfie,
                    latitude = favoriteVideos[0].latitude ?: 0.0,
                    longitude = favoriteVideos[0].longitude ?: 0.0
                )
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }


    override fun registerUploadForId(data: TokenResponse): Single<EncryptedResponse> {
        val md5 = md5ForFile(favoriteVideos[0].trimmedVidPath ?: "")

        return api
            .uploadDataForMd5(
                UploadRequest(
                    md5, data.S3Bucket, data.S3Key, data.AccessKeyId, data.SecretAccessKey, data.SessionToken
                )
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    override fun getAWSDataForUpload(response: VideoInstanceResponse): Single<TokenResponse> {
        return api
            .uploadToken(response)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    override fun uploadVideo(upload: Upload): Single<VideoUploadResponse> {
        val file = splitFile(File(favoriteVideos[0].trimmedVidPath))[0]

        val requestFile = RequestBody.create(
            MediaType.parse("application/octet-stream"), file
        )
        val requestMd5 = RequestBody.create(
            MediaType.parse(context.contentResolver.getType(Uri.parse(upload.md5 ?: "")) ?: ""), upload.md5 ?: ""
        )

        val body =
            MultipartBody.Part.createFormData("data", file.name, requestFile)

        val md5 = RequestBody.create(
            MultipartBody.FORM, upload.md5 ?: ""
        )

        return api
            .uploadSelectedVideo(
                md5Header = upload.md5 ?: "",
                typeHeader = "application/octet-stream",
                videoId = upload.id ?: "",
                uploadChunk = 0,
                description = md5,
                file = body
            )

            .doOnSuccess {
                println("Response from upload... ? $it")
            }
            .doOnError {
                println("Throwable... ${it.localizedMessage}")
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    private var favoriteVideos = mutableListOf<SavedVideo>()
    private var standardVideos = mutableListOf<SavedVideo>()

    @Throws(IOException::class)
    fun splitFile(f: File): List<File> {
        var partCounter = 1
        val result = arrayListOf<File>()
        val sizeOfFiles = 1024 * 1024// 1MB
        val buffer = ByteArray(sizeOfFiles) // create a buffer of bytes sized as the one chunk size
        val bis = BufferedInputStream(FileInputStream(f))
        val name = f.name
        println("byte.. ${(bis.read(buffer)) > 0}")
        while ((bis.read(buffer)) > 0) {
            val newFile = File(
                f.parent,
                name + "." + String.format("%03d", partCounter++)
            ) // naming files as <inputFileName>.001, <inputFileName>.002, ...
            val out = FileOutputStream(newFile)
            out.write(
                buffer,
                0,
                chunkSize
            )//tmp is chunk size. Need it for the last chunk, which could be less then 1 mb.
            result.add(newFile)
        }
        return result
    }


    override fun onReadyVideosForUpload(videoList: MutableList<SavedVideo>) {

        favoriteVideos.clear()
        standardVideos.clear()
        videoList.forEach {
            when (it.is_favorite) {
                true -> favoriteVideos.add(it)
                else -> standardVideos.add(it)
            }
        }
    }

    private fun md5ForFile(s: String): String {
        val MD5 = "MD5"
        try {
            // Create MD5 Hash
            val digest = java.security.MessageDigest
                .getInstance(MD5)
            digest.update(s.toByteArray())
            val messageDigest = digest.digest()
            // Create Hex String
            val hexString = StringBuilder()
            for (aMessageDigest in messageDigest) {
                var h = Integer.toHexString(0xFF and aMessageDigest.toInt())
                while (h.length < 2)
                    h = "0$h"
                hexString.append(h)
            }
            return hexString.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return ""
    }

    override fun onUploadFavoriteMedQualityVideo(): Single<VideoInstanceRequest> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onUploadMediumQualityVideo() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onUploadHighQualityVideo() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}


