package com.overtime.camera.uploads

import android.os.Environment
import com.overtime.camera.videomanager.VideosManager
import io.reactivex.disposables.Disposable

class UploadsPresenter(val view: UploadsFragment, val manager: VideosManager) {

    var managerDisposable: Disposable? = null

    fun onResume() {
        retrieveVideos()
    }

    private fun retrieveVideos() {
        view.context?.let { manager.loadVideosFromGallery(it) }
    }


    fun onDestroy() {
        managerDisposable?.dispose()
    }

}