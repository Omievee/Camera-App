package com.itsovertime.overtimecamera.play.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.itsovertime.overtimecamera.play.model.Event
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.enumConverter

@Database(entities = [SavedVideo::class], version = 3)
@TypeConverters(value = [enumConverter::class])
abstract class AppDatabase : RoomDatabase() {

    abstract fun videoDao(): VideoObjectDAO

    companion object {
        var databaseInstance: AppDatabase? = null

        fun getAppDataBase(context: Context): AppDatabase? {
            if (databaseInstance == null) {
                synchronized(AppDatabase::class) {
                    databaseInstance =
                            Room
                                    .databaseBuilder(
                                            context
                                                    .applicationContext,
                                            AppDatabase::class.java,
                                            "DB"
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