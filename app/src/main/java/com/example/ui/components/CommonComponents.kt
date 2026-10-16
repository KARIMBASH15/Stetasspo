package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.model.MediaTrack
import com.example.model.PlaybackState
import com.example.service.PlaybackController
import java.io.File

@Composable
fun MiniPlayerBar(
    track: MediaTrack?,
    playbackState: PlaybackState,
    onExpandPlayer: () -> Unit,
    volumeManager: com.example.manager.DeviceVolumeManager? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    AnimatedVisibility(
        visible = track != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        if (track != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onExpandPlayer() }
                    .testTag("mini_player_bar"),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Column {
                    LinearProgressIndicator(
                        progress = { playbackState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (track.isVideo) Icons.Default.VideoFile else Icons.Default.AudioFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (playbackState.isBackgroundPlaybackEnabled) "${track.artist} • Background Mode" else track.artist,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        }

                        if (volumeManager != null) {
                            IconButton(
                                onClick = { volumeManager.volumeDown() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.VolumeDown,
                                    contentDescription = "Vol -",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { volumeManager.volumeUp() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.VolumeUp,
                                    contentDescription = "Vol +",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { PlaybackController.togglePlayPause(context) },
                            modifier = Modifier.size(36.dp).testTag("mini_play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = { PlaybackController.next(context) },
                            modifier = Modifier.size(36.dp).testTag("mini_next_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Track",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FileCardItem(
    item: FileItem,
    isSelectionMode: Boolean,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
    onSelectToggle: (Boolean) -> Unit,
    onDetailsClick: () -> Unit,
    onRenameClick: () -> Unit,
    onCopyClick: () -> Unit,
    onCutClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onZipClick: () -> Unit,
    onUnzipClick: () -> Unit,
    onLockToVaultClick: () -> Unit,
    onOpenInBrowser: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = onItemClick,
                onLongClick = onItemLongClick
            )
            .testTag("file_item_${item.name}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = item.isSelected,
                    onCheckedChange = onSelectToggle,
                    modifier = Modifier.testTag("checkbox_${item.name}")
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            // File / Folder Category Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(getCategoryBackgroundColor(item)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(item),
                    contentDescription = null,
                    tint = getCategoryTint(item),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (item.isDirectory) "${item.itemCount} items" else formatSize(item.size),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        softWrap = false,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatDate(item.lastModified),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("menu_${item.name}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Details (تفاصيل)") },
                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                        onClick = { menuExpanded = false; onDetailsClick() }
                    )
                    if (item.category == FileCategory.HTML && onOpenInBrowser != null) {
                        DropdownMenuItem(
                            text = { Text("Open in Browser (فتح بالمتصفح)") },
                            leadingIcon = { Icon(Icons.Default.Web, contentDescription = null) },
                            onClick = { menuExpanded = false; onOpenInBrowser() }
                        )
                    }
                    if (item.extension == "zip") {
                        DropdownMenuItem(
                            text = { Text("Unzip (فك الضغط)") },
                            leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                            onClick = { menuExpanded = false; onUnzipClick() }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Compress to ZIP (ضغط)") },
                            leadingIcon = { Icon(Icons.Default.FolderZip, contentDescription = null) },
                            onClick = { menuExpanded = false; onZipClick() }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Copy (نسخ)") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                        onClick = { menuExpanded = false; onCopyClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Cut / Move (قص)") },
                        leadingIcon = { Icon(Icons.Default.ContentCut, contentDescription = null) },
                        onClick = { menuExpanded = false; onCutClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Lock in Safe Vault (نقل للخزنة)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        onClick = { menuExpanded = false; onLockToVaultClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename (إعادة تسمية)") },
                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                        onClick = { menuExpanded = false; onRenameClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete (حذف)") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; onDeleteClick() }
                    )
                }
            }
        }
    }
}

@Composable
fun MultiSelectActionBar(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onZip: () -> Unit,
    onVault: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("multi_select_action_bar"),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
                Text(
                    text = "$selectedCount Selected",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Row {
                IconButton(onClick = onSelectAll) {
                    Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                }
                IconButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                }
                IconButton(onClick = onCut) {
                    Icon(Icons.Default.ContentCut, contentDescription = "Cut")
                }
                IconButton(onClick = onZip) {
                    Icon(Icons.Default.FolderZip, contentDescription = "Zip")
                }
                IconButton(onClick = onVault) {
                    Icon(Icons.Default.Lock, contentDescription = "Lock to Vault")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDetailsBottomSheet(
    item: FileItem,
    md5Checksum: String,
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var copiedText by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .testTag("file_details_sheet")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(getCategoryBackgroundColor(item)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(item),
                        contentDescription = null,
                        tint = getCategoryTint(item),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.mimeType,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            DetailRow("File Path (المسار)", item.path, onCopy = {
                clipboard.setText(AnnotatedString(item.path))
                copiedText = "Path copied"
            })
            DetailRow("Size (الحجم)", if (item.isDirectory) "${item.itemCount} items" else "${formatSize(item.size)} (${item.size} bytes)")
            DetailRow("Last Modified (تاريخ التعديل)", formatDate(item.lastModified))
            DetailRow("Category (النوع)", item.category.label)
            DetailRow("Permissions (الصلاحيات)", "Read: ${item.file.canRead()}, Write: ${item.file.canWrite()}, Exec: ${item.file.canExecute()}")
            DetailRow("MD5 Checksum (بصمة التشفير)", md5Checksum, onCopy = {
                clipboard.setText(AnnotatedString(md5Checksum))
                copiedText = "MD5 copied"
            })

            if (copiedText != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "✓ $copiedText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, onCopy: (() -> Unit)? = null) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (onCopy != null) {
                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TextHtmlEditorDialog(
    file: File,
    initialContent: String,
    onSave: (String) -> Unit,
    onRunInBrowser: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var content by remember { mutableStateOf(initialContent) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Editor: ${file.name}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .testTag("code_editor_field"),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    placeholder = { Text("Write code or text here...") }
                )
            }
        },
        confirmButton = {
            Row {
                if (file.extension.lowercase() in listOf("html", "htm")) {
                    TextButton(onClick = { onRunInBrowser(content) }) {
                        Icon(Icons.Default.Web, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Live Preview")
                    }
                }
                TextButton(onClick = { onSave(content) }) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun getCategoryIcon(item: FileItem): ImageVector {
    if (item.isDirectory) return Icons.Default.Folder
    return when (item.category) {
        FileCategory.AUDIO -> Icons.Default.AudioFile
        FileCategory.VIDEO -> Icons.Default.VideoFile
        FileCategory.IMAGES -> Icons.Default.Image
        FileCategory.DOCUMENTS -> Icons.Default.Description
        FileCategory.ARCHIVES -> Icons.Default.FolderZip
        FileCategory.HTML -> Icons.Default.Code
        else -> Icons.Default.Description
    }
}

fun getCategoryBackgroundColor(item: FileItem): Color {
    if (item.isDirectory) return Color(0xFF0284C7).copy(alpha = 0.15f)
    return when (item.category) {
        FileCategory.AUDIO -> Color(0xFF10B981).copy(alpha = 0.15f)
        FileCategory.VIDEO -> Color(0xFFF43F5E).copy(alpha = 0.15f)
        FileCategory.IMAGES -> Color(0xFF8B5CF6).copy(alpha = 0.15f)
        FileCategory.DOCUMENTS -> Color(0xFF0EA5E9).copy(alpha = 0.15f)
        FileCategory.ARCHIVES -> Color(0xFFF59E0B).copy(alpha = 0.15f)
        FileCategory.HTML -> Color(0xFFEC4899).copy(alpha = 0.15f)
        else -> Color(0xFF64748B).copy(alpha = 0.15f)
    }
}

fun getCategoryTint(item: FileItem): Color {
    if (item.isDirectory) return Color(0xFF0284C7)
    return when (item.category) {
        FileCategory.AUDIO -> Color(0xFF10B981)
        FileCategory.VIDEO -> Color(0xFFF43F5E)
        FileCategory.IMAGES -> Color(0xFF8B5CF6)
        FileCategory.DOCUMENTS -> Color(0xFF0EA5E9)
        FileCategory.ARCHIVES -> Color(0xFFF59E0B)
        FileCategory.HTML -> Color(0xFFEC4899)
        else -> Color(0xFF64748B)
    }
}

fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    val formatted = String.format(java.util.Locale.US, "%.1f", bytes / Math.pow(1024.0, digitGroups.toDouble()))
    return "$formatted ${units[digitGroups]}"
}

fun formatDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
