package com.example.dicerollserver

import android.app.Service
import android.content.Intent
import android.os.IBinder

class ServerForegroundService : Service() {

    companion object {
        const val TAG = "ServerService"
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(
            Notifs.Id.FOREGROUND_SERVICE,
            Notifs.getForegroundNotification(this)
        )
    }
}
