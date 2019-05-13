package com.overtime.camera.videomanager

import android.content.Context
import android.database.Cursor
import com.overtime.camera.model.SavedVideo
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import android.provider.MediaStore


class VideosManagerImpl : VideosManager {

    var listOfVideos: MutableList<String>? = null


    override fun loadVideosFromGallery(context: Context) {
        var videocursor: Cursor? = null
        val selection = MediaStore.Video.Media.DATA + " like?";
        val selectionArgs = arrayOf("%OverTime%")
        val projection = arrayOf(MediaStore.Video.VideoColumns.DATA)
        var parameters = arrayOf(MediaStore.Video.Media.ALBUM)
        videocursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            parameters,
            selection,
            selectionArgs,
            MediaStore.Video.Media.DATE_TAKEN + " DESC"
        )

//        if (videocursor != null) {
//            try {
//                videocursor?.moveToFirst()
//                listOfVideos?.add(
//                    (videocursor.getString(
//                        videocursor.getColumnIndexOrThrow(
//
//                        )
//                    ))
//                )
//                videocursor.close()
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//            println("size of file?? ${listOfVideos?.size}")
//        }

        if (videocursor != null) {
            if ( videocursor.moveToFirst() ) {
                do {
                    val index = videocursor.getColumnIndex((MediaStore.Video.Media.DISPLAY_NAME))
                    println("Count.... $index")
                    //listOfVideos?.add(videocursor.getString(index))
                } while (videocursor.moveToNext())
            }
        }
        videocursor.close()
    }


    val subject: BehaviorSubject<SavedVideo> = BehaviorSubject.create()

    override fun subscribeToVideoGallery(): Observable<SavedVideo> {
        return subject
            .subscribeOn(Schedulers.single())
            .observeOn(AndroidSchedulers.mainThread())
    }


}