package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.model.MediaTrack
import com.example.model.PlaybackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object PlaybackController {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressTickerJob: Job? = null

    private val _currentTrack = MutableStateFlow<MediaTrack?>(null)
    val currentTrack: StateFlow<MediaTrack?> = _currentTrack.asStateFlow()

    private val _playlist = MutableStateFlow<List<MediaTrack>>(emptyList())
    val playlist: StateFlow<List<MediaTrack>> = _playlist.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    var activeService: MediaPlaybackService? = null

    fun setPlaylist(tracks: List<MediaTrack>, startTrack: MediaTrack? = null, context: Context? = null) {
        _playlist.value = tracks
        val trackToPlay = startTrack ?: tracks.firstOrNull()
        if (trackToPlay != null) {
            playTrack(trackToPlay, context)
        }
    }

    fun playTrack(track: MediaTrack, context: Context? = null) {
        _currentTrack.value = track
        _playbackState.value = _playbackState.value.copy(
            isPlaying = true,
            currentPositionMs = 0L,
            durationMs = track.durationMs
        )

        if (context != null) {
            val intent = Intent(context, MediaPlaybackService::class.java).apply {
                action = MediaPlaybackService.ACTION_PLAY
                putExtra(MediaPlaybackService.EXTRA_TRACK_PATH, track.path)
                putExtra(MediaPlaybackService.EXTRA_TRACK_TITLE, track.title)
                putExtra(MediaPlaybackService.EXTRA_TRACK_ARTIST, track.artist)
                putExtra(MediaPlaybackService.EXTRA_IS_VIDEO, track.isVideo)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            activeService?.playPath(track.path)
        }

        startProgressTicker()
    }

    fun togglePlayPause(context: Context? = null) {
        val currentState = _playbackState.value
        if (currentState.isPlaying) {
            pause(context)
        } else {
            resume(context)
        }
    }

    fun pause(context: Context? = null) {
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
        activeService?.pausePlayback()
        if (context != null) {
            val intent = Intent(context, MediaPlaybackService::class.java).apply {
                action = MediaPlaybackService.ACTION_PAUSE
            }
            context.startService(intent)
        }
        stopProgressTicker()
    }

    fun resume(context: Context? = null) {
        _playbackState.value = _playbackState.value.copy(isPlaying = true)
        activeService?.resumePlayback()
        if (context != null) {
            val intent = Intent(context, MediaPlaybackService::class.java).apply {
                action = MediaPlaybackService.ACTION_RESUME
            }
            context.startService(intent)
        }
        startProgressTicker()
    }

    fun seekTo(positionMs: Long) {
        _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
        activeService?.seekTo(positionMs.toInt())
    }

    fun next(context: Context? = null) {
        val list = _playlist.value
        val current = _currentTrack.value ?: return
        val currentIndex = list.indexOfFirst { it.path == current.path }
        if (currentIndex != -1 && list.isNotEmpty()) {
            val nextIndex = if (_playbackState.value.isShuffle) {
                (list.indices).random()
            } else {
                (currentIndex + 1) % list.size
            }
            playTrack(list[nextIndex], context)
        }
    }

    fun previous(context: Context? = null) {
        val list = _playlist.value
        val current = _currentTrack.value ?: return
        val currentIndex = list.indexOfFirst { it.path == current.path }
        if (currentIndex != -1 && list.isNotEmpty()) {
            val prevIndex = if (currentIndex - 1 < 0) list.size - 1 else currentIndex - 1
            playTrack(list[prevIndex], context)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackState.value = _playbackState.value.copy(playbackSpeed = speed)
        activeService?.setSpeed(speed)
    }

    fun toggleLoop() {
        val newLoop = !_playbackState.value.isLooping
        _playbackState.value = _playbackState.value.copy(isLooping = newLoop)
        activeService?.setLooping(newLoop)
    }

    fun toggleShuffle() {
        _playbackState.value = _playbackState.value.copy(isShuffle = !_playbackState.value.isShuffle)
    }

    fun toggleBackgroundPlayback() {
        val enabled = !_playbackState.value.isBackgroundPlaybackEnabled
        _playbackState.value = _playbackState.value.copy(isBackgroundPlaybackEnabled = enabled)
    }

    fun updateProgress(currentMs: Long, totalMs: Long) {
        _playbackState.value = _playbackState.value.copy(
            currentPositionMs = currentMs,
            durationMs = if (totalMs > 0) totalMs else _playbackState.value.durationMs
        )
    }

    fun stop(context: Context? = null) {
        _playbackState.value = _playbackState.value.copy(isPlaying = false, currentPositionMs = 0L)
        activeService?.stopPlayback()
        if (context != null) {
            val intent = Intent(context, MediaPlaybackService::class.java).apply {
                action = MediaPlaybackService.ACTION_STOP
            }
            context.startService(intent)
        }
        stopProgressTicker()
    }

    private fun startProgressTicker() {
        progressTickerJob?.cancel()
        progressTickerJob = scope.launch {
            while (isActive) {
                delay(500)
                activeService?.let { service ->
                    if (service.isPlaying()) {
                        val current = service.getCurrentPosition().toLong()
                        val duration = service.getDuration().toLong()
                        updateProgress(current, duration)
                    }
                }
            }
        }
    }

    private fun stopProgressTicker() {
        progressTickerJob?.cancel()
        progressTickerJob = null
    }
}
