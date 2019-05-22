package com.itsovertime.overtimecamera.play.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.itsovertime.overtimecamera.play.model.SavedVideo

@Database(entities = [SavedVideo::class], version = 3)
abstract class AppDatabase : RoomDatabase() {

    abstract fun videoDao(): VideoObjectDAO

    companion object {
        var databaseInstance: AppDatabase? = null

        fun getAppDataBase(context: Context): AppDatabase? {
            if (databaseInstance == null) {
                synchronized(AppDatabase::class) {
                    databaseInstance = Room
                        .databaseBuilder(
                            context
                                .applicationContext,
                            AppDatabase::class.java,
                            "VideosDB"
                        )
                        .fallbackToDestructiveMigration()
                        .build()
                }
            }
            return databaseInstance
        }

        fun destroyDataBase() {
            databaseInstance = null
        }
    }
}