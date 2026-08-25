package com.syncparty.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import org.webrtc.PeerConnectionFactory

class SyncPartyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize WebRTC
        val options = PeerConnectionFactory.InitializationOptions.builder(this)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        // Create Notification Channel for Foreground Screen Streaming Service
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_SCREEN_SHARE,
                "Screen Share Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows notification when your screen is actively streaming"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_SCREEN_SHARE = "syncparty_screen_share"
        lateinit var instance: SyncPartyApp
            private set
    }
}
