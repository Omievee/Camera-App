package com.overtime.camera.uploads

import com.overtime.camera.model.SavedVideo

interface UploadsInt {

    fun updateAdapter(videos:List<SavedVideo>)
}