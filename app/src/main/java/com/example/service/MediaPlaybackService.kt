package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import java.io.File
import java.io.FileInputStream

class MediaPlaybackService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var currentPath: String? = null
    private var currentTitle: String = "Playing Audio"
    private var currentArtist: String = "FileHub Pro"

    companion object {
        const val CHANNEL_ID = "media_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.example.service.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.service.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.service.ACTION_RESUME"
        const val ACTION_NEXT = "com.example.service.ACTION_NEXT"
        const val ACTION_PREV = "com.example.service.ACTION_PREV"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"

        const val EXTRA_TRACK_PATH = "extra_track_path"
        const val EXTRA_TRACK_TITLE = "extra_track_title"
        const val EXTRA_TRACK_ARTIST = "extra_track_artist"
        const val EXTRA_IS_VIDEO = "extra_is_video"
    }

    override fun onCreate() {
        super.onCreate()
        PlaybackController.activeService = this
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val path = intent.getStringExtra(EXTRA_TRACK_PATH)
                val title = intent.getStringExtra(EXTRA_TRACK_TITLE) ?: "Playing Audio"
                val artist = intent.getStringExtra(EXTRA_TRACK_ARTIST) ?: "FileHub Pro"
                currentTitle = title
                currentArtist = artist
                if (!path.isNullOrEmpty()) {
                    playPath(path)
                }
            }
            ACTION_PAUSE -> pausePlayback()
            ACTION_RESUME -> resumePlayback()
            ACTION_NEXT -> PlaybackController.next(this)
            ACTION_PREV -> PlaybackController.previous(this)
            ACTION_STOP -> {
                stopPlayback()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    fun playPath(path: String) {
        currentPath = path
        try {
            val file = File(path)
            if (path.startsWith("/") && (!file.exists() || file.length() < 100)) {
                android.util.Log.w("MediaPlaybackService", "Skipping invalid/empty file: $path")
                return
            }

            mediaPlayer?.reset()
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setOnErrorListener { mp, what, extra ->
                    android.util.Log.e("MediaPlaybackService", "MediaPlayer error: what=$what, extra=$extra")
                    PlaybackController.pause()
                    true // Handled, prevent calling onCompletionListener
                }
                setOnCompletionListener {
                    PlaybackController.next(this@MediaPlaybackService)
                }
                setOnPreparedListener { mp ->
                    try {
                        mp.start()
                        val speed = PlaybackController.playbackState.value.playbackSpeed
                        setSpeed(speed)
                        startForeground(NOTIFICATION_ID, buildNotification(isPlaying = true))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (file.exists()) {
                    FileInputStream(file).use { fis ->
                        setDataSource(fis.fd)
                    }
                } else {
                    setDataSource(this@MediaPlaybackService, Uri.parse(path))
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pausePlayback() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
            val notification = buildNotification(isPlaying = false)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resumePlayback() {
        try {
            mediaPlayer?.start()
            val notification = buildNotification(isPlaying = true)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun seekTo(msec: Int) {
        try {
            mediaPlayer?.seekTo(msec)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isPlaying(): Boolean = try {
        mediaPlayer?.isPlaying == true
    } catch (_: Exception) {
        false
    }

    fun getCurrentPosition(): Int = try {
        mediaPlayer?.currentPosition ?: 0
    } catch (_: Exception) {
        0
    }

    fun getDuration(): Int = try {
        mediaPlayer?.duration ?: 0
    } catch (_: Exception) {
        0
    }

    fun setSpeed(speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        player.playbackParams = PlaybackParams().apply { this.speed = speed }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setLooping(loop: Boolean) {
        try {
            mediaPlayer?.isLooping = loop
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground playback controls for audio and video in background"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(isPlaying: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseAction = if (isPlaying) {
            val pauseIntent = Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_PAUSE }
            val pausePending = PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action(android.R.drawable.ic_media_pause, "Pause", pausePending)
        } else {
            val resumeIntent = Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_RESUME }
            val resumePending = PendingIntent.getService(this, 2, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action(android.R.drawable.ic_media_play, "Play", resumePending)
        }

        val nextIntent = Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_NEXT }
        val nextPending = PendingIntent.getService(this, 3, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val prevIntent = Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_PREV }
        val prevPending = PendingIntent.getService(this, 4, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(this, 5, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentTitle)
            .setContentText(currentArtist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(isPlaying)
            .addAction(android.R.drawable.ic_media_previous, "Prev", prevPending)
            .addAction(playPauseAction)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPending)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    override fun onDestroy() {
        stopPlayback()
        PlaybackController.activeService = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
