package com.itsovertime.overtimecamera.play.notifications

import android.R
import android.app.NotificationChannel
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.itsovertime.overtimecamera.play.application.OTApplication

class NotificationManagerImpl(val context: OTApplication) : NotificationManager {
    override fun onCreateNotificationChannel() {
        val name = "HD Uploads"
        val description = "Uploads in progress"
        val importance = android.app.NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("Uploads", name, importance)
        channel.description = description

        // Add the channel
        notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager?.createNotificationChannel(channel)
    }

    override fun onClearNotifications() {
        notificationManager?.cancelAll()
    }

    var uploadsBuilder: NotificationCompat.Builder? = null
    var notificationManager: android.app.NotificationManager? = null
    override fun onCreateProgressNotification(msg: String, uploadMsg: String, ongoing: Boolean) {
        onCreateNotificationChannel()
        uploadsBuilder = NotificationCompat.Builder(context, "Uploads")
        uploadsBuilder?.apply {
            setSmallIcon(R.drawable.sym_def_app_icon)
            setContentTitle(msg)
            setOngoing(ongoing)
            setContentText(uploadMsg)
            setProgress(0, 0, ongoing)
            priority = NotificationCompat.PRIORITY_HIGH
        }

        uploadsBuilder?.build()?.let { NotificationManagerCompat.from(context).notify(Uploads, it) }
    }

    override fun onUpdateProgressNotification(uploadMsg: String) {
        uploadsBuilder?.apply {
            setSmallIcon(R.drawable.sym_def_app_icon)
            setContentTitle(uploadMsg)
            setContentText("")
            setOngoing(false)
            setProgress(0, 0, false)
            priority = NotificationCompat.PRIORITY_HIGH
        }
        NotificationManagerCompat.from(context).notify(Uploads, uploadsBuilder?.build() ?: return)
    }

    override fun onCreateStandardNotification(msg: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    companion object {
        const val General = 0
        const val Uploads = 1
    }
}