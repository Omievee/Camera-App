package com.itsovertime.overtimecamera.play.uploads

import com.itsovertime.overtimecamera.play.videomanager.VideosManager
import io.reactivex.disposables.Disposable

class UploadsPresenter(val view: UploadsFragment, val manager: VideosManager) {

    var managerDisposable: Disposable? = null

    fun onCreate() {
        manager.loadFromDB(view.context ?: return)
    }

    fun onResume() {
        view.swipe2RefreshIsTrue()
        subscribeToVideosFromGallery()
    }


    private fun subscribeToVideosFromGallery() {
        managerDisposable?.dispose()
        managerDisposable = manager
            .subscribeToVideoGallery()
            .map {
                view.updateAdapter(it)
                view.swipe2RefreshIsFalse()
            }.subscribe({
            }, {
                println("throwable: ${it.printStackTrace()}")
            })
    }


    fun onDestroy() {
        managerDisposable?.dispose()
    }

    fun displayBottomSheetSettings() {
        view.displaySettings()
    }
}