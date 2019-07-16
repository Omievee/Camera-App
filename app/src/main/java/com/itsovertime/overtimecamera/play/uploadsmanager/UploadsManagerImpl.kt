package com.itsovertime.overtimecamera.play.uploadsmanager

import android.net.NetworkInfo
import com.itsovertime.overtimecamera.play.application.OTApplication
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.network.Api
import com.itsovertime.overtimecamera.play.wifimanager.WifiManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

class UploadsManagerImpl(val context: OTApplication, val api: Api, val wifiManager: WifiManager) : UploadsManager {

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

    override fun onUploadFavoriteMedQualityVideo() {

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