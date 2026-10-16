package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import java.io.File
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.manager.WebDownloadManager
import com.example.model.BrowserBookmark
import com.example.model.DownloadStatus
import com.example.model.DownloadTask

data class CapturedVideoInfo(
    val title: String,
    val videoUrl: String,
    val pageUrl: String,
    val isYouTube: Boolean = true
)

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
    var pageTitle by remember { mutableStateOf("يوتيوب والويب") }
    var webProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var isDesktopMode by remember { mutableStateOf(false) }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    // Toggle for hiding and showing top options above YouTube
    var isTopOptionsVisible by remember { mutableStateOf(true) }

    // Video grabber / detected video on page
    var capturedVideo by remember { mutableStateOf<CapturedVideoInfo?>(null) }
    var showDownloadsSheet by remember { mutableStateOf(false) }
    val downloads by downloadManager.downloadTasks.collectAsState()

    val quickBookmarks = remember {
        listOf(
            BrowserBookmark("يوتيوب", "https://m.youtube.com", "youtube"),
            BrowserBookmark("جوجل", "https://www.google.com", "google"),
            BrowserBookmark("ويكيبيديا", "https://ar.wikipedia.org", "wiki"),
            BrowserBookmark("أرشيف الإنترنت", "https://archive.org", "archive"),
            BrowserBookmark("نموذج HTML", "https://www.w3schools.com/html/", "html")
        )
    }

    BackHandler(enabled = canGoBack) {
        webViewRef?.goBack()
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Address Bar & Controls - Can be hidden and shown using "إظهار" / "إخفاء الخيارات"
        AnimatedVisibility(
            visible = isTopOptionsVisible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
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
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                        }

                        IconButton(
                            onClick = { webViewRef?.goForward() },
                            enabled = canGoForward
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "تقدم")
                        }

                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("browser_address_bar"),
                            placeholder = { Text("ابحث أو اكتب رابط الموقع...") },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            trailingIcon = {
                                if (isLoading) {
                                    IconButton(onClick = { webViewRef?.stopLoading() }) {
                                        Icon(Icons.Default.Close, contentDescription = "إيقاف")
                                    }
                                } else {
                                    IconButton(onClick = { webViewRef?.reload() }) {
                                        Icon(Icons.Default.Refresh, contentDescription = "تحديث")
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
                            Text("انتقال")
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
                                Icon(Icons.Default.Download, contentDescription = "التحميلات")
                            }
                        }

                        // Button to hide top options so they don't crowd the screen
                        IconButton(
                            onClick = { isTopOptionsVisible = false },
                            modifier = Modifier.testTag("hide_top_options_btn")
                        ) {
                            Icon(
                                Icons.Default.VisibilityOff,
                                contentDescription = "إخفاء الخيارات",
                                tint = MaterialTheme.colorScheme.primary
                            )
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

                    // Quick Bookmarks Row & Hide Options Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
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

                        // Text indicator to hide options
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier
                                .clickable { isTopOptionsVisible = false }
                                .padding(start = 6.dp)
                        ) {
                            Text(
                                text = "إخفاء الخيارات",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Dedicated YouTube banner notice (collapsible along with top options)
        AnimatedVisibility(
            visible = isTopOptionsVisible && (currentUrl.contains("youtube.com") || currentUrl.contains("youtu.be")),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
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
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.SmartDisplay, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "وضع يوتيوب: ملتقط الفيديو التلقائي والتشغيل بالخلفية نشط",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = {
                        isDesktopMode = !isDesktopMode
                        webViewRef?.settings?.userAgentString = if (isDesktopMode) {
                            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                        } else null
                        webViewRef?.reload()
                    }) {
                        Text(if (isDesktopMode) "جوال" else "سطح المكتب")
                    }
                }
            }
        }

        // Web View Area with floating "إظهار" button and Video Sniffer
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

                        // Add JavaScript Bridge to sniff video elements and titles
                        class VideoSnifferBridge {
                            @JavascriptInterface
                            fun onVideoFound(videoUrl: String, title: String, pageUrl: String) {
                                val cleanTitle = if (title.isNotBlank() && title != "YouTube" && title != "YouTube & Web") {
                                    title.replace(" - YouTube", "").trim()
                                } else {
                                    "فيديو يوتيوب"
                                }
                                capturedVideo = CapturedVideoInfo(
                                    title = cleanTitle,
                                    videoUrl = if (videoUrl.startsWith("http")) videoUrl else pageUrl,
                                    pageUrl = pageUrl,
                                    isYouTube = pageUrl.contains("youtube") || pageUrl.contains("youtu.be")
                                )
                            }
                        }

                        addJavascriptInterface(VideoSnifferBridge(), "AndroidVideoSniffer")

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                webProgress = newProgress
                                isLoading = newProgress < 100
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                if (!title.isNullOrEmpty()) {
                                    pageTitle = title
                                    if (currentUrl.contains("youtube.com/watch") || currentUrl.contains("youtu.be/")) {
                                        val cleanTitle = title.replace(" - YouTube", "").trim()
                                        capturedVideo = CapturedVideoInfo(
                                            title = cleanTitle,
                                            videoUrl = currentUrl,
                                            pageUrl = currentUrl,
                                            isYouTube = true
                                        )
                                    }
                                }
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                url?.let {
                                    urlInput = it
                                    currentUrl = it

                                    // If navigating to a video page on YouTube, immediately capture it!
                                    if (it.contains("youtube.com/watch") || it.contains("youtu.be/") || it.contains("/shorts/")) {
                                        val vidId = it.substringAfter("v=").substringBefore("&")
                                        val initialName = if (pageTitle.isNotBlank() && pageTitle != "YouTube & Web" && pageTitle != "YouTube") {
                                            pageTitle.replace(" - YouTube", "").trim()
                                        } else {
                                            "فيديو يوتيوب ($vidId)"
                                        }
                                        capturedVideo = CapturedVideoInfo(
                                            title = initialName,
                                            videoUrl = it,
                                            pageUrl = it,
                                            isYouTube = true
                                        )
                                    }
                                }
                                canGoBack = canGoBack()
                                canGoForward = canGoForward()
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                canGoBack = canGoBack()
                                canGoForward = canGoForward()

                                // Inject video sniffer script into the page
                                val jsSniffer = """
                                    (function() {
                                        function sniff() {
                                            var title = document.title || 'فيديو يوتيوب';
                                            var h1 = document.querySelector('h1.title') || document.querySelector('h1') || document.querySelector('.yt-core-attributed-string');
                                            if (h1 && h1.innerText && h1.innerText.length > 2) {
                                                title = h1.innerText.trim();
                                            }
                                            var v = document.querySelector('video');
                                            var vUrl = (v && v.src) ? v.src : window.location.href;
                                            if (window.location.href.indexOf('watch') !== -1 || window.location.href.indexOf('youtu') !== -1 || (v && v.src)) {
                                                if (window.AndroidVideoSniffer) {
                                                    window.AndroidVideoSniffer.onVideoFound(vUrl, title, window.location.href);
                                                }
                                            }
                                        }
                                        sniff();
                                        document.addEventListener('play', sniff, true);
                                    })();
                                """.trimIndent()
                                view?.evaluateJavascript(jsSniffer, null)
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val reqUrl = request?.url?.toString() ?: return false
                                // Media stream interceptor
                                if (isMediaStreamUrl(reqUrl)) {
                                    capturedVideo = CapturedVideoInfo(
                                        title = reqUrl.substringAfterLast("/").substringBefore("?").ifEmpty { "فيديو تم التقاطه" },
                                        videoUrl = reqUrl,
                                        pageUrl = currentUrl,
                                        isYouTube = currentUrl.contains("youtube")
                                    )
                                }
                                return false
                            }
                        }

                        setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                            downloadManager.startDownload(url)
                            Toast.makeText(ctx, "بدأ التحميل: ${mimetype ?: "ملف"}", Toast.LENGTH_SHORT).show()
                            showDownloadsSheet = true
                        }

                        loadUrl(currentUrl)
                        webViewRef = this
                    }
                },
                update = { webView ->
                    canGoBack = webView.canGoBack()
                    canGoForward = webView.canGoForward()
                },
                modifier = Modifier.fillMaxSize()
            )

            // When options above YouTube are hidden, show the "إظهار" button as explicitly requested ("وكلمة اظهار تظهرها")
            if (!isTopOptionsVisible) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clickable { isTopOptionsVisible = true }
                        .testTag("show_top_options_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "إظهار الخيارات",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إظهار",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // Prominent Arabic Video Capture & Download Card when video is detected
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = capturedVideo != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.SmartDisplay,
                                                contentDescription = null,
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "تم التقاط فيديو يوتيوب بنجاح! 🎬",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = capturedVideo?.title ?: "فيديو جاهز للتحميل",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { capturedVideo = null },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "إغلاق", modifier = Modifier.size(16.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Download MP4 Video button
                                Button(
                                    onClick = {
                                        val video = capturedVideo
                                        if (video != null) {
                                            val clean = video.title.take(30).replace(Regex("[^a-zA-Z0-9ء-ي_ ]"), "_").trim()
                                            val safeName = "${if (clean.isEmpty()) "youtube_video" else clean}.mp4"
                                            downloadManager.startDownload(video.videoUrl, safeName)
                                            Toast.makeText(context, "بدأ تحميل الفيديو: $safeName", Toast.LENGTH_SHORT).show()
                                            showDownloadsSheet = true
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("download_video_mp4_btn"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تحميل فيديو (MP4)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                // Download MP3 Audio button
                                FilledTonalButton(
                                    onClick = {
                                        val video = capturedVideo
                                        if (video != null) {
                                            val clean = video.title.take(30).replace(Regex("[^a-zA-Z0-9ء-ي_ ]"), "_").trim()
                                            val safeName = "${if (clean.isEmpty()) "youtube_audio" else clean}.mp3"
                                            downloadManager.startDownload(video.videoUrl, safeName)
                                            Toast.makeText(context, "بدأ تحميل الصوت: $safeName", Toast.LENGTH_SHORT).show()
                                            showDownloadsSheet = true
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("download_audio_mp3_btn"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تحميل صوت (MP3)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
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
                        text = "التحميلات الحالية (${downloads.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(onClick = { showDownloadsSheet = false }) {
                        Text("تم")
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
                        Text("لا توجد تحميلات نشطة حالياً", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Icon(Icons.Default.Close, contentDescription = "إلغاء", modifier = Modifier.size(16.dp))
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
                    text = when (task.status) {
                        DownloadStatus.COMPLETED -> "اكتمل التحميل ✓"
                        DownloadStatus.FAILED -> "فشل التحميل"
                        DownloadStatus.DOWNLOADING -> "جارٍ التحميل..."
                        else -> task.status.name
                    },
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
    return lower.contains(".googlevideo.com") || lower.endsWith(".mp4") || lower.endsWith(".mp3") ||
            lower.endsWith(".m4a") || lower.endsWith(".webm") || lower.endsWith(".m3u8") || lower.endsWith(".wav")
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 كيلوبايت"
    val kb = bytes / 1024
    val mb = kb / 1024
    return if (mb > 0) "$mb ميجابايت" else "$kb كيلوبايت"
}
