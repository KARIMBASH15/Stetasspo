package com.example.manager

import android.content.Context
import android.os.Environment
import com.example.model.DownloadStatus
import com.example.model.DownloadTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

class WebDownloadManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _downloadTasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val downloadTasks: StateFlow<List<DownloadTask>> = _downloadTasks.asStateFlow()

    private fun getDownloadDir(): File {
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val fileHubDownloads = File(publicDownloads, "FileHub")
        if (!fileHubDownloads.exists()) {
            fileHubDownloads.mkdirs()
        }
        return if (fileHubDownloads.canWrite()) fileHubDownloads else (context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir)
    }

    fun startDownload(url: String, suggestedName: String? = null) {
        val fileName = suggestedName ?: url.substringAfterLast("/").substringBefore("?").ifEmpty { "download_${System.currentTimeMillis()}" }
        val id = UUID.randomUUID().toString()
        val task = DownloadTask(
            id = id,
            url = url,
            fileName = fileName,
            status = DownloadStatus.DOWNLOADING
        )

        _downloadTasks.value = listOf(task) + _downloadTasks.value

        scope.launch {
            executeDownload(task)
        }
    }

    private suspend fun executeDownload(task: DownloadTask) = withContext(Dispatchers.IO) {
        val targetFile = File(getDownloadDir(), task.fileName)
        try {
            val request = Request.Builder()
                .url(task.url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 12; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .header("Accept", "*/*")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    updateTaskStatus(task.id, DownloadStatus.FAILED, "خطأ HTTP ${response.code}")
                    return@use
                }
                val body = response.body ?: run {
                    updateTaskStatus(task.id, DownloadStatus.FAILED, "Empty response")
                    return@use
                }
                val contentLength = body.contentLength()
                updateTaskProgress(task.id, 0L, contentLength)

                body.byteStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalDownloaded = 0L
                        var lastUpdate = System.currentTimeMillis()

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalDownloaded += bytesRead
                            val now = System.currentTimeMillis()
                            if (now - lastUpdate > 300) {
                                lastUpdate = now
                                updateTaskProgress(task.id, totalDownloaded, contentLength)
                            }
                        }
                        updateTaskProgress(task.id, totalDownloaded, contentLength)
                        updateTaskStatus(task.id, DownloadStatus.COMPLETED)
                    }
                }
            }
        } catch (e: Exception) {
            updateTaskStatus(task.id, DownloadStatus.FAILED, e.message ?: "Download failed")
        }
    }

    private fun updateTaskProgress(id: String, downloaded: Long, total: Long) {
        _downloadTasks.value = _downloadTasks.value.map {
            if (it.id == id) it.copy(downloadedBytes = downloaded, totalBytes = total) else it
        }
    }

    private fun updateTaskStatus(id: String, status: DownloadStatus, error: String? = null) {
        _downloadTasks.value = _downloadTasks.value.map {
            if (it.id == id) it.copy(status = status, error = error) else it
        }
    }

    fun removeTask(id: String) {
        _downloadTasks.value = _downloadTasks.value.filter { it.id != id }
    }
}
