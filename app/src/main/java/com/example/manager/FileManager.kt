package com.example.manager

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.model.SortOption
import com.example.model.StorageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class StorageShortcut(
    val id: String,
    val name: String,
    val nameAr: String,
    val directory: File,
    val iconType: String
)

class FileManager(private val context: Context) {

    var clipboardFiles: List<File> = emptyList()
        private set
    var isCutMode: Boolean = false
        private set

    fun getRootDirectory(): File {
        val extStorage = Environment.getExternalStorageDirectory()
        // If external primary storage exists and can be read, use it as root
        return if (extStorage.exists() && extStorage.canRead()) {
            extStorage
        } else {
            getAppStorageDirectory()
        }
    }

    fun getAppStorageDirectory(): File {
        val appFolder = File(Environment.getExternalStorageDirectory(), "FileHub")
        if (!appFolder.exists()) {
            try {
                appFolder.mkdirs()
            } catch (_: Exception) {}
        }
        val external = context.getExternalFilesDir(null)
        val filesDir = context.filesDir
        return if (appFolder.exists() && appFolder.canWrite()) appFolder else (external ?: filesDir)
    }

    fun getStorageShortcuts(): List<StorageShortcut> {
        val list = mutableListOf<StorageShortcut>()
        val root = Environment.getExternalStorageDirectory()
        list.add(StorageShortcut("storage_root", "Internal Storage", "الذاكرة الداخلية", root, "storage"))

        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloads != null) {
            list.add(StorageShortcut("downloads", "Downloads", "التنزيلات", downloads, "downloads"))
        }

        val dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        if (dcim != null) {
            list.add(StorageShortcut("camera", "Camera / Photos", "الصور والكاميرا", dcim, "camera"))
        }

        val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (docs != null) {
            list.add(StorageShortcut("docs", "Documents", "المستندات", docs, "documents"))
        }

        val music = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        if (music != null) {
            list.add(StorageShortcut("music", "Music", "الموسيقى", music, "music"))
        }

        val movies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        if (movies != null) {
            list.add(StorageShortcut("movies", "Videos", "الفيديوهات", movies, "videos"))
        }

        val appDir = getAppStorageDirectory()
        list.add(StorageShortcut("app_dir", "FileHub App Folder", "مجلد FileHub", appDir, "app"))

        return list
    }

    suspend fun listDirectory(
        directory: File,
        category: FileCategory = FileCategory.ALL,
        query: String = "",
        sortOption: SortOption = SortOption.NAME_ASC
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val files = if (category == FileCategory.ALL) {
            directory.listFiles()?.toList() ?: emptyList()
        } else {
            // When filtering by a specific category, scan the directory and its subdirectories
            scanDirectoryForCategory(directory, category)
        }

        var items = files.map { FileItem(it) }

        if (query.isNotBlank()) {
            items = items.filter { it.name.contains(query, ignoreCase = true) }
        }

        items = when (sortOption) {
            SortOption.NAME_ASC -> items.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            SortOption.NAME_DESC -> items.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.name.lowercase() })
            SortOption.DATE_DESC -> items.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.lastModified })
            SortOption.SIZE_DESC -> items.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.size })
        }

        items
    }

    private fun scanDirectoryForCategory(dir: File, category: FileCategory): List<File> {
        val result = mutableListOf<File>()
        dir.walkTopDown().maxDepth(4).forEach { file ->
            if (file.isFile) {
                val item = FileItem(file)
                if (item.category == category) {
                    result.add(file)
                }
            }
        }
        return result
    }

    fun setClipboard(files: List<File>, cutMode: Boolean) {
        clipboardFiles = files
        isCutMode = cutMode
    }

    fun clearClipboard() {
        clipboardFiles = emptyList()
        isCutMode = false
    }

    suspend fun pasteClipboard(targetDir: File): Boolean = withContext(Dispatchers.IO) {
        try {
            for (source in clipboardFiles) {
                val dest = File(targetDir, source.name)
                if (source.isDirectory) {
                    source.copyRecursively(dest, overwrite = true)
                } else {
                    source.copyTo(dest, overwrite = true)
                }
                if (isCutMode) {
                    source.deleteRecursively()
                }
            }
            if (isCutMode) {
                clearClipboard()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteFiles(files: List<File>): Boolean = withContext(Dispatchers.IO) {
        try {
            files.forEach { it.deleteRecursively() }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun renameFile(file: File, newName: String): File? = withContext(Dispatchers.IO) {
        try {
            val dest = File(file.parentFile, newName)
            if (file.renameTo(dest)) dest else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun createDirectory(parent: File, name: String): File? = withContext(Dispatchers.IO) {
        try {
            val dir = File(parent, name)
            if (dir.mkdirs() || dir.exists()) dir else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun createTextFile(parent: File, name: String, content: String): File? = withContext(Dispatchers.IO) {
        try {
            val file = File(parent, name)
            file.writeText(content)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun zipFiles(files: List<File>, targetZipFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(targetZipFile))).use { zos ->
                files.forEach { file ->
                    addFileToZip(file, "", zos)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun addFileToZip(file: File, parentPath: String, zos: ZipOutputStream) {
        val entryName = if (parentPath.isEmpty()) file.name else "$parentPath/${file.name}"
        if (file.isDirectory) {
            val dirEntry = ZipEntry("$entryName/")
            zos.putNextEntry(dirEntry)
            zos.closeEntry()
            file.listFiles()?.forEach { child ->
                addFileToZip(child, entryName, zos)
            }
        } else {
            val entry = ZipEntry(entryName)
            zos.putNextEntry(entry)
            FileInputStream(file).use { fis ->
                fis.copyTo(zos)
            }
            zos.closeEntry()
        }
    }

    suspend fun unzipFile(zipFile: File, targetDir: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val newFile = File(targetDir, entry.name)
                    // Protect against Zip Slip vulnerability
                    if (!newFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                        entry = zis.nextEntry
                        continue
                    }
                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun calculateMd5(file: File): String = withContext(Dispatchers.IO) {
        try {
            if (file.isDirectory) return@withContext "Directory"
            val digest = MessageDigest.getInstance("MD5")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "Error calculating MD5"
        }
    }

    suspend fun getStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        val storagePath = try {
            Environment.getExternalStorageDirectory().path
        } catch (_: Exception) {
            context.filesDir.path
        }
        val stat = try {
            StatFs(storagePath)
        } catch (_: Exception) {
            StatFs(context.filesDir.path)
        }

        val totalSpace = stat.totalBytes.coerceAtLeast(1024L * 1024L * 1024L * 8L)
        val freeSpace = stat.availableBytes.coerceAtLeast(0L)
        val realUsed = (totalSpace - freeSpace).coerceAtLeast(0L)

        // Query MediaStore for fast, accurate media size sums across device storage
        var img = queryMediaStoreCategoryBytes(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        var vid = queryMediaStoreCategoryBytes(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        var aud = queryMediaStoreCategoryBytes(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        var doc = 0L
        var arch = 0L
        var oth = 0L

        // Scan quick files in root directory
        try {
            val root = getRootDirectory()
            root.listFiles()?.forEach { file ->
                if (file.isFile) {
                    val len = file.length()
                    when (FileItem(file).category) {
                        FileCategory.DOCUMENTS -> doc += len
                        FileCategory.ARCHIVES -> arch += len
                        FileCategory.IMAGES -> if (img == 0L) img += len
                        FileCategory.VIDEO -> if (vid == 0L) vid += len
                        FileCategory.AUDIO -> if (aud == 0L) aud += len
                        else -> oth += len
                    }
                }
            }
        } catch (_: Exception) {}

        val categorizedSum = img + vid + aud + doc + arch + oth
        if (realUsed > categorizedSum) {
            oth = (realUsed - (img + vid + aud + doc + arch)).coerceAtLeast(0L)
        }

        StorageStats(
            totalBytes = totalSpace,
            usedBytes = realUsed,
            freeBytes = freeSpace,
            imagesBytes = img,
            videoBytes = vid,
            audioBytes = aud,
            docsBytes = doc,
            archivesBytes = arch,
            otherBytes = oth
        )
    }

    private fun queryMediaStoreCategoryBytes(uri: android.net.Uri): Long {
        var total = 0L
        try {
            val projection = arrayOf(MediaStore.MediaColumns.SIZE)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                if (sizeIndex != -1) {
                    while (cursor.moveToNext()) {
                        val size = cursor.getLong(sizeIndex)
                        if (size > 0) total += size
                    }
                }
            }
        } catch (_: Exception) {}
        return total
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val formatted = String.format(Locale.US, "%.1f", bytes / Math.pow(1024.0, digitGroups.toDouble()))
        val unitsAr = arrayOf("بايت", "ك.ب", "م.ب", "ج.ب", "ت.ب")
        return "$formatted ${units[digitGroups]} (${unitsAr[digitGroups]})"
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    suspend fun initializeSampleFiles() = withContext(Dispatchers.IO) {
        val root = getRootDirectory()
        val docsDir = File(root, "Documents").apply { mkdirs() }
        val musicDir = File(root, "Music").apply { mkdirs() }
        val videosDir = File(root, "Videos").apply { mkdirs() }
        val webDir = File(root, "WebProjects").apply { mkdirs() }

        // 1. Sample HTML File
        val htmlFile = File(webDir, "index.html")
        if (!htmlFile.exists()) {
            htmlFile.writeText(
                """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FileHub Pro - Live HTML Preview</title>
    <style>
        body {
            font-family: system-ui, -apple-system, sans-serif;
            background: linear-gradient(135deg, #0f172a, #1e293b);
            color: #f8fafc;
            padding: 24px;
            margin: 0;
        }
        .card {
            background: rgba(255, 255, 255, 0.05);
            backdrop-filter: blur(10px);
            border: 1px solid rgba(255, 255, 255, 0.1);
            border-radius: 16px;
            padding: 24px;
            max-width: 600px;
            margin: 20px auto;
            box-shadow: 0 10px 25px rgba(0,0,0,0.3);
        }
        h1 { color: #38bdf8; margin-top: 0; }
        p { color: #94a3b8; line-height: 1.6; }
        .badge {
            display: inline-block;
            background: #0284c7;
            color: #fff;
            padding: 6px 14px;
            border-radius: 9999px;
            font-size: 13px;
            font-weight: 600;
        }
        button {
            background: #38bdf8;
            color: #0f172a;
            border: none;
            padding: 12px 20px;
            border-radius: 10px;
            font-weight: bold;
            cursor: pointer;
            margin-top: 16px;
            transition: transform 0.2s;
        }
        button:hover { transform: scale(1.05); }
    </style>
</head>
<body>
    <div class="card">
        <span class="badge">Live HTML Engine</span>
        <h1>مرحباً بك في FileHub Pro</h1>
        <p>يدعم هذا التطبيق استعراض وتشغيل ملفات HTML و CSS و JavaScript محلياً بالكامل وعبر المتصفح المدمج!</p>
        <button onclick="testFunction()">انقر لاختبار التفاعل (Click me)</button>
        <div id="output" style="margin-top: 15px; font-weight: bold; color: #4ade80;"></div>
    </div>
    <script>
        function testFunction() {
            document.getElementById('output').innerText = 'تم تشغيل كود JavaScript بنجاح في متصفح FileHub Pro! 🚀';
        }
    </script>
</body>
</html>"""
            )
        }

        // 2. Sample Audio WAV File (Synthesized 440Hz Sine Wave chime for testing player!)
        val sampleAudio = File(musicDir, "Cyber_Tone_Sample.wav")
        if (!sampleAudio.exists()) {
            writeWavFile(sampleAudio, durationSeconds = 6, sampleRate = 44100, frequency = 440.0)
        }

        // 3. Sample Text file
        val textFile = File(docsDir, "Welcome_Note.txt")
        if (!textFile.exists()) {
            textFile.writeText(
                """FileHub Pro - ميزات التطبيق الشاملة:
- إدارة متكاملة للملفات مع النسخ، القص، اللصق، الحذف، التحديد، وإعادة التسمية.
- فك الضغط (Unzip) وضغط الملفات إلى ZIP بكل سهولة.
- مشغل موسيقى MP3 وفيديو متطور مع دعم التشغيل في الخلفية حتى مع إغلاق الشاشة.
- متصفح ويب مدمج مع دعم يوتيوب وتشغيل وتنزيل الميديا وصفحات HTML.
- خزنة آمنة مشفرة لحماية الصور والمستندات بكلمة مرور PIN.
- مركز متقدم لإدارة وتفقد أذونات التخزين والنظام ومراقبة سعة الذاكرة السحابية والمحلية."""
            )
        }

        // 4. Sample Video
        val videoDir = File(videosDir, "Demo_Clip.mp4")
        if (videoDir.exists() && videoDir.length() < 1024) {
            videoDir.delete() // Remove corrupt placeholder
        }
    }

    private fun writeWavFile(file: File, durationSeconds: Int, sampleRate: Int, frequency: Double) {
        val totalAudioLen = (durationSeconds * sampleRate * 2).toLong()
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = (16 * sampleRate * channels / 8).toLong()

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
        header[20] = 1; header[21] = 0
        header[22] = channels.toByte(); header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (2 * 1).toByte(); header[33] = 0
        header[34] = 16; header[35] = 0
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        FileOutputStream(file).use { out ->
            out.write(header)
            val numSamples = durationSeconds * sampleRate
            for (i in 0 until numSamples) {
                val time = i.toDouble() / sampleRate
                val angle = 2.0 * Math.PI * frequency * time
                val sample = (Math.sin(angle) * 32767).toInt().toShort()
                out.write(sample.toInt() and 0xff)
                out.write((sample.toInt() shr 8) and 0xff)
            }
        }
    }
}
