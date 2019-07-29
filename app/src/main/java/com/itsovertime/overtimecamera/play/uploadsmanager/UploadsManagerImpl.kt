package com.itsovertime.overtimecamera.play.uploadsmanager

import com.itsovertime.overtimecamera.play.application.OTApplication
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.network.Api
import com.itsovertime.overtimecamera.play.network.VideoResponse
import com.itsovertime.overtimecamera.play.network.VideoInstanceRequest
import com.itsovertime.overtimecamera.play.wifimanager.WifiManager
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*


class UploadsManagerImpl(val context: OTApplication, val api: Api, val wifiManager: WifiManager) : UploadsManager {


    override fun getVideoInstance(): Single<VideoResponse> {
        val request =
            VideoInstanceRequest(
                client_id = UUID.fromString(favoriteVideos[0].id)
//                is_favorite = favoriteVideos[0].is_favorite,
//                is_selfie = favoriteVideos[0].is_selfie,
//                latitude = 0.0,
//                longitude = 0.0
            )

        return api
            .getVideoInstance(request)
            .doOnSuccess {
                println("Successs???>>>>> ${it.video.id}")
            }
            .doOnError {
                println("Error.... ${it.localizedMessage}")
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

    }


    override fun onUploadFavoriteMedQualityVideo(): Single<VideoInstanceRequest> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//        favoriteVideos.forEach {
//            videoFile = File(it.mediumVidPath)
//            videoName = it.id.toString()
//        }
//        val uploadFile = MultipartBody.Part.createFormData(videoName ?: "", videoFile?.name ?: "")
//        return api
//            .getVideoInstance()
//            .doOnSuccess {
//                println("Successs???>>>>> $it")
//            }
//            .subscribeOn(Schedulers.io())
//            .observeOn(AndroidSchedulers.mainThread())


    }


    override fun onUploadFavoriteHighQualityVideo() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onUploadMediumQualityVideo() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onUploadHighQualityVideo() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}


