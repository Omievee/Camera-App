package com.overtime.camera.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.overtime.camera.model.SavedVideo


@Dao
interface VideoObjectDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveVideo(video: SavedVideo)


    @Query("SELECT * FROM SavedVideo")
    fun getVideos(): List<SavedVideo>

}