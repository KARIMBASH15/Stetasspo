package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.manager.FileManager
import com.example.manager.PermissionsManager
import com.example.manager.StorageShortcut
import com.example.manager.VaultManager
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.model.MediaTrack
import com.example.model.SortOption
import com.example.service.PlaybackController
import com.example.ui.components.FileCardItem
import com.example.ui.components.FileDetailsBottomSheet
import com.example.ui.components.MultiSelectActionBar
import com.example.ui.components.TextHtmlEditorDialog
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    fileManager: FileManager,
    vaultManager: VaultManager,
    permissionsManager: PermissionsManager? = null,
    onOpenMedia: (MediaTrack) -> Unit,
    onOpenHtmlInBrowser: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentDir by remember { mutableStateOf(fileManager.getRootDirectory()) }
    var selectedCategory by remember { mutableStateOf(FileCategory.ALL) }
    var sortOption by remember { mutableStateOf(SortOption.NAME_ASC) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val fileItems = remember { mutableStateListOf<FileItem>() }
    var isSelectionMode by remember { mutableStateOf(false) }
    var speedDialOpen by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    // Dialogs state
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<FileItem?>(null) }
    var showDetailsSheet by remember { mutableStateOf<FileItem?>(null) }
    var calculatedMd5 by remember { mutableStateOf("") }
    var showEditorDialog by remember { mutableStateOf<FileItem?>(null) }
    var editorInitialContent by remember { mutableStateOf("") }

    // Directory navigation & back press handler
    val canGoBack = currentDir.absolutePath != fileManager.getRootDirectory().absolutePath
    BackHandler(enabled = canGoBack || isSelectionMode) {
        if (isSelectionMode) {
            isSelectionMode = false
            fileItems.forEachIndexed { i, item -> fileItems[i] = item.copy(isSelected = false) }
        } else if (canGoBack) {
            currentDir.parentFile?.let { currentDir = it }
        }
    }

    fun refreshFiles() {
        scope.launch {
            val list = fileManager.listDirectory(
                directory = currentDir,
                category = selectedCategory,
                query = searchQuery,
                sortOption = sortOption
            )
            fileItems.clear()
            fileItems.addAll(list)
        }
    }

    LaunchedEffect(currentDir, selectedCategory, sortOption, searchQuery) {
        refreshFiles()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (speedDialOpen) {
                    if (fileManager.clipboardFiles.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("لصق (${fileManager.clipboardFiles.size})", modifier = Modifier.padding(6.dp), style = MaterialTheme.typography.labelSmall)
                            }
                            SmallFloatingActionButton(
                                onClick = {
                                    speedDialOpen = false
                                    scope.launch {
                                        val ok = fileManager.pasteClipboard(currentDir)
                                        Toast.makeText(context, if (ok) "تم اللصق بنجاح" else "تعذر اللصق", Toast.LENGTH_SHORT).show()
                                        refreshFiles()
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.tertiary
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "لصق")
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("ملف جديد", modifier = Modifier.padding(6.dp), style = MaterialTheme.typography.labelSmall)
                        }
                        SmallFloatingActionButton(
                            onClick = {
                                speedDialOpen = false
                                showNewFileDialog = true
                            },
                            containerColor = MaterialTheme.colorScheme.secondary
                        ) {
                            Icon(Icons.Default.NoteAdd, contentDescription = "ملف جديد")
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("مجلد جديد", modifier = Modifier.padding(6.dp), style = MaterialTheme.typography.labelSmall)
                        }
                        SmallFloatingActionButton(
                            onClick = {
                                speedDialOpen = false
                                showNewFolderDialog = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = "مجلد جديد")
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { speedDialOpen = !speedDialOpen },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_add")
                ) {
                    Icon(
                        imageVector = if (speedDialOpen) Icons.Default.Clear else Icons.Default.Add,
                        contentDescription = "إجراءات"
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search / Top Header Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_field"),
                            placeholder = { Text("البحث في جميع الملفات والمجلدات...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    isSearchActive = false
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "إغلاق البحث")
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (canGoBack) {
                                    IconButton(onClick = { currentDir.parentFile?.let { currentDir = it } }) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                                    }
                                }
                                Column {
                                    Text(
                                        text = if (currentDir.absolutePath == fileManager.getRootDirectory().absolutePath) "الذاكرة الداخلية" else currentDir.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${fileItems.size} عنصر",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row {
                                IconButton(onClick = { isSearchActive = true }) {
                                    Icon(Icons.Default.Search, contentDescription = "بحث")
                                }
                                Box {
                                    IconButton(onClick = { sortMenuOpen = true }) {
                                        Icon(Icons.Default.Sort, contentDescription = "ترتيب")
                                    }
                                    DropdownMenu(
                                        expanded = sortMenuOpen,
                                        onDismissRequest = { sortMenuOpen = false }
                                    ) {
                                        SortOption.values().forEach { opt ->
                                            DropdownMenuItem(
                                                text = { Text(opt.titleAr) },
                                                onClick = {
                                                    sortOption = opt
                                                    sortMenuOpen = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Breadcrumbs Path
                    val pathSegments = currentDir.absolutePath.split("/").filter { it.isNotEmpty() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الذاكرة الرئيسية",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { currentDir = fileManager.getRootDirectory() }
                        )
                        pathSegments.takeLast(3).forEach { segment ->
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = segment,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Missing All Files Permission Banner
            if (permissionsManager != null && !permissionsManager.hasAllFilesAccess()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "إذن إدارة جميع الملفات غير مفعل",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "مطلوب لتصفح وتعديل كافة مجلدات الذاكرة بدون قيود",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = { permissionsManager.openAllFilesAccessSettings() },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("تفعيل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Quick Storage Shortcuts Carousel (Internal Storage, Downloads, DCIM, Documents, Music, Videos)
            val shortcuts = remember { fileManager.getStorageShortcuts() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                shortcuts.forEach { shortcut ->
                    val isSelected = currentDir.absolutePath == shortcut.directory.absolutePath
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = if (isSelected) 4.dp else 0.dp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                currentDir = shortcut.directory
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = when (shortcut.iconType) {
                                    "storage" -> Icons.Default.Storage
                                    "downloads" -> Icons.Default.Download
                                    "camera" -> Icons.Default.PhotoCamera
                                    "documents" -> Icons.Default.Description
                                    "music" -> Icons.Default.Headphones
                                    "videos" -> Icons.Default.SmartDisplay
                                    else -> Icons.Default.Folder
                                },
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = shortcut.nameAr,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Category Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FileCategory.values().forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.labelAr) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Multi-selection bar if items selected
            val selectedItems = fileItems.filter { it.isSelected }
            if (isSelectionMode || selectedItems.isNotEmpty()) {
                MultiSelectActionBar(
                    selectedCount = selectedItems.size,
                    onSelectAll = {
                        val allSelected = fileItems.all { it.isSelected }
                        fileItems.forEachIndexed { i, itm -> fileItems[i] = itm.copy(isSelected = !allSelected) }
                    },
                    onCopy = {
                        fileManager.setClipboard(selectedItems.map { it.file }, cutMode = false)
                        Toast.makeText(context, "Copied ${selectedItems.size} items", Toast.LENGTH_SHORT).show()
                        isSelectionMode = false
                    },
                    onCut = {
                        fileManager.setClipboard(selectedItems.map { it.file }, cutMode = true)
                        Toast.makeText(context, "Cut ${selectedItems.size} items", Toast.LENGTH_SHORT).show()
                        isSelectionMode = false
                    },
                    onZip = {
                        scope.launch {
                            val zipFile = File(currentDir, "Archive_${System.currentTimeMillis()}.zip")
                            val success = fileManager.zipFiles(selectedItems.map { it.file }, zipFile)
                            Toast.makeText(context, if (success) "Zipped to ${zipFile.name}" else "Failed to zip", Toast.LENGTH_SHORT).show()
                            isSelectionMode = false
                            refreshFiles()
                        }
                    },
                    onVault = {
                        scope.launch {
                            var lockedCount = 0
                            selectedItems.forEach { item ->
                                if (vaultManager.lockFile(item.file, item.mimeType)) lockedCount++
                            }
                            Toast.makeText(context, "Moved $lockedCount items to Safe Vault", Toast.LENGTH_SHORT).show()
                            isSelectionMode = false
                            refreshFiles()
                        }
                    },
                    onDelete = {
                        scope.launch {
                            fileManager.deleteFiles(selectedItems.map { it.file })
                            Toast.makeText(context, "Deleted ${selectedItems.size} items", Toast.LENGTH_SHORT).show()
                            isSelectionMode = false
                            refreshFiles()
                        }
                    },
                    onClose = {
                        isSelectionMode = false
                        fileItems.forEachIndexed { i, itm -> fileItems[i] = itm.copy(isSelected = false) }
                    }
                )
            }

            // Files List
            if (fileItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No files found in this category",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = {
                            selectedCategory = FileCategory.ALL
                            searchQuery = ""
                        }) {
                            Text("Reset filters")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(fileItems, key = { it.path }) { item ->
                        FileCardItem(
                            item = item,
                            isSelectionMode = isSelectionMode,
                            onItemClick = {
                                if (isSelectionMode) {
                                    val index = fileItems.indexOf(item)
                                    if (index != -1) fileItems[index] = item.copy(isSelected = !item.isSelected)
                                } else {
                                    if (item.isDirectory) {
                                        currentDir = item.file
                                    } else {
                                        when (item.category) {
                                            FileCategory.AUDIO -> {
                                                val track = MediaTrack(
                                                    id = item.path,
                                                    title = item.name.substringBeforeLast("."),
                                                    artist = "Local Audio",
                                                    durationMs = 0L,
                                                    path = item.path,
                                                    isVideo = false,
                                                    size = item.size
                                                )
                                                PlaybackController.playTrack(track, context)
                                                onOpenMedia(track)
                                            }
                                            FileCategory.VIDEO -> {
                                                val track = MediaTrack(
                                                    id = item.path,
                                                    title = item.name.substringBeforeLast("."),
                                                    artist = "Video Player",
                                                    durationMs = 0L,
                                                    path = item.path,
                                                    isVideo = true,
                                                    size = item.size
                                                )
                                                PlaybackController.playTrack(track, context)
                                                onOpenMedia(track)
                                            }
                                            FileCategory.HTML -> {
                                                showEditorDialog = item
                                                scope.launch {
                                                    editorInitialContent = try { item.file.readText() } catch (_: Exception) { "" }
                                                }
                                            }
                                            FileCategory.DOCUMENTS -> {
                                                if (item.extension in listOf("txt", "json", "md", "csv", "log", "xml")) {
                                                    showEditorDialog = item
                                                    scope.launch {
                                                        editorInitialContent = try { item.file.readText() } catch (_: Exception) { "" }
                                                    }
                                                } else {
                                                    Toast.makeText(context, "Opening ${item.name}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            FileCategory.ARCHIVES -> {
                                                // Prompt unzip
                                                scope.launch {
                                                    val ok = fileManager.unzipFile(item.file, currentDir)
                                                    Toast.makeText(context, if (ok) "Extracted successfully" else "Extraction failed", Toast.LENGTH_SHORT).show()
                                                    refreshFiles()
                                                }
                                            }
                                            else -> {
                                                Toast.makeText(context, "File: ${item.name}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            },
                            onItemLongClick = {
                                isSelectionMode = true
                                val index = fileItems.indexOf(item)
                                if (index != -1) fileItems[index] = item.copy(isSelected = true)
                            },
                            onSelectToggle = { checked ->
                                val index = fileItems.indexOf(item)
                                if (index != -1) fileItems[index] = item.copy(isSelected = checked)
                            },
                            onDetailsClick = {
                                showDetailsSheet = item
                                scope.launch {
                                    calculatedMd5 = fileManager.calculateMd5(item.file)
                                }
                            },
                            onRenameClick = { showRenameDialog = item },
                            onCopyClick = {
                                fileManager.setClipboard(listOf(item.file), cutMode = false)
                                Toast.makeText(context, "Copied ${item.name}", Toast.LENGTH_SHORT).show()
                            },
                            onCutClick = {
                                fileManager.setClipboard(listOf(item.file), cutMode = true)
                                Toast.makeText(context, "Cut ${item.name}", Toast.LENGTH_SHORT).show()
                            },
                            onDeleteClick = {
                                scope.launch {
                                    fileManager.deleteFiles(listOf(item.file))
                                    Toast.makeText(context, "Deleted ${item.name}", Toast.LENGTH_SHORT).show()
                                    refreshFiles()
                                }
                            },
                            onZipClick = {
                                scope.launch {
                                    val zipDest = File(currentDir, "${item.name.substringBeforeLast(".")}.zip")
                                    val ok = fileManager.zipFiles(listOf(item.file), zipDest)
                                    Toast.makeText(context, if (ok) "Created ${zipDest.name}" else "Zip failed", Toast.LENGTH_SHORT).show()
                                    refreshFiles()
                                }
                            },
                            onUnzipClick = {
                                scope.launch {
                                    val ok = fileManager.unzipFile(item.file, currentDir)
                                    Toast.makeText(context, if (ok) "Unzipped successfully" else "Unzip failed", Toast.LENGTH_SHORT).show()
                                    refreshFiles()
                                }
                            },
                            onLockToVaultClick = {
                                scope.launch {
                                    val ok = vaultManager.lockFile(item.file, item.mimeType)
                                    Toast.makeText(context, if (ok) "Moved to Safe Vault" else "Failed to lock", Toast.LENGTH_SHORT).show()
                                    refreshFiles()
                                }
                            },
                            onOpenInBrowser = {
                                onOpenHtmlInBrowser("file://${item.file.absolutePath}")
                            }
                        )
                    }
                }
            }
        }
    }

    // New Folder Dialog
    if (showNewFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("إنشاء مجلد جديد") },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("اسم المجلد") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            scope.launch {
                                fileManager.createDirectory(currentDir, folderName.trim())
                                showNewFolderDialog = false
                                refreshFiles()
                            }
                        }
                    }
                ) { Text("إنشاء") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // New HTML/Text File Dialog
    if (showNewFileDialog) {
        var fileName by remember { mutableStateOf("new_page.html") }
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("إنشاء ملف جديد") },
            text = {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("اسم الملف (مثال: page.html أو notes.txt)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (fileName.isNotBlank()) {
                            scope.launch {
                                val template = if (fileName.endsWith(".html")) {
                                    "<!DOCTYPE html>\n<html>\n<head><title>صفحتي</title></head>\n<body>\n<h1>مرحباً بك في FileHub!</h1>\n</body>\n</html>"
                                } else {
                                    "تم الإنشاء بواسطة FileHub Pro"
                                }
                                fileManager.createTextFile(currentDir, fileName.trim(), template)
                                showNewFileDialog = false
                                refreshFiles()
                            }
                        }
                    }
                ) { Text("إنشاء") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Rename Dialog
    showRenameDialog?.let { item ->
        var newName by remember { mutableStateOf(item.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("إعادة تسمية الملف") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("الاسم الجديد") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank() && newName != item.name) {
                            scope.launch {
                                fileManager.renameFile(item.file, newName.trim())
                                showRenameDialog = null
                                refreshFiles()
                            }
                        }
                    }
                ) { Text("تعديل") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) { Text("إلغاء") }
            }
        )
    }

    // Details Sheet
    showDetailsSheet?.let { item ->
        FileDetailsBottomSheet(
            item = item,
            md5Checksum = calculatedMd5.ifEmpty { "Calculating..." },
            onDismiss = { showDetailsSheet = null }
        )
    }

    // Text & HTML Editor Dialog
    showEditorDialog?.let { item ->
        TextHtmlEditorDialog(
            file = item.file,
            initialContent = editorInitialContent,
            onSave = { newContent ->
                scope.launch {
                    item.file.writeText(newContent)
                    Toast.makeText(context, "Saved ${item.name}", Toast.LENGTH_SHORT).show()
                    showEditorDialog = null
                    refreshFiles()
                }
            },
            onRunInBrowser = { code ->
                scope.launch {
                    item.file.writeText(code)
                    showEditorDialog = null
                    onOpenHtmlInBrowser("file://${item.file.absolutePath}")
                }
            },
            onDismiss = { showEditorDialog = null }
        )
    }
}
