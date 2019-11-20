package com.itsovertime.overtimecamera.play.db

import androidx.room.*
import com.itsovertime.overtimecamera.play.model.Event
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.UploadState
import io.reactivex.Single
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

    @Query("UPDATE SavedVideo SET encodedPath = :encodedPath WHERE clientId = :lastID")
    fun updateEncodedPath(encodedPath: String, lastID: String)


    @Query("UPDATE SavedVideo SET md5 = :md5 WHERE clientId = :selectedVideoId")
    fun updateVideoMd5(md5: String, selectedVideoId: String)

    @Query("UPDATE SavedVideo SET uploadId = :uploadId WHERE clientId = :selectedVideoId")
    fun updateUploadId(uploadId: String, selectedVideoId: String)

    @Query("SELECT * FROM SavedVideo")
    fun getVideos(): List<SavedVideo>

    @Query("SELECT * FROM SavedVideo WHERE clientId= :clientId")
    fun getVideoForUpload(clientId: String): SavedVideo

    @Query("SELECT * FROM SavedVideo WHERE clientId= :clientId")
    fun getEncodedVideo(clientId: String): Single<SavedVideo>

    @Query("UPDATE SavedVideo SET uploadState = :uploadState WHERE clientId = :lastID")
    fun updateVideoState(uploadState: UploadState, lastID: String)

    @Query("UPDATE SavedVideo SET mediumUploaded = :mediumUploaded, uploadState =:uploadState WHERE clientId = :lastID")
    fun updateMediumUpload(mediumUploaded: Boolean?, lastID: String, uploadState: UploadState)

    @Query("UPDATE SavedVideo SET highUploaded = :highUploaded , uploadState =:uploadState WHERE clientId = :lastID")
    fun updateHighUpload(highUploaded: Boolean?, lastID: String, uploadState: UploadState)


    @Query("UPDATE SavedVideo SET  tagged= :taggedAthletes  WHERE clientId = :lastID")
    fun updateTaggedAthletesField(taggedAthletes: ArrayList<String>?, lastID: String)

    @Query("UPDATE SavedVideo SET isProcessed = :isProcessed WHERE clientId = :lastID")
    fun updateVideoIsProcessed(isProcessed: Boolean, lastID: String)

    @Query("UPDATE SavedVideo SET  uploadState = :uploadState, uploadId = :uploadId, mediumRes = :mediumVidPath, trimmedVidPath = :trimmedVidPath WHERE clientId = :lastID")
    fun resetUploadDataForVideo(
        uploadState: UploadState,
        uploadId: String,
        mediumVidPath: String?,
        trimmedVidPath: String,
        lastID: String
    )

    @Query("SELECT * FROM SavedVideo")
    fun getVideosForUpload(): Single<List<SavedVideo>>
}