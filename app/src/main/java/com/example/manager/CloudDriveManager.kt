package com.example.manager

import android.content.Context
import com.example.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

data class CloudFile(
    val id: String,
    val name: String,
    val size: Long,
    val modifiedDate: Long,
    val isFolder: Boolean = false,
    val categoryName: String = "Documents"
)

data class CloudSyncState(
    val isSyncing: Boolean = false,
    val lastSyncTime: Long = System.currentTimeMillis() - 3600000,
    val totalQuotaBytes: Long = 15L * 1024L * 1024L * 1024L, // 15 GB
    val usedQuotaBytes: Long = 3L * 1024L * 1024L * 1024L + 420L * 1024L * 1024L, // ~3.4 GB
    val syncedItemsCount: Int = 18
) {
    val freeQuotaBytes: Long get() = (totalQuotaBytes - usedQuotaBytes).coerceAtLeast(0L)
    val usedPercent: Float get() = (usedQuotaBytes.toFloat() / totalQuotaBytes.toFloat()).coerceIn(0f, 1f)
}

class CloudDriveManager(private val context: Context) {

    private val cloudLocalDir = File(context.filesDir, "CloudDrive").apply { mkdirs() }

    private val _syncState = MutableStateFlow(CloudSyncState())
    val syncState: StateFlow<CloudSyncState> = _syncState.asStateFlow()

    private val _cloudFiles = MutableStateFlow<List<CloudFile>>(emptyList())
    val cloudFiles: StateFlow<List<CloudFile>> = _cloudFiles.asStateFlow()

    init {
        initializeCloudDrive()
    }

    private fun initializeCloudDrive() {
        val sampleList = listOf(
            CloudFile("c1", "Cloud_Backup_2026.zip", 450 * 1024 * 1024L, System.currentTimeMillis() - 86400000 * 2, false, "Archives"),
            CloudFile("c2", "Work_Presentation_Slides.pdf", 18 * 1024 * 1024L, System.currentTimeMillis() - 86400000 * 4, false, "Documents"),
            CloudFile("c3", "Nature_4K_Relaxing_Audio.mp3", 32 * 1024 * 1024L, System.currentTimeMillis() - 86400000 * 1, false, "Audio"),
            CloudFile("c4", "FileHub_Config_Backup.json", 24 * 1024L, System.currentTimeMillis() - 3600000 * 5, false, "Documents"),
            CloudFile("c5", "Family_Vacation_Album", 0L, System.currentTimeMillis() - 86400000 * 10, true, "Folders")
        )
        _cloudFiles.value = sampleList
    }

    suspend fun syncNow(): Boolean = withContext(Dispatchers.IO) {
        _syncState.value = _syncState.value.copy(isSyncing = true)
        delay(1500) // Simulate cloud handshake & checksum verification
        _syncState.value = _syncState.value.copy(
            isSyncing = false,
            lastSyncTime = System.currentTimeMillis(),
            syncedItemsCount = _cloudFiles.value.size
        )
        true
    }

    suspend fun uploadFileToCloud(file: File): Boolean = withContext(Dispatchers.IO) {
        _syncState.value = _syncState.value.copy(isSyncing = true)
        delay(1200)
        val cloudFile = CloudFile(
            id = "c_${System.currentTimeMillis()}",
            name = file.name,
            size = file.length(),
            modifiedDate = System.currentTimeMillis(),
            isFolder = file.isDirectory,
            categoryName = FileItem(file).category.label
        )
        val updated = _cloudFiles.value.toMutableList().apply { add(0, cloudFile) }
        _cloudFiles.value = updated
        _syncState.value = _syncState.value.copy(
            isSyncing = false,
            usedQuotaBytes = _syncState.value.usedQuotaBytes + file.length(),
            syncedItemsCount = updated.size,
            lastSyncTime = System.currentTimeMillis()
        )
        true
    }

    suspend fun deleteCloudFile(cloudFile: CloudFile): Boolean = withContext(Dispatchers.IO) {
        val updated = _cloudFiles.value.filter { it.id != cloudFile.id }
        _cloudFiles.value = updated
        _syncState.value = _syncState.value.copy(
            usedQuotaBytes = (_syncState.value.usedQuotaBytes - cloudFile.size).coerceAtLeast(0L),
            syncedItemsCount = updated.size
        )
        true
    }
}
