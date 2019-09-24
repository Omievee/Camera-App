package com.itsovertime.overtimecamera.play.quemanager

import com.itsovertime.overtimecamera.play.application.OTApplication
import com.itsovertime.overtimecamera.play.model.SavedVideo
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

class QueManagerImpl(val context: OTApplication) : QueManager {
    private val subject: BehaviorSubject<Boolean> = BehaviorSubject.create()

    override fun onIsQueReady(): Observable<Boolean> {
        return subject
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    var vidHD: SavedVideo? = null
    override fun onGetNextVideoFromMediumList(): SavedVideo? {
        if (highQList.size > 0) {
            vidHD = highQList[0]
        }

        vidHD.let {
            highQList.remove(it)
        }
        return vidHD
    }

    var vid: SavedVideo? = null
    override fun onGetNextVideo(): SavedVideo? {
        if (queList.size > 0) {
            vid = queList[0]
        }

        vid?.let {
            queList.remove(it)
        }
        return vid
    }

    var queList = mutableListOf<SavedVideo>()
    val highQList = mutableListOf<SavedVideo>()
    override fun onUpdateQueList(video: List<SavedVideo>) {
        println("Made the que start... ${video.size}")
        queList.clear()
        highQList.clear()

        if (video != queList) {
            queList = video.toMutableList()
            println("made this .. .${queList.size}")
        }
        queList.sortedBy {
            it.is_favorite
        }
        queList.removeIf {
            it.highUploaded
        }
        val iterator = queList.iterator()
        while (iterator.hasNext()) {
            val vid = iterator.next()
            if (vid.mediumUploaded) {
                iterator.remove()
                highQList.add(vid)
            }
        }

        queList.forEach {
            if (it.mediumUploaded) {
                highQList.add(it)
                queList.remove(it)
            }
        }
        highQList.sortBy {
            it.is_favorite
        }

        if (!queList.isNullOrEmpty() || !highQList.isNullOrEmpty()) {
            subject.onNext(true)
        }
        println("Size of que list -- ${queList.size}")
        println("Size of med list -- ${highQList.size}")
    }

}