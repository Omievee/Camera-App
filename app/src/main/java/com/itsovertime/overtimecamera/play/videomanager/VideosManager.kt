package com.itsovertime.overtimecamera.play.videomanager

import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.UploadState
import io.reactivex.Observable
import io.reactivex.Single
import java.io.File

interface VideosManager {


    fun subscribeToVideoGallery(): Observable<List<SavedVideo>>
    fun subscribeToVideoGallerySize(): Observable<Int>
    fun subscribeToNewFavoriteVideoEvent(): Observable<SavedVideo>
    fun subscribeToHDSwitch(): Observable<Boolean>
    fun subscribeToEncodeComplete(): Observable<SavedVideo>
    fun subscribeToNewVideos(): Observable<Boolean>
    fun subscribeToCompletedUploads(): Observable<SavedVideo>
    fun onLoadDb()
    fun onSaveVideoToDb(video: SavedVideo)
    fun onRegisterVideoWithServer(notifyWorker: Boolean, saved: SavedVideo)
    fun onTransCodeVideo(savedVideo: SavedVideo, videoFile: File)
    fun onVideoIsFavorite(isFavorite: Boolean, video: SavedVideo)
    fun onVideoIsFunny(isFunny: Boolean, clientId: String)
    fun videoIsValid(vid: SavedVideo): Boolean
    fun subscribeToResetReasons(): Observable<VideosManagerImpl.ResetReasons>

    fun updateVideoMd5(md5: String, clientId: String)
    fun onUpdateUploadIdInDb(uplaodId: String, savedVideo: SavedVideo, notify:Boolean)
    fun updateVideoStatus(video: SavedVideo, state: UploadState)
    fun onUpdatedTaggedAthletesInDb(taggedAthletesArray: ArrayList<String>, clientId: String)
    fun onResetCurrentVideo(currentVideo: SavedVideo, reason: VideosManagerImpl.RESET, stage: String)
    fun updateMediumUploaded(qualityUploaded: Boolean, clientId: String)
    fun onLoadFFMPEG()
    fun updateHighuploaded(qualityUploaded: Boolean, video: SavedVideo)
    fun onNotifyWorkIsDone(savedVideo: SavedVideo)

    fun onGetVideosForUpload(): Single<List<SavedVideo>>
    fun onGetVideosForUploadScreen(): Single<List<SavedVideo>>

    fun onNotifyHDUploadsTriggered(hd: Boolean)

    fun onUpdateEncodedPath(path: String, clientId: String)
    fun onGetEncodedVideo(clientId: String): Single<SavedVideo>

}