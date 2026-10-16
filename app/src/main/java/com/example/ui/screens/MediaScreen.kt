package com.example.ui.screens

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.manager.DeviceVolumeManager
import com.example.manager.FileManager
import com.example.model.FileCategory
import com.example.model.MediaTrack
import com.example.service.PlaybackController
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaScreen(
    fileManager: FileManager,
    volumeManager: DeviceVolumeManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Audio/MP3, 1: Video

    val currentTrack by PlaybackController.currentTrack.collectAsState()
    val playbackState by PlaybackController.playbackState.collectAsState()

    // Scan for available local media
    var localAudios by remember { mutableStateOf<List<MediaTrack>>(emptyList()) }
    var localVideos by remember { mutableStateOf<List<MediaTrack>>(emptyList()) }

    LaunchedEffect(Unit) {
        val root = fileManager.getRootDirectory()
        val audioFiles = fileManager.listDirectory(root, category = FileCategory.AUDIO)
        val videoFiles = fileManager.listDirectory(root, category = FileCategory.VIDEO).filter { it.size > 1024 }

        val audios = audioFiles.map {
            MediaTrack(
                id = it.path,
                title = it.name.substringBeforeLast("."),
                artist = "Local Audio",
                durationMs = 0L,
                path = it.path,
                isVideo = false,
                size = it.size
            )
        }
        val videos = videoFiles.map {
            MediaTrack(
                id = it.path,
                title = it.name.substringBeforeLast("."),
                artist = "Video Collection",
                durationMs = 0L,
                path = it.path,
                isVideo = true,
                size = it.size
            )
        }.toMutableList()

        // Add a working demo stream video if no large local video is present yet
        if (videos.isEmpty()) {
            videos.add(
                MediaTrack(
                    id = "sample_stream_1",
                    title = "Big Buck Bunny (HD Sample)",
                    artist = "Blender Foundation",
                    durationMs = 600000L,
                    path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    isVideo = true,
                    size = 158000000L
                )
            )
        }

        localAudios = audios
        localVideos = videos

        if (PlaybackController.currentTrack.value == null && audios.isNotEmpty()) {
            PlaybackController.setPlaylist(audios, startTrack = null, context = null)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = "MP3 & Audio Player",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                },
                icon = { Icon(Icons.Default.AudioFile, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = "Video Player",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                },
                icon = { Icon(Icons.Default.VideoFile, contentDescription = null) }
            )
        }

        if (selectedTab == 0) {
            AudioPlayerTab(
                currentTrack = currentTrack?.takeIf { !it.isVideo } ?: localAudios.firstOrNull(),
                localAudios = localAudios,
                volumeManager = volumeManager
            )
        } else {
            VideoPlayerTab(
                currentVideo = currentTrack?.takeIf { it.isVideo } ?: localVideos.firstOrNull(),
                localVideos = localVideos,
                volumeManager = volumeManager
            )
        }
    }
}

@Composable
fun AudioPlayerTab(
    currentTrack: MediaTrack?,
    localAudios: List<MediaTrack>,
    volumeManager: DeviceVolumeManager
) {
    val context = LocalContext.current
    val playbackState by PlaybackController.playbackState.collectAsState()
    val volumeState by volumeManager.volumeState.collectAsState()
    var speedMenuOpen by remember { mutableStateOf(false) }

    // Vinyl rotation animation
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(playbackState.isPlaying) {
        if (playbackState.isPlaying) {
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 6000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))

            // Vinyl / Artwork Display
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                        )
                    )
                    .rotate(rotation.value),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF111827).copy(alpha = 0.85f))
                )
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Track Details with safe horizontal layout
            Text(
                text = currentTrack?.title ?: "Select a track to play",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = currentTrack?.artist ?: "FileHub Pro Studio",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Waveform animation bars
            AnimatedWaveformBars(isPlaying = playbackState.isPlaying)

            Spacer(modifier = Modifier.height(10.dp))

            // Device Hardware Volume Buttons & Slider
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Device Volume (مستوى صوت الجهاز)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "${(volumeState.volumePercent * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { volumeManager.volumeDown() },
                            modifier = Modifier.size(40.dp).testTag("vol_down_button")
                        ) {
                            Icon(Icons.Default.VolumeDown, contentDescription = "Volume Down", tint = MaterialTheme.colorScheme.primary)
                        }

                        Slider(
                            value = volumeState.volumePercent,
                            onValueChange = { volumeManager.setVolume(it) },
                            modifier = Modifier.weight(1f).testTag("volume_slider")
                        )

                        IconButton(
                            onClick = { volumeManager.volumeUp() },
                            modifier = Modifier.size(40.dp).testTag("vol_up_button")
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Volume Up", tint = MaterialTheme.colorScheme.primary)
                        }

                        IconButton(
                            onClick = { volumeManager.toggleMute() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (volumeState.isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                contentDescription = "Mute Toggle",
                                tint = if (volumeState.isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Background Playback Chip
            FilterChip(
                selected = playbackState.isBackgroundPlaybackEnabled,
                onClick = { PlaybackController.toggleBackgroundPlayback() },
                label = {
                    Text(
                        text = if (playbackState.isBackgroundPlaybackEnabled) "Background Audio: Active ✓" else "Background Audio: Off",
                        maxLines = 1,
                        softWrap = false
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Headphones, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Slider & Timestamps
            Slider(
                value = playbackState.progress,
                onValueChange = { newProgress ->
                    val targetMs = (newProgress * playbackState.durationMs).toLong()
                    PlaybackController.seekTo(targetMs)
                },
                modifier = Modifier.fillMaxWidth().testTag("audio_seek_bar")
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(playbackState.currentPositionMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDuration(playbackState.durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Controls Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { PlaybackController.toggleShuffle() }) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playbackState.isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { PlaybackController.previous(context) }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(36.dp))
                }

                FilledIconButton(
                    onClick = {
                        if (currentTrack != null) {
                            if (PlaybackController.currentTrack.value == null) {
                                PlaybackController.playTrack(currentTrack, context)
                            } else {
                                PlaybackController.togglePlayPause(context)
                            }
                        }
                    },
                    modifier = Modifier.size(64.dp).testTag("play_pause_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = { PlaybackController.next(context) }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(36.dp))
                }

                IconButton(onClick = { PlaybackController.toggleLoop() }) {
                    Icon(
                        if (playbackState.isLooping) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Loop",
                        tint = if (playbackState.isLooping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Speed Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    FilterChip(
                        selected = false,
                        onClick = { speedMenuOpen = true },
                        label = { Text("Speed: ${playbackState.playbackSpeed}x", maxLines = 1, softWrap = false) },
                        leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    DropdownMenu(
                        expanded = speedMenuOpen,
                        onDismissRequest = { speedMenuOpen = false }
                    ) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                            DropdownMenuItem(
                                text = { Text("${spd}x") },
                                onClick = {
                                    PlaybackController.setPlaybackSpeed(spd)
                                    speedMenuOpen = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Audio Playlist (${localAudios.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        items(localAudios) { track ->
            val isCurrent = currentTrack?.path == track.path
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable {
                        PlaybackController.setPlaylist(localAudios, startTrack = track, context = context)
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isCurrent && playbackState.isPlaying) Icons.Default.GraphicEq else Icons.Default.AudioFile,
                        contentDescription = null,
                        tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPlayerTab(
    currentVideo: MediaTrack?,
    localVideos: List<MediaTrack>,
    volumeManager: DeviceVolumeManager
) {
    val context = LocalContext.current
    var activeVideo by remember { mutableStateOf(currentVideo) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    val playbackState by PlaybackController.playbackState.collectAsState()
    val volumeState by volumeManager.volumeState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Video View Container
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val videoPath = activeVideo?.path
                val isFile = videoPath != null && videoPath.startsWith("/")
                val isFileValid = isFile && File(videoPath).exists() && File(videoPath).length() > 1024
                val isHttp = videoPath != null && (videoPath.startsWith("http://") || videoPath.startsWith("https://"))

                if (activeVideo != null && (isFileValid || isHttp)) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                val controller = MediaController(ctx)
                                controller.setAnchorView(this)
                                setMediaController(controller)
                                setOnErrorListener { _, _, _ -> true }
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    start()
                                }
                                if (isHttp) {
                                    setVideoURI(Uri.parse(videoPath))
                                } else if (isFileValid) {
                                    setVideoURI(Uri.fromFile(File(videoPath)))
                                }
                                videoViewRef = this
                            }
                        },
                        update = { view ->
                            try {
                                if (isHttp) {
                                    view.setVideoURI(Uri.parse(videoPath))
                                    view.start()
                                } else if (isFileValid) {
                                    view.setVideoURI(Uri.fromFile(File(videoPath)))
                                    view.start()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.VideoFile,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (activeVideo != null) activeVideo!!.title else "Select a video clip below",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Device Volume Control Bar for Video
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { volumeManager.volumeDown() }) {
                Icon(Icons.Default.VolumeDown, contentDescription = "Vol -", tint = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = volumeState.volumePercent,
                onValueChange = { volumeManager.setVolume(it) },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { volumeManager.volumeUp() }) {
                Icon(Icons.Default.VolumeUp, contentDescription = "Vol +", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Quick Seek and Background Video Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                IconButton(onClick = {
                    videoViewRef?.let {
                        val pos = (it.currentPosition - 10000).coerceAtLeast(0)
                        it.seekTo(pos)
                    }
                }) {
                    Icon(Icons.Default.Replay10, contentDescription = "-10s")
                }
                IconButton(onClick = {
                    videoViewRef?.let {
                        val pos = it.currentPosition + 10000
                        it.seekTo(pos)
                    }
                }) {
                    Icon(Icons.Default.Forward10, contentDescription = "+10s")
                }
            }

            FilterChip(
                selected = playbackState.isBackgroundPlaybackEnabled,
                onClick = {
                    PlaybackController.toggleBackgroundPlayback()
                    activeVideo?.let {
                        PlaybackController.playTrack(it, context)
                    }
                },
                label = { Text("Background Video Audio", maxLines = 1, softWrap = false) },
                leadingIcon = { Icon(Icons.Default.Headphones, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Video Library (${localVideos.size})",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            softWrap = false
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(localVideos) { video ->
                val isSelected = activeVideo?.path == video.path
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            activeVideo = video
                            try {
                                if (video.path.startsWith("http://") || video.path.startsWith("https://")) {
                                    videoViewRef?.setVideoURI(Uri.parse(video.path))
                                } else if (File(video.path).exists() && File(video.path).length() > 1024) {
                                    videoViewRef?.setVideoURI(Uri.fromFile(File(video.path)))
                                }
                                videoViewRef?.start()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = video.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "MP4 Video",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedWaveformBars(isPlaying: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(24.dp)
    ) {
        val barCount = 7
        for (i in 0 until barCount) {
            val animHeight = remember { Animatable(4f) }
            LaunchedEffect(isPlaying) {
                if (isPlaying) {
                    animHeight.animateTo(
                        targetValue = (8..24).random().toFloat(),
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = (200..450).random(), easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                } else {
                    animHeight.snapTo(4f)
                }
            }
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(animHeight.value.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
