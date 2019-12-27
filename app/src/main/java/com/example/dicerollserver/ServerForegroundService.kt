package com.example.dicerollserver

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.IBinder


class ServerForegroundService : Service() {
    private lateinit var wifiLock: WifiManager.WifiLock
    private val WIFI_LOCK_TAG = "server_wifi_lock"

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

        // Acquire wifi lock to keep wifi alive to handle request to server
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, WIFI_LOCK_TAG)
        wifiLock.acquire()



    }

    override fun onDestroy() {
        super.onDestroy()
        if (wifiLock.isHeld) wifiLock.release()
    }
}
