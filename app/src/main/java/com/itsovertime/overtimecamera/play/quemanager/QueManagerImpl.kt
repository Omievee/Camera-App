package com.itsovertime.overtimecamera.play.quemanager

import androidx.work.WorkerParameters
import com.itsovertime.overtimecamera.play.application.OTApplication
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.uploadsmanager.VideoUploadWorker
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.util.*

class QueManagerImpl(val context: OTApplication) : QueManager {

    private val subject: BehaviorSubject<Boolean> = BehaviorSubject.create()

    override fun onIsQueReady(): Observable<Boolean> {
        return subject
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    var vidHD: SavedVideo? = null

    var vid: SavedVideo? = null

    var mainList = mutableListOf<SavedVideo>()
    var standardVideoList = mutableListOf<SavedVideo>()
    var favoriteVideoList = mutableListOf<SavedVideo>()
    val standardVideoListHQ = mutableListOf<SavedVideo>()
    var favoriteVideoListHQ = mutableListOf<SavedVideo>()
    override fun onUpdateQueList(video: List<SavedVideo>) {
        println("Made the que start... ${video.size}")
        mainList.clear()


//        standardVideoList.clear()
//        favoriteVideoList.clear()
//        standardVideoListHQ.clear()
//        favoriteVideoListHQ.clear()


        mainList = video.toMutableList()

        mainList.removeIf {
            it.highUploaded
        }

        val iterator = mainList.iterator()
        while (iterator.hasNext()) {
            val vid = iterator.next()
            if (vid.is_favorite && !vid.mediumUploaded) {
                iterator.remove()
                favoriteVideoList.add(0, vid)
            } else if (vid.is_favorite && vid.mediumUploaded) {
                iterator.remove()
                favoriteVideoListHQ.add(0, vid)
            } else if (!vid.is_favorite && !vid.mediumUploaded) {
                iterator.remove()
                standardVideoList.add(0, vid)
            } else if (!vid.is_favorite && vid.mediumUploaded) {
                iterator.remove()
                standardVideoListHQ.add(0, vid)
            }
        }

        if (!standardVideoList.isNullOrEmpty()
            || !favoriteVideoList.isNullOrEmpty()
            || !favoriteVideoListHQ.isNullOrEmpty()
            || !standardVideoListHQ.isNullOrEmpty()
        ) {
            subject.onNext(true)
        }


        println("Size of fave list -- ${favoriteVideoList.size}")
        println("Size of regular list -- ${standardVideoList.size}")
        println("Size of fave HQ list -- ${favoriteVideoListHQ.size}")
        println("Size of regular HQ list -- ${standardVideoListHQ.size}")
    }

    override fun getStandardVideo(): SavedVideo? {
        if (standardVideoList.size > 0) {
            vid = standardVideoList[0]
        }
        vid?.let {
            standardVideoList.remove(it)
        }
        return vid
    }

    override fun getFavoriteVideo(): SavedVideo? {
        if (favoriteVideoList.size > 0) {
            vid = favoriteVideoList[0]
        }
        vid.let {
            favoriteVideoList.remove(it)
        }
        return vid
    }


    override fun getFavoriteHQVideo(): SavedVideo? {
        if (favoriteVideoListHQ.size > 0) {
            vidHD = favoriteVideoListHQ[0]
        }
        vidHD.let {
            favoriteVideoListHQ.remove(it)
        }
        return vidHD
    }

    override fun getStandardHQVideo(): SavedVideo? {
        if (standardVideoListHQ.size > 0) {
            vidHD = standardVideoListHQ[0]
        }
        vidHD.let {
            standardVideoListHQ.remove(it)
        }
        return vidHD
    }


}