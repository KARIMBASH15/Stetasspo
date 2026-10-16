package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.manager.DeviceVolumeManager
import com.example.manager.NotepadManager
import com.example.model.NoteItem
import com.example.model.NoteType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun FloatingNotepadBubble(
    notepadManager: NotepadManager,
    volumeManager: DeviceVolumeManager,
    onOpenUrlInBrowser: (String) -> Unit,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var isExpanded by remember { mutableStateOf(false) }
    var showVolumeHud by remember { mutableStateOf(false) }

    // Multi-tap detection engine (1x: Volume controls, 2x: Floating Window, 3x: Open Main App)
    var tapCount by remember { mutableIntStateOf(0) }
    var tapJob by remember { mutableStateOf<Job?>(null) }
    var tapFeedbackText by remember { mutableStateOf<String?>(null) }

    val folders by notepadManager.folders.collectAsState()
    val allNotes by notepadManager.notes.collectAsState()
    val volumeState by volumeManager.volumeState.collectAsState()

    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var showAddNoteDialog by remember { mutableStateOf(false) }

    // Futuristic breathing glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "floating_hub_glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    BoxWithConstraints(modifier = modifier) {
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val bubbleSizePx = with(density) { 48.dp.toPx() }
        val marginPx = with(density) { 10.dp.toPx() }

        // Position coordinates: starts docked gracefully on the right edge, ~62% down the screen
        var bubbleX by remember(screenWidthPx) {
            mutableFloatStateOf(if (screenWidthPx > 0) (screenWidthPx - bubbleSizePx - marginPx) else 0f)
        }
        var bubbleY by remember(screenHeightPx) {
            mutableFloatStateOf(if (screenHeightPx > 0) (screenHeightPx * 0.62f) else 400f)
        }
        var isDockedLeft by remember { mutableStateOf(false) }

        // Tap handling function with clear user feedback
        fun handleBubbleTap() {
            tapJob?.cancel()
            tapCount++
            when (tapCount) {
                1 -> tapFeedbackText = "1x • الصوت 🔊"
                2 -> tapFeedbackText = "2x • المذكرة 📝"
                3 -> tapFeedbackText = "3x • الرئيسية 🏠"
            }

            if (tapCount >= 3) {
                tapCount = 0
                tapFeedbackText = null
                showVolumeHud = false
                isExpanded = false
                onNavigateHome()
                Toast.makeText(context, "FileHub Pro - تم فتح التطبيق والواجهة الرئيسية 🏠", Toast.LENGTH_SHORT).show()
            } else {
                tapJob = scope.launch {
                    delay(330)
                    when (tapCount) {
                        1 -> {
                            // 1 Tap: Toggle Volume Controls HUD
                            showVolumeHud = !showVolumeHud
                        }
                        2 -> {
                            // 2 Taps: Open Floating Notepad & Links Vault
                            showVolumeHud = false
                            isExpanded = true
                        }
                    }
                    tapCount = 0
                    tapFeedbackText = null
                }
            }
        }

        // Floating Hub Component (Visible when not expanded)
        if (!isExpanded) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(bubbleX.roundToInt(), bubbleY.roundToInt()) }
            ) {
                // Volume Controls Floating HUD (Appears on 1 Tap!)
                AnimatedVisibility(
                    visible = showVolumeHud,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                    modifier = Modifier.offset {
                        // Position HUD to left or right of bubble depending on dock position
                        val hudWidthPx = with(density) { 260.dp.toPx() }
                        val xOffset = if (isDockedLeft) (bubbleSizePx + 8f) else (-hudWidthPx - 8f)
                        IntOffset(xOffset.roundToInt(), -40)
                    }
                ) {
                    Surface(
                        modifier = Modifier
                            .width(260.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .shadow(16.dp, RoundedCornerShape(20.dp))
                            .border(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .testTag("floating_volume_hud"),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f),
                        tonalElevation = 10.dp
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            // HUD Header with Volume % and Close button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (volumeState.isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        tint = if (volumeState.isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "مستوى الصوت: ${(volumeState.volumePercent * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { showVolumeHud = false },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Volume Adjustment Row: Vol-, Slider, Vol+
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { volumeManager.volumeDown() },
                                    modifier = Modifier.size(34.dp).testTag("floating_vol_down")
                                ) {
                                    Icon(Icons.Default.VolumeDown, contentDescription = "Vol -", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }

                                Slider(
                                    value = volumeState.volumePercent,
                                    onValueChange = { volumeManager.setVolume(it) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(28.dp)
                                )

                                IconButton(
                                    onClick = { volumeManager.volumeUp() },
                                    modifier = Modifier.size(34.dp).testTag("floating_vol_up")
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Vol +", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }

                            // Quick Shortcut Buttons Row (Notepad 2x & Home 3x)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    color = Color.Transparent
                                ) {
                                    TextButton(
                                        onClick = {
                                            showVolumeHud = false
                                            isExpanded = true
                                        },
                                        modifier = Modifier.fillMaxWidth().height(28.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("المذكرة (2x)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                                    color = Color.Transparent
                                ) {
                                    TextButton(
                                        onClick = {
                                            showVolumeHud = false
                                            onNavigateHome()
                                            Toast.makeText(context, "الواجهة الرئيسية 🏠", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.fillMaxWidth().height(28.dp)
                                    ) {
                                        Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("الرئيسية (3x)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            }
                        }
                    }
                }

                // Tap Count Visual Indicator Badge (Appears floating above bubble while tapping)
                AnimatedVisibility(
                    visible = tapFeedbackText != null,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier.offset(y = (-28).dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 6.dp
                    ) {
                        Text(
                            text = tapFeedbackText ?: "",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Main Sleek Floating Bubble (Distinctive Glowing Orb, Non-Intrusive & Draggable)
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .shadow(10.dp, CircleShape)
                        .border(
                            width = 2.5.dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.tertiary,
                                    MaterialTheme.colorScheme.primary
                                )
                            ),
                            shape = CircleShape
                        )
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                val startX = down.position.x
                                val startY = down.position.y
                                var hasMoved = false

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    if (change.pressed) {
                                        val dx = change.position.x - startX
                                        val dy = change.position.y - startY
                                        if (!hasMoved && hypot(dx, dy) > 14f) {
                                            hasMoved = true
                                        }
                                        if (hasMoved) {
                                            change.consume()
                                            val minY = 60f
                                            val maxY = if (screenHeightPx > 0) screenHeightPx - bubbleSizePx - 100f else 1200f
                                            val minX = marginPx
                                            val maxX = if (screenWidthPx > 0) screenWidthPx - bubbleSizePx - marginPx else 800f

                                            bubbleY = (bubbleY + (change.position.y - change.previousPosition.y)).coerceIn(minY, maxY)
                                            bubbleX = (bubbleX + (change.position.x - change.previousPosition.x)).coerceIn(minX, maxX)
                                        }
                                    } else {
                                        if (!hasMoved) {
                                            // Finger tapped crisply without dragging!
                                            handleBubbleTap()
                                        } else {
                                            // Drag completed: magnetic snap to closest screen edge (left or right)
                                            val midX = screenWidthPx / 2f
                                            if (bubbleX < midX) {
                                                isDockedLeft = true
                                                bubbleX = marginPx
                                            } else {
                                                isDockedLeft = false
                                                bubbleX = screenWidthPx - bubbleSizePx - marginPx
                                            }
                                        }
                                        break
                                    }
                                }
                            }
                        }
                        .testTag("floating_messenger_bubble"),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                    tonalElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        BadgedBox(
                            badge = {
                                if (allNotes.isNotEmpty()) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text("${allNotes.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (showVolumeHud) Icons.Default.VolumeUp else Icons.Default.Edit,
                                contentDescription = "Floating Hub (1x: Vol, 2x: Notes, 3x: Home)",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        // Expanded Floating Window (Messenger-style, opened on 2 Taps!)
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .imePadding()
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxHeight(0.88f)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .shadow(20.dp, RoundedCornerShape(24.dp))
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    // Header with Title & Close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Quick Notes & Links Vault",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "خزنة الملاحظات والحافظة العائمة",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        IconButton(onClick = { isExpanded = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Gesture Shortcuts Guide Banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "💡 اختصارات الأيقونة العائمة:\nضغطة 1 = الصوت | ضغطتان = المذكرة | 3 ضغطات = فتح التطبيق",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Device Volume Controls Row
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { volumeManager.volumeDown() },
                                modifier = Modifier.size(32.dp).testTag("floating_vol_down")
                            ) {
                                Icon(Icons.Default.VolumeDown, contentDescription = "Vol -", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }

                            Slider(
                                value = volumeState.volumePercent,
                                onValueChange = { volumeManager.setVolume(it) },
                                modifier = Modifier.weight(1f).height(30.dp)
                            )

                            IconButton(
                                onClick = { volumeManager.volumeUp() },
                                modifier = Modifier.size(32.dp).testTag("floating_vol_up")
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Vol +", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }

                            IconButton(
                                onClick = { volumeManager.toggleMute() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (volumeState.isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                    contentDescription = "Mute",
                                    tint = if (volumeState.isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Paste & Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val clipText = clipboard.getText()?.text
                                if (!clipText.isNullOrBlank()) {
                                    notepadManager.addNote(
                                        folderId = selectedFolderId ?: "f_clipboard",
                                        title = if (clipText.startsWith("http")) "Pasted Link" else "Clipboard Snippet",
                                        content = clipText,
                                        type = if (clipText.startsWith("http")) NoteType.LINK else NoteType.CLIPBOARD_SNIPPET
                                    )
                                    Toast.makeText(context, "تم حفظ النص من الحافظة! ✓", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "الحافظة فارغة حالياً", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("لصق سريع", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }

                        Button(
                            onClick = { showAddNoteDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ملاحظة جديدة", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Folders Filter Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedFolderId == null,
                            onClick = { selectedFolderId = null },
                            label = { Text("الكل", maxLines = 1, softWrap = false) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        folders.forEach { folder ->
                            FilterChip(
                                selected = selectedFolderId == folder.id,
                                onClick = { selectedFolderId = folder.id },
                                label = { Text(folder.name, maxLines = 1, softWrap = false) },
                                leadingIcon = {
                                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Notes List
                    val filteredNotes = notepadManager.getNotesForFolder(selectedFolderId)
                    if (filteredNotes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد ملاحظات أو روابط في هذا المجلد بعد.\nاضغط 'لصق سريع' أو 'ملاحظة جديدة' لحفظ نصوصك!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredNotes, key = { it.id }) { note ->
                                NoteCardItem(
                                    note = note,
                                    onCopy = {
                                        clipboard.setText(AnnotatedString(note.content))
                                        Toast.makeText(context, "تم النسخ للحافظة!", Toast.LENGTH_SHORT).show()
                                    },
                                    onOpenUrl = { onOpenUrlInBrowser(note.content.trim()) },
                                    onPinToggle = { notepadManager.togglePin(note.id) },
                                    onDelete = { notepadManager.deleteNote(note.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Note / Link Dialog
    if (showAddNoteDialog) {
        var noteTitle by remember { mutableStateOf("") }
        var noteContent by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("حفظ ملاحظة أو رابط") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                ) {
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("العنوان (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { noteContent = it },
                        label = { Text("النص أو الرابط...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteContent.isNotBlank()) {
                            notepadManager.addNote(
                                folderId = selectedFolderId ?: "f_clipboard",
                                title = noteTitle,
                                content = noteContent
                            )
                            showAddNoteDialog = false
                            Toast.makeText(context, "تم حفظ الملاحظة!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun NoteCardItem(
    note: NoteItem,
    onCopy: () -> Unit,
    onOpenUrl: () -> Unit,
    onPinToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (note.isPinned) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (note.isUrl) Icons.Default.Link else Icons.Default.Edit,
                        contentDescription = null,
                        tint = if (note.isUrl) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }

                Row {
                    IconButton(onClick = onPinToggle, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pin",
                            tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = note.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (note.isUrl) {
                    TextButton(
                        onClick = onOpenUrl,
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("فتح الرابط", style = MaterialTheme.typography.labelSmall)
                    }
                }

                TextButton(
                    onClick = onCopy,
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("نسخ", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
