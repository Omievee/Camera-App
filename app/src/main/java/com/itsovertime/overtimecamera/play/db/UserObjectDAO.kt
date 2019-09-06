package com.itsovertime.overtimecamera.play.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.itsovertime.overtimecamera.play.model.User

@Dao
interface UserObjectDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveUserData(user: User)

}