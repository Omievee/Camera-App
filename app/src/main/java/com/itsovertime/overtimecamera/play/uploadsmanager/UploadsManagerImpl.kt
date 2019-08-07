package com.itsovertime.overtimecamera.play.uploadsmanager

import com.itsovertime.overtimecamera.play.application.OTApplication
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.network.*
import com.itsovertime.overtimecamera.play.wifimanager.WifiManager
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*


class UploadsManagerImpl(val context: OTApplication, val api: Api, val wifiManager: WifiManager) : UploadsManager {
    override fun uploadVideos(data: TokenResponse): Single<UploadRequest> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
        videoList.forEach {
            when (it.is_favorite) {
                true -> favoriteVideos.add(it)
                else -> standardVideos.add(it)
            }
        }

        println("Fave size... ${favoriteVideos.size}")

    }

    //TODO: BUG: lists continue to add  videos each timel...
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


