package com.example.dicerollserver

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.IBinder
import android.util.Log
import fi.iki.elonen.NanoHTTPD


class ServerForegroundService : Service() {

    companion object {
        const val TAG = "ServerService"
    }

    private lateinit var wifiLock: WifiManager.WifiLock
    private val WIFI_LOCK_TAG = "server_wifi_lock"

    private val server = WebServer(8822)

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


        server.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        server.stop()
        if (wifiLock.isHeld) wifiLock.release()
    }

    private inner class WebServer(PORT: Int) : NanoHTTPD(PORT) {

        override fun serve(session: IHTTPSession?): Response {
            val res: Response = newFixedLengthResponse("Hello there!")
            res.mimeType = "text/plain"
            return res
        }

        override fun start() {
            super.start()
            Log.i(TAG, "Server started")
        }

        override fun stop() {
            super.stop()
            Log.i(TAG, "Server stopped")
        }
    }
}
