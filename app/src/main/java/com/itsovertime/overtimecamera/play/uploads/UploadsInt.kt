package com.itsovertime.overtimecamera.play.uploads

import com.itsovertime.overtimecamera.play.model.SavedVideo

interface UploadsInt {

    fun updateAdapter(videos: List<SavedVideo>)
    fun displaySettings()
    fun swipe2RefreshIsTrue()
    fun swipe2RefreshIsFalse()
}