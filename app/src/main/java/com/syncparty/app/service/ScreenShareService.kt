package com.syncparty.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.syncparty.app.MainActivity
import com.syncparty.app.R
import com.syncparty.app.SyncPartyApp
import com.syncparty.app.webrtc.WebRtcManager

class ScreenShareService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP) {
            stopScreenSharing()
            stopSelf()
            return START_NOT_STICKY
        }

        if (action == ACTION_START) {
            val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
            val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)

            if (resultData != null) {
                startForegroundWithNotification()
                val webRtcManager = WebRtcManager.getInstance(applicationContext)
                webRtcManager.startScreenCapture(resultCode, resultData)
            } else {
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification() {
        val stopIntent = Intent(this, ScreenShareService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, SyncPartyApp.CHANNEL_SCREEN_SHARE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("SyncParty Screen Stream")
            .setContentText("Broadcasting screen to room participants")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Stop Sharing", stopPendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopScreenSharing() {
        WebRtcManager.getInstance(applicationContext).stopScreenCapture()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScreenSharing()
    }

    companion object {
        const val NOTIFICATION_ID = 4040
        const val ACTION_START = "com.syncparty.app.action.START_SCREEN_SHARE"
        const val ACTION_STOP = "com.syncparty.app.action.STOP_SCREEN_SHARE"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        fun startService(context: Context, resultCode: Int, resultData: Intent) {
            val intent = Intent(context, ScreenShareService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ScreenShareService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
