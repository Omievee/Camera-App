package com.itsovertime.overtimecamera.play.notifications

interface NotificationManager {

    fun onCreateProgressNotification(msg: String, uploadMsg: String, ongoing: Boolean)
    fun onUpdateProgressNotification(uploadMsg: String)
    fun onCreateStandardNotification(msg: String)
    fun onCreateNotificationChannel()
}