package com.itsovertime.overtimecamera.play.uploadsmanager

import com.itsovertime.overtimecamera.play.application.OTApplication
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.network.*
import com.itsovertime.overtimecamera.play.wifimanager.WifiManager
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import retrofit2.http.FormUrlEncoded
import java.util.*
import java.security.NoSuchAlgorithmException


class UploadsManagerImpl(val context: OTApplication, val api: Api, val wifiManager: WifiManager) : UploadsManager {

    private var MIN_CHUNK_SIZE = 0.5 * 1024
    private var MAX_CHUNK_SIZE = 2 * 1024 * 1024
    private var chunkSize = 1 * 1024
    private var uploadRate: Double = 0.0
    private var time = System.currentTimeMillis()


    override fun getVideoInstance(): Single<VideoInstanceResponse> {
        return api
                .getVideoInstance(
                        VideoInstanceRequest(
                                client_id = UUID.fromString(favoriteVideos[0].id),
                                is_favorite = favoriteVideos[0].is_favorite,
                                is_selfie = favoriteVideos[0].is_selfie,
                                latitude = favoriteVideos[0].latitude ?: 0.0,
                                longitude = favoriteVideos[0].longitude ?: 0.0
                        )
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    var data: TokenResponse? = null
    override fun registerUploadForId(data: TokenResponse): Single<EncryptedResponse> {
        this.data = data
        return api
                .uploadDataForMd5(
                        UploadRequest(
                                md5ForFile(
                                        favoriteVideos[0].trimmedVidPath
                                                ?: ""
                                ), data.S3Bucket, data.S3Key, data.AccessKeyId, data.SecretAccessKey, data.SessionToken
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
        return api
                .uploadSelectedVideo(id = upload.id
                        ?: "", chunk = 0.0, data = VideoUploadRequest(upload.md5))
                .doOnSuccess {
                    println("Response from upload... ? $it")
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    private var favoriteVideos = mutableListOf<SavedVideo>()
    private var standardVideos = mutableListOf<SavedVideo>()

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


