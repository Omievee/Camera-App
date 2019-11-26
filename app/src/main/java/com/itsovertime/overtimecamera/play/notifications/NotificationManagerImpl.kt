package com.itsovertime.overtimecamera.play.notifications

import android.R
import android.app.NotificationChannel
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.itsovertime.overtimecamera.play.application.OTApplication

class NotificationManagerImpl(val context: OTApplication) : NotificationManager {
    override fun onCreateNotificationChannel(notificationMessage: String) {
        val name = "Uploads"
        val description = "Uploads in progress"
        val importance = android.app.NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("Uploads", name, importance)
        channel.description = description

        // Add the channel
        notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        notificationManager?.createNotificationChannel(channel)
        onCreateProgressNotification(notificationMessage, 0, 0)
    }

    var uploadsBuilder = NotificationCompat.Builder(context, "Uploads")
    var notificationManager: android.app.NotificationManager? = null
    override fun onCreateProgressNotification(msg: String, progress: Int, maxProg: Int) {

        uploadsBuilder.apply {
            setSmallIcon(R.drawable.sym_def_app_icon)
            setContentTitle(msg)
            setContentText("Uploads in progress")
            setProgress(100, 0, true)
            priority = NotificationCompat.PRIORITY_HIGH
        }

        NotificationManagerCompat.from(context).notify(Uploads, uploadsBuilder.build())
    }

    override fun onUpdateProgressNotification(progress: Int, maxProg: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCreateStandardNotification(msg: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    companion object {
        const val General = 0
        const val Uploads = 1
    }

}