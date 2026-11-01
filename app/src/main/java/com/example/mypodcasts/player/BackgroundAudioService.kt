@file:OptIn(UnstableApi::class)

package com.example.mypodcasts.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import androidx.media3.common.util.UnstableApi
import com.example.mypodcasts.R
import com.example.mypodcasts.ui.main.MainActivity

class BackgroundAudioService : Service() {

    companion object {
        const val CHANNEL_ID = "mypodcasts_media_controls_silent_v7"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START_BG_AUDIO"
        const val ACTION_UPDATE = "ACTION_UPDATE_BG_AUDIO"
        const val ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE"
        const val ACTION_REWIND_15 = "ACTION_REWIND_15"
        const val ACTION_FORWARD_15 = "ACTION_FORWARD_15"
        const val ACTION_STOP = "ACTION_STOP_BG_AUDIO"

        const val EXTRA_TRACK_TITLE = "EXTRA_TRACK_TITLE"
        const val EXTRA_ARTIST = "EXTRA_ARTIST"
        const val EXTRA_IS_PLAYING = "EXTRA_IS_PLAYING"

        var playerManagerRef: AudioPlayerManager? = null

        fun startService(context: Context, title: String, artist: String? = null, isPlaying: Boolean = true) {
            val intent = Intent(context, BackgroundAudioService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TRACK_TITLE, title)
                putExtra(EXTRA_ARTIST, artist)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun updateNotification(context: Context, title: String, artist: String? = null, isPlaying: Boolean) {
            val intent = Intent(context, BackgroundAudioService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_TRACK_TITLE, title)
                putExtra(EXTRA_ARTIST, artist)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, BackgroundAudioService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            ACTION_START, ACTION_UPDATE -> {
                val title = intent.getStringExtra(EXTRA_TRACK_TITLE) ?: "MyPodcasts"
                val artist = intent.getStringExtra(EXTRA_ARTIST)
                val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, true)
                buildAndShowNotification(title, artist, isPlaying)
            }
            ACTION_PLAY_PAUSE -> {
                playerManagerRef?.togglePlayPause()
            }
            ACTION_REWIND_15 -> {
                playerManagerRef?.rewind15Sec()
            }
            ACTION_FORWARD_15 -> {
                playerManagerRef?.forward15Sec()
            }
            ACTION_STOP -> {
                playerManagerRef?.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun buildAndShowNotification(title: String, artist: String?, isPlaying: Boolean) {
        createNotificationChannel()

        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Media Control PendingIntents
        val rewindPendingIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, BackgroundAudioService::class.java).apply { action = ACTION_REWIND_15 },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPausePendingIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, BackgroundAudioService::class.java).apply { action = ACTION_PLAY_PAUSE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val forwardPendingIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, BackgroundAudioService::class.java).apply { action = ACTION_FORWARD_15 },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPendingIntent = PendingIntent.getService(
            this,
            4,
            Intent(this, BackgroundAudioService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        val mediaStyle = MediaNotificationCompat.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)

        val mediaSession = playerManagerRef?.mediaSession
        if (mediaSession != null) {
            try {
                mediaStyle.setMediaSession(mediaSession.sessionCompatToken)
            } catch (_: Exception) {}
        }

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist ?: "MyPodcasts Media Player")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(mainPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSound(null)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .addAction(android.R.drawable.ic_media_rew, "-15s", rewindPendingIntent)
            .addAction(playPauseIcon, playPauseTitle, playPausePendingIntent)
            .addAction(android.R.drawable.ic_media_ff, "+15s", forwardPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setStyle(mediaStyle)

        val notification: Notification = notificationBuilder.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Controls (Silent)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Silent media controls notification for audio and video playback"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
