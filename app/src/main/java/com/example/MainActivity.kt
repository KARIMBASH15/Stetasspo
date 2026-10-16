package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Tune
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.manager.CloudDriveManager
import com.example.manager.DeviceVolumeManager
import com.example.manager.FileManager
import com.example.manager.NotepadManager
import com.example.manager.PermissionsManager
import com.example.manager.TabVisibilityManager
import com.example.manager.VaultManager
import com.example.manager.WebDownloadManager
import com.example.service.PlaybackController
import com.example.ui.components.ControlPanelDialog
import com.example.ui.components.FloatingNotepadBubble
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.PermissionsOnboardingDialog
import com.example.ui.components.ThemeSelectionDialog
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.CloudScreen
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.MediaScreen
import com.example.ui.screens.NotepadScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemeManager
import com.example.service.FloatingOverlayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        val initialTabRequest = MutableStateFlow<AppTab?>(null)
    }

    private lateinit var fileManager: FileManager
    private lateinit var vaultManager: VaultManager
    private lateinit var cloudDriveManager: CloudDriveManager
    private lateinit var webDownloadManager: WebDownloadManager
    private lateinit var permissionsManager: PermissionsManager
    private lateinit var volumeManager: DeviceVolumeManager
    private lateinit var notepadManager: NotepadManager
    private lateinit var tabVisibilityManager: TabVisibilityManager

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        val tab = when (intent?.getStringExtra("EXTRA_START_TAB")) {
            "files" -> AppTab.FILES
            "media" -> AppTab.MEDIA
            "browser" -> AppTab.BROWSER
            "cloud" -> AppTab.CLOUD
            "vault" -> AppTab.VAULT
            "notepad" -> AppTab.NOTEPAD
            "permissions" -> AppTab.PERMISSIONS
            else -> null
        }
        if (tab != null) {
            initialTabRequest.value = tab
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        ThemeManager.init(applicationContext)

        // Pre-create Chromium Code Cache directory hierarchy to prevent opendir error logs
        try {
            java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/js").mkdirs()
            java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm").mkdirs()
        } catch (_: Exception) {}

        fileManager = FileManager(applicationContext)
        vaultManager = VaultManager(applicationContext)
        cloudDriveManager = CloudDriveManager(applicationContext)
        webDownloadManager = WebDownloadManager(applicationContext)
        permissionsManager = PermissionsManager(applicationContext)
        volumeManager = DeviceVolumeManager(applicationContext)
        notepadManager = NotepadManager(applicationContext)
        tabVisibilityManager = TabVisibilityManager(applicationContext)

        setContent {
            MyApplicationTheme {
                MainApp(
                    fileManager = fileManager,
                    vaultManager = vaultManager,
                    cloudDriveManager = cloudDriveManager,
                    downloadManager = webDownloadManager,
                    permissionsManager = permissionsManager,
                    volumeManager = volumeManager,
                    notepadManager = notepadManager,
                    tabVisibilityManager = tabVisibilityManager
                )
            }
        }
    }
}

enum class AppTab(val title: String, val titleAr: String) {
    FILES("Files", "الملفات"),
    MEDIA("Media", "الميديا"),
    BROWSER("YouTube", "يوتيوب"),
    CLOUD("Cloud", "السحابة"),
    VAULT("Vault", "الخزنة"),
    NOTEPAD("Notepad", "المذكرة"),
    PERMISSIONS("Permissions", "الأذونات")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    fileManager: FileManager,
    vaultManager: VaultManager,
    cloudDriveManager: CloudDriveManager,
    downloadManager: WebDownloadManager,
    permissionsManager: PermissionsManager,
    volumeManager: DeviceVolumeManager,
    notepadManager: NotepadManager,
    tabVisibilityManager: TabVisibilityManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val visibleTabs by tabVisibilityManager.visibleTabs.collectAsState()
    var selectedTab by remember { mutableStateOf(visibleTabs.firstOrNull() ?: AppTab.FILES) }
    var browserInitialUrl by remember { mutableStateOf("https://m.youtube.com") }

    val currentTrack by PlaybackController.currentTrack.collectAsState()
    val playbackState by PlaybackController.playbackState.collectAsState()
    val currentTheme by ThemeManager.currentTheme.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showControlPanelDialog by remember { mutableStateOf(false) }
    var showPermissionsOnboardingDialog by remember {
        mutableStateOf(!permissionsManager.hasCompletedFirstTimePermissions || !permissionsManager.hasAllFilesAccess())
    }
    var isTopShortcutsBarVisible by remember { mutableStateOf(true) }
    val isOverlayActive by FloatingOverlayService.isOverlayActive.collectAsState()
    val externalTabRequest by MainActivity.initialTabRequest.collectAsState()

    LaunchedEffect(externalTabRequest) {
        externalTabRequest?.let {
            selectedTab = it
            MainActivity.initialTabRequest.value = null
        }
    }

    // Ensure selected tab is always in visible tabs
    LaunchedEffect(visibleTabs) {
        if (!visibleTabs.contains(selectedTab)) {
            selectedTab = visibleTabs.firstOrNull() ?: AppTab.FILES
        }
    }

    // Initialize sample files safely for immediate testing
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                fileManager.initializeSampleFiles()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth()) {
                    TopAppBar(
                        title = {
                            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = when (selectedTab) {
                                                AppTab.FILES -> Icons.Default.Folder
                                                AppTab.MEDIA -> Icons.Default.Headphones
                                                AppTab.BROWSER -> Icons.Default.SmartDisplay
                                                AppTab.CLOUD -> Icons.Default.Cloud
                                                AppTab.VAULT -> Icons.Default.Lock
                                                AppTab.NOTEPAD -> Icons.Default.Edit
                                                AppTab.PERMISSIONS -> Icons.Default.Security
                                            },
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                                androidx.compose.foundation.layout.Column {
                                    Text(
                                        text = when (selectedTab) {
                                            AppTab.FILES -> "مدير الملفات FileHub"
                                            AppTab.MEDIA -> "مشغل الموسيقى والميديا"
                                            AppTab.BROWSER -> "يوتيوب وتصفح الويب"
                                            AppTab.CLOUD -> "التخزين السحابي ومحلل المساحة"
                                            AppTab.VAULT -> "الخزنة المشفرة والحماية"
                                            AppTab.NOTEPAD -> "المفكرة والحافظة العائمة"
                                            AppTab.PERMISSIONS -> "إدارة أذونات النظام"
                                        },
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = when (selectedTab) {
                                            AppTab.FILES -> "تصفح وتنظيم كامل مجلدات الذاكرة"
                                            AppTab.MEDIA -> "تشغيل الصوتيات والفيديوهات بالخلفية"
                                            AppTab.BROWSER -> "مشاهدة وتحميل الوسائط والويب"
                                            AppTab.CLOUD -> "إحصائيات الذاكرة والنسخ الاحتياطي"
                                            AppTab.VAULT -> "تشفير وحماية الملفات والصور الحساسة"
                                            AppTab.NOTEPAD -> "تسجيل سريع للملاحظات والروابط"
                                            AppTab.PERMISSIONS -> "صلاحيات الوصول والتخزين والوسائط"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        },
                        actions = {
                            // Quick Eye Button: Toggle top shortcuts bar so it doesn't crowd ("كي لا يزاحم")
                            IconButton(
                                onClick = { isTopShortcutsBarVisible = !isTopShortcutsBarVisible },
                                modifier = Modifier.testTag("toggle_top_shortcuts_eye_button")
                            ) {
                                Icon(
                                    imageVector = if (isTopShortcutsBarVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (isTopShortcutsBarVisible) "إخفاء شريط الاختصارات العلوي" else "إظهار شريط الاختصارات العلوي",
                                    tint = if (isTopShortcutsBarVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Control Panel button to customize bottom bar sections
                            IconButton(
                                onClick = { showControlPanelDialog = true },
                                modifier = Modifier.testTag("control_panel_button")
                            ) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = "لوحة التحكم بالأقسام",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Theme Picker Button
                            IconButton(
                                onClick = { showThemeDialog = true },
                                modifier = Modifier.testTag("theme_picker_button")
                            ) {
                                Icon(
                                    Icons.Default.Palette,
                                    contentDescription = "المظهر والألوان",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Permissions Governance Button
                            IconButton(
                                onClick = { selectedTab = AppTab.PERMISSIONS },
                                modifier = Modifier.testTag("nav_permissions_top_button")
                            ) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = "الأذونات",
                                    tint = if (selectedTab == AppTab.PERMISSIONS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Quick Top Shortcuts Bar for direct navigation, animated to hide/show on eye press ("كي لا يزاحم")
                    AnimatedVisibility(
                        visible = isTopShortcutsBarVisible,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        QuickTopShortcutsBar(
                            selectedTab = selectedTab,
                            onSelectTab = { selectedTab = it },
                            onToggleFloatingOverlay = {
                                if (permissionsManager.hasOverlayPermission()) {
                                    if (isOverlayActive) {
                                        FloatingOverlayService.stop(context)
                                    } else {
                                        FloatingOverlayService.start(context)
                                    }
                                } else {
                                    permissionsManager.openOverlaySettings()
                                }
                            },
                            isOverlayActive = isOverlayActive
                        )
                    }
                }
            },
            bottomBar = {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    // Persistent Mini Player above bottom bar with Device Volume controls
                    MiniPlayerBar(
                        track = currentTrack,
                        playbackState = playbackState,
                        onExpandPlayer = { selectedTab = AppTab.MEDIA },
                        volumeManager = volumeManager
                    )

                    // Dynamic Navigation Bar showing user's customized visible sections
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        visibleTabs.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = {
                                    when (tab) {
                                        AppTab.FILES -> Icon(Icons.Default.Folder, contentDescription = tab.titleAr)
                                        AppTab.MEDIA -> {
                                            BadgedBox(
                                                badge = {
                                                    if (playbackState.isPlaying) {
                                                        Badge { Text("▶") }
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Default.Headphones, contentDescription = tab.titleAr)
                                            }
                                        }
                                        AppTab.BROWSER -> Icon(Icons.Default.SmartDisplay, contentDescription = tab.titleAr)
                                        AppTab.CLOUD -> Icon(Icons.Default.Cloud, contentDescription = tab.titleAr)
                                        AppTab.VAULT -> Icon(Icons.Default.Lock, contentDescription = tab.titleAr)
                                        AppTab.NOTEPAD -> Icon(Icons.Default.Edit, contentDescription = tab.titleAr)
                                        AppTab.PERMISSIONS -> Icon(Icons.Default.Security, contentDescription = tab.titleAr)
                                    }
                                },
                                label = { Text(tab.titleAr, maxLines = 1, softWrap = false) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    AppTab.FILES -> {
                        FilesScreen(
                            fileManager = fileManager,
                            vaultManager = vaultManager,
                            permissionsManager = permissionsManager,
                            onOpenMedia = { track ->
                                selectedTab = AppTab.MEDIA
                            },
                            onOpenHtmlInBrowser = { url ->
                                browserInitialUrl = url
                                selectedTab = AppTab.BROWSER
                            }
                        )
                    }
                    AppTab.MEDIA -> {
                        MediaScreen(
                            fileManager = fileManager,
                            volumeManager = volumeManager
                        )
                    }
                    AppTab.BROWSER -> {
                        BrowserScreen(
                            downloadManager = downloadManager,
                            initialUrl = browserInitialUrl
                        )
                    }
                    AppTab.CLOUD -> {
                        CloudScreen(
                            fileManager = fileManager,
                            cloudDriveManager = cloudDriveManager
                        )
                    }
                    AppTab.VAULT -> {
                        VaultScreen(
                            vaultManager = vaultManager,
                            fileManager = fileManager
                        )
                    }
                    AppTab.NOTEPAD -> {
                        NotepadScreen(
                            notepadManager = notepadManager,
                            onOpenUrlInBrowser = { url ->
                                browserInitialUrl = url
                                selectedTab = AppTab.BROWSER
                            }
                        )
                    }
                    AppTab.PERMISSIONS -> {
                        PermissionsScreen(
                            permissionsManager = permissionsManager
                        )
                    }
                }

                // Floating Messenger-style Quick Notes & Link Vault Bubble with Vol+ and Vol- shortcuts
                FloatingNotepadBubble(
                    notepadManager = notepadManager,
                    volumeManager = volumeManager,
                    onOpenUrlInBrowser = { url ->
                        browserInitialUrl = url
                        selectedTab = AppTab.BROWSER
                    },
                    onNavigateHome = {
                        selectedTab = AppTab.FILES
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // First-time Launch Permissions Dialog
                if (showPermissionsOnboardingDialog) {
                    PermissionsOnboardingDialog(
                        permissionsManager = permissionsManager,
                        onDismissOrCompleted = {
                            showPermissionsOnboardingDialog = false
                            scope.launch {
                                fileManager.getStorageStats()
                            }
                        }
                    )
                }

                // Theme Selection Dialog
                if (showThemeDialog) {
                    ThemeSelectionDialog(
                        currentTheme = currentTheme,
                        onSelectTheme = { chosenTheme ->
                            ThemeManager.setTheme(chosenTheme, context)
                        },
                        onDismiss = { showThemeDialog = false }
                    )
                }

                // Section Visibility Control Panel Dialog
                if (showControlPanelDialog) {
                    ControlPanelDialog(
                        tabVisibilityManager = tabVisibilityManager,
                        onDismiss = { showControlPanelDialog = false }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickTopShortcutsBar(
    selectedTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
    onToggleFloatingOverlay: () -> Unit,
    isOverlayActive: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickShortcutChip(
                icon = Icons.Default.Folder,
                label = "الملفات والذاكرة",
                isSelected = selectedTab == AppTab.FILES,
                onClick = { onSelectTab(AppTab.FILES) }
            )
            QuickShortcutChip(
                icon = Icons.Default.SmartDisplay,
                label = "يوتيوب وتنزيل الفيديو",
                isSelected = selectedTab == AppTab.BROWSER,
                onClick = { onSelectTab(AppTab.BROWSER) }
            )
            QuickShortcutChip(
                icon = Icons.Default.Headphones,
                label = "مشغل الموسيقى",
                isSelected = selectedTab == AppTab.MEDIA,
                onClick = { onSelectTab(AppTab.MEDIA) }
            )
            QuickShortcutChip(
                icon = Icons.Default.Cloud,
                label = "محلل المساحة",
                isSelected = selectedTab == AppTab.CLOUD,
                onClick = { onSelectTab(AppTab.CLOUD) }
            )
            QuickShortcutChip(
                icon = Icons.Default.Lock,
                label = "الخزنة المشفرة",
                isSelected = selectedTab == AppTab.VAULT,
                onClick = { onSelectTab(AppTab.VAULT) }
            )
            QuickShortcutChip(
                icon = Icons.Default.Edit,
                label = "المفكرة السريعة",
                isSelected = selectedTab == AppTab.NOTEPAD,
                onClick = { onSelectTab(AppTab.NOTEPAD) }
            )
            QuickShortcutChip(
                icon = Icons.Default.Security,
                label = "أذونات النظام",
                isSelected = selectedTab == AppTab.PERMISSIONS,
                onClick = { onSelectTab(AppTab.PERMISSIONS) }
            )
            QuickShortcutChip(
                icon = Icons.Default.Layers,
                label = if (isOverlayActive) "الأيقونة العائمة: نشطة ✓" else "تفعيل الأيقونة العائمة فوق التطبيقات",
                isSelected = isOverlayActive,
                onClick = onToggleFloatingOverlay
            )
        }
    }
}

@Composable
fun QuickShortcutChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
