package com.itsovertime.overtimecamera.play.db

import androidx.room.*
import com.itsovertime.overtimecamera.play.model.Event
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.UploadState
import java.util.*


@Dao
interface VideoObjectDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveVideoData(video: SavedVideo)

    @Query("UPDATE SavedVideo SET is_favorite = :is_favorite WHERE clientId = :lastID")
    fun setVideoAsFavorite(is_favorite: Boolean, lastID: String)

    @Query("UPDATE SavedVideo SET is_funny = :is_funny WHERE clientId = :lastID")
    fun setVideoAsFunny(is_funny: Boolean, lastID: String)

    @Query("UPDATE SavedVideo SET mediumRes = :mediumVidPath WHERE clientId = :lastID")
    fun updateMediumQualityPath(mediumVidPath: String, lastID: String)

    @Query("UPDATE SavedVideo SET is_selfie = :is_selfie WHERE clientId = :lastID")
    fun updateVideoIsSelfie(is_selfie: Boolean, lastID: String)

    @Query("UPDATE SavedVideo SET trimmedVidPath = :trimmedVidPath WHERE clientId = :lastID")
    fun updateTrimVideoPath(trimmedVidPath: String, lastID: String)

    @Query("UPDATE SavedVideo SET md5 = :md5 WHERE clientId = :selectedVideoId")
    fun updateVideoMd5(md5: String, selectedVideoId: String)

    @Query("UPDATE SavedVideo SET uploadId = :uploadId WHERE clientId = :selectedVideoId")
    fun updateUploadId(uploadId: String, selectedVideoId: String)

    @Query("UPDATE SavedVideo SET id = :videoInstanceId WHERE clientId = :selectedVideoId")
    fun updateVideoInstanceId(videoInstanceId: String, selectedVideoId: String)

    @Query("SELECT * FROM SavedVideo")
    fun getVideos(): List<SavedVideo>

    @Query("SELECT * FROM SavedVideo WHERE clientId= :clientId")
    fun getVideoForUpload(clientId: String): SavedVideo

    @Query("UPDATE SavedVideo SET uploadState = :uploadState WHERE clientId = :lastID")
    fun updateVideoState(uploadState: UploadState, lastID: String)

    @Query("UPDATE SavedVideo SET isProcessed = :isProcessed WHERE clientId = :lastID")
    fun updateVideoIsProcessed(isProcessed: Boolean, lastID: String)
}