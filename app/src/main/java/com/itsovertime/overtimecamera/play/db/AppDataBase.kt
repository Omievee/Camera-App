package com.itsovertime.overtimecamera.play.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.itsovertime.overtimecamera.play.model.Event
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.User
import com.itsovertime.overtimecamera.play.model.customConverter

@Database(entities = [SavedVideo::class, User::class, Event::class], version = 3)
@TypeConverters(value = [customConverter::class])
abstract class AppDatabase : RoomDatabase() {

    abstract fun videoDao(): VideoObjectDAO
    abstract fun userDao(): UserObjectDAO
    abstract fun eventDao(): EventObjectDAO

    companion object {
        private var databaseInstance: AppDatabase? = null

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