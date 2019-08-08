package com.itsovertime.overtimecamera.play.uploadsmanager

import com.itsovertime.overtimecamera.play.application.OTApplication
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.network.*
import com.itsovertime.overtimecamera.play.wifimanager.WifiManager
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.security.NoSuchAlgorithmException


class UploadsManagerImpl(val context: OTApplication, val api: Api, val wifiManager: WifiManager) : UploadsManager {


    override fun uploadVideos(data: TokenResponse): Single<UploadResponse> {
        val request = UploadRequest(md5ForFile(favoriteVideos[0].trimmedVidPath
                ?: ""), data.S3Bucket, data.S3Key, data.AccessKeyId, data.SecretAccessKey, data.SessionToken)
        return api
                .uploadVideo(request)
                .doOnSuccess {
                    println("response id ::: ${it.upload?.id}")
                }
                .doOnError {
                    println("Upload error... ${it.message}")
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())

    }

    override fun getTokenForLowQuality(response: VideoResponse): Single<TokenResponse> {
        return api
                .uploadToken(response)
                .doOnSuccess {
                    println("it?? $it")
                }

                .doOnError {
                    println("Error... ${it.message}")
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    var videoInstanceFromServer: VideoResponse? = null
    override fun getVideoInstance(): Single<VideoResponse> {
        val request =
                VideoInstanceRequest(
                        client_id = UUID.fromString(favoriteVideos[0].id),
                        is_favorite = favoriteVideos[0].is_favorite,
                        is_selfie = favoriteVideos[0].is_selfie,
                        latitude = favoriteVideos[0].latitude ?: 0.0,
                        longitude = favoriteVideos[0].longitude ?: 0.0
                )


        return api
                .getVideoInstance(request)
                .doOnSuccess {
                    videoInstanceFromServer = it
                }
                .doOnError {
                    println("it.... ${it.stackTrace}")
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

    override fun onUploadFavoriteMedQualityVideo(): Single<VideoInstanceRequest> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onUploadMediumQualityVideo() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onUploadHighQualityVideo() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
}


