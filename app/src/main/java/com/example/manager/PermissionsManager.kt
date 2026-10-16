package com.example.manager

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.model.PermissionItem

class PermissionsManager(private val context: Context) {

    fun getPermissionsList(): List<PermissionItem> {
        val list = mutableListOf<PermissionItem>()

        // 1. Audio / Music
        val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        list.add(
            PermissionItem(
                id = "audio_perm",
                name = "Audio & Music Access",
                nameAr = "إذن الوصول للصوتيات والموسيقى",
                description = "Required to scan and play MP3 and audio tracks in the background.",
                descriptionAr = "مطلوب لفحص وتشغيل ملفات MP3 والموسيقى بالخلفية.",
                isGranted = isPermissionGranted(audioPermission),
                permissionKey = audioPermission,
                isCritical = true
            )
        )

        // 2. Video Access
        val videoPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        list.add(
            PermissionItem(
                id = "video_perm",
                name = "Video Files Access",
                nameAr = "إذن الوصول لملفات الفيديو",
                description = "Allows playback of MP4 and high-definition video clips.",
                descriptionAr = "يتيح تشغيل مقاطع الفيديو والأفلام بدقة عالية.",
                isGranted = isPermissionGranted(videoPermission),
                permissionKey = videoPermission,
                isCritical = true
            )
        )

        // 3. Photos & Images Access
        val imagesPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        list.add(
            PermissionItem(
                id = "images_perm",
                name = "Photos & Images",
                nameAr = "إذن الصور والمستندات المرئية",
                description = "Required to preview thumbnails and secure photos in the vault.",
                descriptionAr = "مطلوب لمعاينة الصور وحفظها في الخزنة السرية.",
                isGranted = isPermissionGranted(imagesPermission),
                permissionKey = imagesPermission,
                isCritical = false
            )
        )

        // 4. Notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(
                PermissionItem(
                    id = "notif_perm",
                    name = "Media & Download Notifications",
                    nameAr = "إشعارات المشغل والتحميلات",
                    description = "Needed to show player controls and download progress in the status bar.",
                    descriptionAr = "مطلوب لإظهار أزرار التحكم بالمشغل وسرعة التحميل في شريط الإشعارات.",
                    isGranted = isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS),
                    permissionKey = Manifest.permission.POST_NOTIFICATIONS,
                    isCritical = true
                )
            )
        }

        // 5. All Files Management (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val hasAllFiles = Environment.isExternalStorageManager()
            list.add(
                PermissionItem(
                    id = "all_files_perm",
                    name = "All Files Access (Storage Manager)",
                    nameAr = "إدارة جميع الملفات بالذاكرة",
                    description = "Gives full access to copy, move, rename, unzip, and organize all storage folders.",
                    descriptionAr = "يمنح تحكماً شاملاً لتصفح ونسخ ونقل وفك ضغط وتنظيم ملفات الذاكرة الداخلية.",
                    isGranted = hasAllFiles,
                    permissionKey = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    isCritical = true
                )
            )
        }

        // 6. Display Over Other Apps (Floating Icon & Widget)
        list.add(
            PermissionItem(
                id = "overlay_perm",
                name = "Display Over Other Apps",
                nameAr = "الظهور فوق التطبيقات الأخرى (الأيقونة العائمة)",
                description = "Enables the floating shortcut icon and quick widget to appear over any app.",
                descriptionAr = "يتيح للأيقونة العائمة والمفكرة السريعة العمل والظهور فوق كافة التطبيقات الأخرى.",
                isGranted = hasOverlayPermission(),
                permissionKey = Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                isCritical = false
            )
        )

        return list
    }

    fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun openOverlaySettings() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openAppSettings()
        }
    }

    fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun hasMediaAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isPermissionGranted(Manifest.permission.READ_MEDIA_AUDIO) ||
            isPermissionGranted(Manifest.permission.READ_MEDIA_IMAGES) ||
            isPermissionGranted(Manifest.permission.READ_MEDIA_VIDEO) ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager())
        } else {
            isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun getRuntimePermissionsToRequest(): Array<String> {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        return permissions.toTypedArray()
    }

    private val prefs = context.getSharedPreferences("permissions_prefs", Context.MODE_PRIVATE)

    var hasCompletedFirstTimePermissions: Boolean
        get() = prefs.getBoolean("has_completed_first_time", false)
        set(value) = prefs.edit().putBoolean("has_completed_first_time", value).apply()

    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openAllFilesAccessSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e2: Exception) {
                    openAppSettings()
                }
            }
        } else {
            openAppSettings()
        }
    }
}
