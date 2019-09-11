package com.itsovertime.overtimecamera.play.uploads

import com.itsovertime.overtimecamera.play.model.SavedVideo

interface UploadsInt {

    fun updateAdapter(videos: List<SavedVideo>, data: ProgressData?=null)
    fun displaySettings()
    fun swipe2RefreshIsTrue()
    fun swipe2RefreshIsFalse()
    fun displayWifiReady()
    fun displayNoNetworkConnection()
    fun updateProgressBar(start: Int, end: Int, highQuality: Boolean, clientId: String)


}