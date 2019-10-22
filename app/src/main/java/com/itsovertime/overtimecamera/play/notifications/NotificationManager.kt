package com.itsovertime.overtimecamera.play.notifications

interface NotificationManager {

    fun onCreateProgressNotification(msg: String, progress: Int, maxProg: Int)
    fun onUpdateProgressNotification(progress: Int, maxProg: Int)
    fun onCreateStandardNotification(msg: String)
    fun onCreateNotificationChannel()
}