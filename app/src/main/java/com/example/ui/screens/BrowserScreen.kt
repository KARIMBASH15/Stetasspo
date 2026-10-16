package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import java.io.File
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.manager.WebDownloadManager
import com.example.model.BrowserBookmark
import com.example.model.DownloadStatus
import com.example.model.DownloadTask

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    downloadManager: WebDownloadManager,
    initialUrl: String = "https://m.youtube.com",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var urlInput by remember { mutableStateOf(initialUrl) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var pageTitle by remember { mutableStateOf("YouTube & Web") }
    var webProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var isDesktopMode by remember { mutableStateOf(false) }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    // Media sniffer / detected media on page
    var detectedMediaUrl by remember { mutableStateOf<String?>(null) }
    var showDownloadsSheet by remember { mutableStateOf(false) }
    val downloads by downloadManager.downloadTasks.collectAsState()

    val quickBookmarks = remember {
        listOf(
            BrowserBookmark("YouTube", "https://m.youtube.com", "youtube"),
            BrowserBookmark("Google", "https://www.google.com", "google"),
            BrowserBookmark("Wikipedia", "https://www.wikipedia.org", "wiki"),
            BrowserBookmark("Archive.org", "https://archive.org", "archive"),
            BrowserBookmark("HTML Sample", "https://www.w3schools.com/html/", "html")
        )
    }

    BackHandler(enabled = canGoBack) {
        webViewRef?.goBack()
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Address Bar & Controls
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { webViewRef?.goBack() },
                        enabled = canGoBack
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }

                    IconButton(
                        onClick = { webViewRef?.goForward() },
                        enabled = canGoForward
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Forward")
                    }

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("browser_address_bar"),
                        placeholder = { Text("Search or type URL...") },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        trailingIcon = {
                            if (isLoading) {
                                IconButton(onClick = { webViewRef?.stopLoading() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Stop")
                                }
                            } else {
                                IconButton(onClick = { webViewRef?.reload() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reload")
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    FilledTonalButton(
                        onClick = {
                            val target = formatUrl(urlInput)
                            currentUrl = target
                            webViewRef?.loadUrl(target)
                        },
                        shape = CircleShape,
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("Go")
                    }

                    BadgedBox(
                        badge = {
                            val activeCount = downloads.count { it.status == DownloadStatus.DOWNLOADING }
                            if (activeCount > 0) {
                                Badge { Text("$activeCount") }
                            }
                        }
                    ) {
                        IconButton(onClick = { showDownloadsSheet = true }) {
                            Icon(Icons.Default.Download, contentDescription = "Downloads")
                        }
                    }
                }

                // Progress Bar
                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { webProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Quick Bookmarks Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickBookmarks.forEach { bm ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                urlInput = bm.url
                                currentUrl = bm.url
                                webViewRef?.loadUrl(bm.url)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (bm.iconName == "youtube") Icons.Default.SmartDisplay else Icons.Default.Language,
                                    contentDescription = null,
                                    tint = if (bm.iconName == "youtube") Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(bm.title, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // Dedicated YouTube / Media banner notice
        if (currentUrl.contains("youtube.com") || currentUrl.contains("youtu.be")) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF991B1B).copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SmartDisplay, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "YouTube Mode: Desktop view & Background audio enabled",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    TextButton(onClick = {
                        isDesktopMode = !isDesktopMode
                        webViewRef?.settings?.userAgentString = if (isDesktopMode) {
                            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                        } else null
                        webViewRef?.reload()
                    }) {
                        Text(if (isDesktopMode) "Mobile" else "Desktop")
                    }
                }
            }
        }

        // Web View Area with floating Media Sniffer
        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                factory = { ctx ->
                    // Pre-create Chromium Code Cache directory hierarchy to prevent opendir error logs
                    try {
                        File(ctx.cacheDir, "WebView/Default/HTTP Cache/Code Cache/js").mkdirs()
                        File(ctx.cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm").mkdirs()
                    } catch (_: Exception) {}

                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // Avoid Mesa rendernode errors if hardware DRM render node is absent (container/emulator)
                        try {
                            val renderNode = File("/dev/dri/renderD128")
                            val isEmulator = android.os.Build.FINGERPRINT.contains("generic") ||
                                    android.os.Build.HARDWARE.contains("goldfish") ||
                                    android.os.Build.HARDWARE.contains("ranchu") ||
                                    android.os.Build.PRODUCT.contains("sdk")
                            if (!renderNode.exists() || isEmulator) {
                                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                            }
                        } catch (_: Exception) {
                            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                        }

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            builtInZoomControls = true
                            displayZoomControls = false
                        }

                        CookieManager.getInstance().setAcceptCookie(true)

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                webProgress = newProgress
                                isLoading = newProgress < 100
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                if (!title.isNullOrEmpty()) pageTitle = title
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                url?.let {
                                    urlInput = it
                                    currentUrl = it
                                }
                                canGoBack = canGoBack()
                                canGoForward = canGoForward()
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                canGoBack = canGoBack()
                                canGoForward = canGoForward()
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val reqUrl = request?.url?.toString() ?: return false
                                // Media stream interceptor
                                if (isMediaStreamUrl(reqUrl)) {
                                    detectedMediaUrl = reqUrl
                                }
                                return false
                            }
                        }

                        setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                            downloadManager.startDownload(url)
                            Toast.makeText(ctx, "Download started for: ${mimetype ?: "file"}", Toast.LENGTH_SHORT).show()
                            showDownloadsSheet = true
                        }

                        loadUrl(currentUrl)
                        webViewRef = this
                    }
                },
                update = { webView ->
                    // Keep references synced
                    canGoBack = webView.canGoBack()
                    canGoForward = webView.canGoForward()
                },
                modifier = Modifier.fillMaxSize()
            )

            // Floating Media Grabber Button when media is detected on page
            if (detectedMediaUrl != null) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val media = detectedMediaUrl
                        if (media != null) {
                            downloadManager.startDownload(media)
                            Toast.makeText(context, "Downloading detected media", Toast.LENGTH_SHORT).show()
                            detectedMediaUrl = null
                            showDownloadsSheet = true
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    icon = { Icon(Icons.Default.Download, contentDescription = null) },
                    text = { Text("Download Media") }
                )
            }
        }
    }

    // Downloads Drawer / Bottom Sheet
    if (showDownloadsSheet) {
        ModalBottomSheet(onDismissRequest = { showDownloadsSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Downloads (${downloads.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(onClick = { showDownloadsSheet = false }) {
                        Text("Done")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (downloads.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No active or recent downloads", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                        items(downloads, key = { it.id }) { task ->
                            DownloadTaskItem(task = task, onCancel = { downloadManager.removeTask(task.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadTaskItem(task: DownloadTask, onCancel: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.fileName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { task.progressPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${task.progressPercent}% • ${formatBytes(task.downloadedBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = task.status.name,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = when (task.status) {
                        DownloadStatus.COMPLETED -> Color(0xFF10B981)
                        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }
        }
    }
}

private fun formatUrl(input: String): String {
    val trimmed = input.trim()
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("file://")) {
        trimmed
    } else if (trimmed.contains(".") && !trimmed.contains(" ")) {
        "https://$trimmed"
    } else {
        "https://www.google.com/search?q=${java.net.URLEncoder.encode(trimmed, "UTF-8")}"
    }
}

private fun isMediaStreamUrl(url: String): Boolean {
    val lower = url.lowercase()
    return lower.endsWith(".mp4") || lower.endsWith(".mp3") || lower.endsWith(".m4a") ||
            lower.endsWith(".webm") || lower.endsWith(".m3u8") || lower.endsWith(".wav")
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 KB"
    val kb = bytes / 1024
    val mb = kb / 1024
    return if (mb > 0) "$mb MB" else "$kb KB"
}
