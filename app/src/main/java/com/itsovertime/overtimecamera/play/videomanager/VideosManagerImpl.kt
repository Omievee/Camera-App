package com.itsovertime.overtimecamera.play.videomanager

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.ParcelFileDescriptor
import com.itsovertime.overtimecamera.play.model.SavedVideo
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import com.itsovertime.overtimecamera.play.db.AppDatabase
import com.itsovertime.overtimecamera.play.network.UploadResponse
import io.reactivex.Single
import java.io.File
import java.util.*
import android.widget.Toast
import android.R.attr.data
import android.util.Log
import net.ypresto.androidtranscoder.MediaTranscoder
import net.ypresto.androidtranscoder.format.MediaFormatStrategyPresets
import java.io.FileNotFoundException
import java.util.concurrent.Future
import android.R.attr.data
import android.content.ContentResolver
import android.net.Uri
import java.lang.Exception
import android.R.attr.data


class VideosManagerImpl() : VideosManager {

    override fun transcodeVideo(context: Context, videoFile: File) {
        val file = Uri.fromFile(videoFile)
        println("file?? $file")

        val parcelFileDescriptor = context.contentResolver.openFileDescriptor(file, "rw")
        val fileDescriptor = parcelFileDescriptor?.fileDescriptor

        val listener = object : MediaTranscoder.Listener {
            override fun onTranscodeProgress(progress: Double) {
                println("progress ::: $progress")
            }

            override fun onTranscodeCanceled() {
                println("canceled")
            }

            override fun onTranscodeFailed(exception: Exception?) {
                exception?.printStackTrace()
            }

            override fun onTranscodeCompleted() {
                println("complete :::")
            }
        }
        MediaTranscoder.getInstance().transcodeVideo(
            fileDescriptor, videoFile.absolutePath,
            MediaFormatStrategyPresets.createExportPreset960x540Strategy(), listener
        )
    }

    override fun uploadVideo(): Single<UploadResponse> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private val subject: BehaviorSubject<List<SavedVideo>> = BehaviorSubject.create()

    @SuppressLint("CheckResult")
    override fun saveVideoToDB(context: Context, filePath: String, isFavorite: Boolean) {
        println("SAVING")
        val db = AppDatabase.getAppDataBase(context = context)
        Observable.fromCallable {
            val video = SavedVideo(vidPath = filePath, isFavorite = isFavorite)
            val videoDao = db?.videoDao()
            with(videoDao) {
                this?.saveVideo(video)
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

        val db = AppDatabase.getAppDataBase(context = context)
        Observable.fromCallable {
            db?.videoDao()?.getVideos()
        }.map {
            println("LOADING")
            it.forEach {
                listOfVideos.add(0, it)

            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                subject.onNext(listOfVideos)
                transcodeVideo(context, File(listOfVideos[1].vidPath))
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