package com.overtime.camera.videomanager

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.overtime.camera.model.SavedVideo
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import android.provider.MediaStore
import android.util.Log
import com.overtime.camera.db.AppDatabase
import kotlin.random.Random


class VideosManagerImpl : VideosManager {

    private val subject: BehaviorSubject<List<SavedVideo>> = BehaviorSubject.create()

    @SuppressLint("CheckResult")
    override fun saveVideoToDB(context: Context, filePath: String) {
        println("SAVING")
        val db = AppDatabase.getAppDataBase(context = context)
        Observable.fromCallable {
            val video = SavedVideo(vidPath = filePath)
            val videoDao = db?.videoDao()
            with(videoDao) {
                this?.saveVideo(video)
                println("SAVING 2>>>>>>>>")
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorReturn {
                it.printStackTrace()
            }
            .subscribe({
                loadFromDB(context)
            }, {
                it.printStackTrace()
            })
    }

    @SuppressLint("CheckResult")
    override fun loadFromDB(context: Context) {
        val listOfVideos = mutableListOf<SavedVideo>()
        println("LOADING")
        val db = AppDatabase.getAppDataBase(context = context)
        Observable.fromCallable {
            db?.videoDao()?.getVideos()
        }.map {
            it.forEach {
                listOfVideos.add(it)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                subject.onNext(listOfVideos)
            }, {
                it.printStackTrace()
            })
    }


    override fun subscribeToVideoGallery(): Observable<List<SavedVideo>> {
        return subject
            .subscribeOn(Schedulers.single())
            .observeOn(AndroidSchedulers.mainThread())
    }
}


//        val uri: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
//        val videoCursor: Cursor?
//        var videoPath: String?
//        val orderBy = MediaStore.Images.Media.DATE_TAKEN
//
////        val selectionArgs = arrayOf("%OverTime%")
//        val projection = arrayOf(
//            MediaStore.MediaColumns.DATA,
//            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
//            MediaStore.Video.Media._ID,
//            MediaStore.Video.Thumbnails.MINI_KIND.toString()
//        )
//        videoCursor = context.contentResolver.query(
//            uri,
//            projection,
//            null,
//            null,
//            "$orderBy DESC"
//        )
//
//
//        val dataColumn = videoCursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
//        val folderColumn = videoCursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
//        val thumbnail = videoCursor.getColumnIndexOrThrow(MediaStore.Video.Thumbnails.MINI_KIND.toString())
//
//        val listOfVideos = mutableListOf<SavedVideo>()
//        if (videoCursor != null) {
//            while (videoCursor.moveToNext()) {
//                videoPath = videoCursor.getString(dataColumn)
//                Log.e("Column", videoPath)
//                Log.e("Folder", videoCursor.getString(folderColumn))
//                Log.e("thum", videoCursor.getString(thumbnail))
//
//                if (videoCursor.getString(folderColumn) == "Overtime") {
//                    val savedVideos = SavedVideo(vidPath = videoPath)
//                    listOfVideos.add(savedVideos)
//
//                }
//            }
//            videoCursor.close()
//        }