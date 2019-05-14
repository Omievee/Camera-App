package com.overtime.camera.uploads

import com.overtime.camera.videomanager.VideosManager
import io.reactivex.disposables.Disposable

class UploadsPresenter(val view: UploadsFragment, val manager: VideosManager) {

    var managerDisposable: Disposable? = null

    fun onCreate(){
        manager.loadFromDB(view.context ?: return)
    }
    fun onResume() {
        retrieveVideos()
    }

    private fun retrieveVideos() {
        subscribeToVideosFromGallery()
    }

    private fun subscribeToVideosFromGallery() {
        managerDisposable?.dispose()
        managerDisposable = manager
            .subscribeToVideoGallery()
            .map {
                view.updateAdapter(it)
            }.subscribe({
            }, {
                println("throwable: ${it.printStackTrace()}")
            })
    }


    fun onDestroy() {
        managerDisposable?.dispose()
    }

}