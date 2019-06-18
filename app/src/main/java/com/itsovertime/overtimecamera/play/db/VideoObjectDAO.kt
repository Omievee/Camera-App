package com.itsovertime.overtimecamera.play.db

import androidx.room.*
import com.itsovertime.overtimecamera.play.model.SavedVideo


@Dao
interface VideoObjectDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveVideo(video: SavedVideo)

    @Query("UPDATE SavedVideo SET is_favorite = :is_favorite WHERE id = :lastID")
    fun setVideoAsFavorite(is_favorite: Boolean, lastID: Int)

    @Query("UPDATE SavedVideo SET is_funny = :is_funny WHERE id = :lastID")
    fun setVideoAsFunny(is_funny: Boolean, lastID: Int)

    @Query("SELECT * FROM SavedVideo")
    fun getVideos(): List<SavedVideo>
}