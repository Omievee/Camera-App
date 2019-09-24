package com.itsovertime.overtimecamera.play.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.model.User
import io.reactivex.Single

@Dao
interface UserObjectDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveUserData(user: User)


    @Query("SELECT * FROM User")
    fun getUser(): Single<User>

}