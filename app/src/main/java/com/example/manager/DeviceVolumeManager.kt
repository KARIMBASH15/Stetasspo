package com.example.manager

import android.content.Context
import android.media.AudioManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VolumeState(
    val currentVolume: Int = 10,
    val maxVolume: Int = 15,
    val isMuted: Boolean = false
) {
    val volumePercent: Float
        get() = if (maxVolume > 0) (currentVolume.toFloat() / maxVolume.toFloat()).coerceIn(0f, 1f) else 0f
}

class DeviceVolumeManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _volumeState = MutableStateFlow(getCurrentVolumeState())
    val volumeState: StateFlow<VolumeState> = _volumeState.asStateFlow()

    private fun getCurrentVolumeState(): VolumeState {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return VolumeState(
            currentVolume = current,
            maxVolume = max,
            isMuted = current == 0
        )
    }

    fun updateState() {
        _volumeState.value = getCurrentVolumeState()
    }

    fun volumeUp() {
        try {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_SHOW_UI
            )
            updateState()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun volumeDown() {
        try {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI
            )
            updateState()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setVolume(percent: Float) {
        try {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val target = (percent * max).toInt().coerceIn(0, max)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
            updateState()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleMute() {
        try {
            val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (current > 0) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
            } else {
                val half = (audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2).coerceAtLeast(1)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, half, 0)
            }
            updateState()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
