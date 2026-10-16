package com.example.model

import java.io.File

enum class FileCategory(val label: String, val labelAr: String) {
    ALL("All", "الكل"),
    DOCUMENTS("Documents", "مستندات"),
    AUDIO("Audio", "صوتيات"),
    VIDEO("Video", "فيديو"),
    IMAGES("Images", "صور"),
    ARCHIVES("Archives", "أرشيف"),
    HTML("HTML & Web", "صفحات ويب"),
    APK("Apps", "تطبيقات")
}

enum class SortOption(val title: String, val titleAr: String) {
    NAME_ASC("Name (A-Z)", "الاسم (أ-ي)"),
    NAME_DESC("Name (Z-A)", "الاسم (ي-أ)"),
    DATE_DESC("Newest First", "الأحدث أولاً"),
    SIZE_DESC("Largest First", "الأكبر حجماً")
}

data class FileItem(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val isDirectory: Boolean = file.isDirectory,
    val size: Long = if (file.isDirectory) 0L else file.length(),
    val lastModified: Long = file.lastModified(),
    val extension: String = file.extension.lowercase(),
    val mimeType: String = getMimeTypeForExtension(file.extension.lowercase()),
    val isHidden: Boolean = file.isHidden || file.name.startsWith("."),
    val isVaultLocked: Boolean = false,
    val itemCount: Int = if (file.isDirectory) (file.listFiles()?.size ?: 0) else 0,
    val isSelected: Boolean = false
) {
    val category: FileCategory
        get() = when (extension) {
            "mp3", "wav", "m4a", "aac", "ogg", "flac" -> FileCategory.AUDIO
            "mp4", "mkv", "webm", "avi", "3gp", "mov" -> FileCategory.VIDEO
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "svg" -> FileCategory.IMAGES
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md" -> FileCategory.DOCUMENTS
            "zip", "rar", "7z", "tar", "gz" -> FileCategory.ARCHIVES
            "html", "htm", "css", "js", "json" -> FileCategory.HTML
            "apk" -> FileCategory.APK
            else -> FileCategory.ALL
        }

    companion object {
        fun getMimeTypeForExtension(ext: String): String {
            return when (ext) {
                "mp3" -> "audio/mpeg"
                "wav" -> "audio/wav"
                "m4a" -> "audio/mp4"
                "ogg" -> "audio/ogg"
                "flac" -> "audio/flac"
                "mp4" -> "video/mp4"
                "mkv" -> "video/x-matroska"
                "webm" -> "video/webm"
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "webp" -> "image/webp"
                "gif" -> "image/gif"
                "pdf" -> "application/pdf"
                "txt" -> "text/plain"
                "html", "htm" -> "text/html"
                "json" -> "application/json"
                "zip" -> "application/zip"
                "apk" -> "application/vnd.android.package-archive"
                else -> "*/*"
            }
        }
    }
}

data class StorageStats(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val imagesBytes: Long = 0L,
    val videoBytes: Long = 0L,
    val audioBytes: Long = 0L,
    val docsBytes: Long = 0L,
    val archivesBytes: Long = 0L,
    val otherBytes: Long = 0L
) {
    val usedPercentage: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()) else 0f
}
