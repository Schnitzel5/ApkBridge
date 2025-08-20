package me.schnitzel.apkbridge.web.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import me.schnitzel.apkbridge.R
import me.schnitzel.apkbridge.web.WebServer

class WebService : Service() {
    private val webServer: WebServer = WebServer()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Actions.START.toString() -> {
                if (!webServer.isRunning) {
                    startForegroundService(intent)
                    start()
                }
            }
            Actions.STOP.toString() -> {
                stopSelf()
                Log.d("Stopping", "WebServer stopping...")
                webServer.shutdown()
            }
        }
        return START_STICKY
    }

    enum class Actions {
        START, STOP
    }

    private fun start() {
        Log.d("Starting", "WebServer starting...")
        webServer.start({
            Log.d("Running", "WebServer running")
        }, {
            Log.d("Stopped", "WebServer stopped")
        })
        val notification = NotificationCompat.Builder(this, "ApkBridgeServiceChannelId")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("ApkBridge")
            .setContentText("Android Proxy Server is running")
            .build()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        Log.d("Destroyed", "WebServer destroyed")
        super.onDestroy()
    }
}
