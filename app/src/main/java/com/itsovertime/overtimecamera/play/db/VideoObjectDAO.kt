package com.itsovertime.overtimecamera.play.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.itsovertime.overtimecamera.play.model.SavedVideo


@Dao
interface VideoObjectDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveVideo(video: SavedVideo)


//    @Query("SELECT * FROM SavedVideo")
//    fun set(): List<SavedVideo>

    @Query("SELECT * FROM SavedVideo")
    fun getVideos(): List<SavedVideo>

}