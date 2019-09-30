package com.itsovertime.overtimecamera.play.workmanager

import androidx.core.app.NotificationManagerCompat
import android.R
import android.content.Context.NOTIFICATION_SERVICE
import androidx.core.content.ContextCompat.getSystemService
import android.app.NotificationManager
import android.app.NotificationChannel
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat


class WorkerUtils {

    fun makeStatusNotification(message: String, context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "OverTime"
            val description = "Uploading Video.."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("Uploads", name, importance)
            channel.description = description

            // Add the channel
            val notificationManager =
                context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.createNotificationChannel(channel)
        }

        // Create the notification
        val builder = NotificationCompat.Builder(context, "Uploads")
            .setSmallIcon(R.drawable.sym_def_app_icon)
            .setContentTitle("Uploading Videos")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(LongArray(2))

        // Show the notification
        NotificationManagerCompat.from(context).notify(1, builder.build())
    }

}