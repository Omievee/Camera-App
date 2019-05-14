package com.overtime.camera.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.overtime.camera.model.SavedVideo

@Database(entities = [SavedVideo::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun videoDao(): VideoObjectDAO

    companion object {
        var INSTANCE: AppDatabase? = null

        fun getAppDataBase(context: Context): AppDatabase? {
            if (INSTANCE == null) {
                synchronized(AppDatabase::class) {
                    INSTANCE = Room
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
            return INSTANCE
        }

        fun destroyDataBase() {
            INSTANCE = null
        }
    }
}