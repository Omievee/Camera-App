package com.itsovertime.overtimecamera.play.db

import androidx.room.*
import com.itsovertime.overtimecamera.play.model.SavedVideo


@Dao
interface VideoObjectDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveVideo(video: SavedVideo)

    @Query("UPDATE SavedVideo SET isFavorite = :isFave WHERE id = :lastID")
    fun setVideoAsFavorite(isFave: Boolean, lastID: Int)

    @Query("SELECT * FROM SavedVideo")
    fun getVideos(): List<SavedVideo>
}