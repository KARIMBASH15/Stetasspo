package com.example.model

data class MediaTrack(
    val id: String,
    val title: String,
    val artist: String = "Unknown Artist",
    val album: String = "FileHub Media",
    val durationMs: Long = 0L,
    val path: String,
    val isVideo: Boolean = false,
    val thumbnailUri: String? = null,
    val size: Long = 0L
)

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isLooping: Boolean = false,
    val isShuffle: Boolean = false,
    val isBackgroundPlaybackEnabled: Boolean = true
) {
    val progress: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

data class BrowserBookmark(
    val title: String,
    val url: String,
    val iconName: String = "web"
)

enum class DownloadStatus {
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

data class DownloadTask(
    val id: String,
    val url: String,
    val fileName: String,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.DOWNLOADING,
    val error: String? = null
) {
    val progressPercent: Int
        get() = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
}

data class VaultEntry(
    val id: String,
    val originalName: String,
    val originalPath: String,
    val vaultFileName: String,
    val dateLocked: Long,
    val size: Long,
    val mimeType: String
)

data class PermissionItem(
    val id: String,
    val name: String,
    val nameAr: String,
    val description: String,
    val descriptionAr: String,
    val isGranted: Boolean,
    val permissionKey: String,
    val isCritical: Boolean = true
)
