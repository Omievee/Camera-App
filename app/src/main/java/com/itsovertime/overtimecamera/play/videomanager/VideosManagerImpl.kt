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
import android.os.Environment
import com.crashlytics.android.Crashlytics
import java.io.IOException
import java.lang.RuntimeException


class VideosManagerImpl() : VideosManager {
    override fun uploadVideo(): Single<UploadResponse> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    var context: Context? = null
    override fun transcodeVideo(context: Context, videoFile: File) {
        this.context = context
        val file = Uri.fromFile(videoFile)
        println("file?? $file")

        val parcelFileDescriptor = context.contentResolver.openAssetFileDescriptor(file, "rw")
        val fileDescriptor = parcelFileDescriptor?.fileDescriptor

        val listener = object : MediaTranscoder.Listener {
            override fun onTranscodeProgress(progress: Double) {
                println("progress ::: $progress")
            }

            override fun onTranscodeCanceled() {}
            override fun onTranscodeFailed(exception: Exception?) {
                //Place crashlytics logic here..
                exception?.printStackTrace()
            }

            override fun onTranscodeCompleted() {
                //begin upload
                println("complete :::")
            }
        }
        try {
            MediaTranscoder.getInstance().transcodeVideo(
                fileDescriptor, compressedFile(videoFile).absolutePath,
                MediaFormatStrategyPresets.createAndroid720pStrategy(), listener
            )
        } catch (r: RuntimeException) {
            Crashlytics.log("MediaTranscoder-RuntimeException ${r.message}")
            r.printStackTrace()
        } catch (io: IOException) {
            io.printStackTrace()
        } catch (ia: IllegalArgumentException) {
            ia.printStackTrace()
        }
    }

    private fun compressedFile(file: File): File {
        val mediaStorageDir = File(context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "OverTime720")
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            println("Failed....")
        }
        return File(mediaStorageDir.path + File.separator + file.name)
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
            .doFinally {
                when(listOfVideos.size){
                     0 -> {

                     }
                    else ->{
                        transcodeVideo(context = context, videoFile = File(listOfVideos[0].vidPath))
                    }
                }

            }
            .subscribe({
                subject.onNext(listOfVideos)

            },
                {
                    it.printStackTrace()
                }
            )
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