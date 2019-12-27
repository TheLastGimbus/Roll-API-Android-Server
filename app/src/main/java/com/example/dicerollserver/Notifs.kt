package com.example.dicerollserver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class Notifs {
    class Id {
        companion object {
            const val FOREGROUND_SERVICE = 10
        }
    }

    class ChannelId {
        companion object {
            const val FOREGROUND_SERVICE = "server_foregroud_service"
        }
    }

    companion object {
        fun notify(context: Context, notification: Notification, id: Int = 0) {
            val nm = NotificationManagerCompat.from(context)
            nm.notify(id, notification)
        }

        fun getForegroundNotification(ctx: Context): Notification {
            val builder = NotificationCompat.Builder(ctx, ChannelId.FOREGROUND_SERVICE)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Server is up and running!")
                .setPriority(NotificationCompat.PRIORITY_LOW)
            return builder.build()
        }

        fun createChannels(ctx: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createChannel(
                    ctx,
                    ChannelId.FOREGROUND_SERVICE,
                    "Server foreground service",
                    null,
                    NotificationManager.IMPORTANCE_LOW
                )
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun createChannel(
            ctx: Context,
            id: String,
            name: String,
            description: String?,
            importance: Int = NotificationManager.IMPORTANCE_DEFAULT
        ) {
            val notificationManager =
                ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val mChannel = NotificationChannel(id, name, importance)
            if (!description.isNullOrEmpty()) {
                mChannel.description = description
            }
            notificationManager.createNotificationChannel(mChannel)
        }
    }
}