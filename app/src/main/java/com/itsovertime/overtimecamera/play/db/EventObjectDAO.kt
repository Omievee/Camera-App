package com.itsovertime.overtimecamera.play.db

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.itsovertime.overtimecamera.play.model.Event
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.User

interface EventObjectDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveEventData(event: List<Event>)

    @Query("SELECT * FROM Event")
    fun getEvents(): List<Event>
}