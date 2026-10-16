package com.example.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppTheme(val titleEn: String, val titleAr: String, val primaryColor: Color, val previewBg: Color) {
    CYBER_CYAN(
        "Cyber Cyan",
        "السيان السيبراني",
        Color(0xFF38BDF8),
        Color(0xFF0A0F1D)
    ),
    AMOLED_BLACK(
        "AMOLED Dark",
        "أسود أموليد نقي",
        Color(0xFF10B981),
        Color(0xFF000000)
    ),
    ROYAL_PURPLE(
        "Royal Violet",
        "البنفسجي الملكي",
        Color(0xFFA78BFA),
        Color(0xFF130E26)
    ),
    SUNSET_ROSE(
        "Sunset Rose",
        "الغروب الوردي",
        Color(0xFFF43F5E),
        Color(0xFF1E1019)
    ),
    EMERALD_MINT(
        "Emerald Forest",
        "الزمردي الطبيعي",
        Color(0xFF34D399),
        Color(0xFF06231A)
    ),
    ELEGANT_LIGHT(
        "Crystal Light",
        "الأبيض النقي",
        Color(0xFF0284C7),
        Color(0xFFF8FAFC)
    )
}

object ThemeManager {
    private const val PREFS_NAME = "filehub_theme_prefs"
    private const val KEY_THEME = "selected_theme"

    private val _currentTheme = MutableStateFlow(AppTheme.CYBER_CYAN)
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedName = prefs.getString(KEY_THEME, AppTheme.CYBER_CYAN.name)
        val found = AppTheme.values().firstOrNull { it.name == savedName } ?: AppTheme.CYBER_CYAN
        _currentTheme.value = found
    }

    fun setTheme(theme: AppTheme, context: Context) {
        _currentTheme.value = theme
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, theme.name).apply()
    }

    fun getColorScheme(theme: AppTheme): ColorScheme {
        return when (theme) {
            AppTheme.CYBER_CYAN -> darkColorScheme(
                primary = Color(0xFF38BDF8),
                onPrimary = Color(0xFF0F172A),
                primaryContainer = Color(0xFF0369A1),
                onPrimaryContainer = Color(0xFFE0F2FE),
                secondary = Color(0xFF818CF8),
                background = Color(0xFF0A0F1D),
                surface = Color(0xFF131B2E),
                surfaceVariant = Color(0xFF1E293B),
                onBackground = Color(0xFFF8FAFC),
                onSurface = Color(0xFFF8FAFC),
                onSurfaceVariant = Color(0xFF94A3B8),
                outline = Color(0xFF334155)
            )
            AppTheme.AMOLED_BLACK -> darkColorScheme(
                primary = Color(0xFF10B981),
                onPrimary = Color(0xFF000000),
                primaryContainer = Color(0xFF064E3B),
                onPrimaryContainer = Color(0xFFA7F3D0),
                secondary = Color(0xFF34D399),
                background = Color(0xFF000000),
                surface = Color(0xFF0A0A0A),
                surfaceVariant = Color(0xFF141414),
                onBackground = Color(0xFFFFFFFF),
                onSurface = Color(0xFFFFFFFF),
                onSurfaceVariant = Color(0xFFA3A3A3),
                outline = Color(0xFF262626)
            )
            AppTheme.ROYAL_PURPLE -> darkColorScheme(
                primary = Color(0xFFA78BFA),
                onPrimary = Color(0xFF1E1035),
                primaryContainer = Color(0xFF5B21B6),
                onPrimaryContainer = Color(0xFFEDE9FE),
                secondary = Color(0xFFC084FC),
                background = Color(0xFF130E26),
                surface = Color(0xFF1C1438),
                surfaceVariant = Color(0xFF2B2050),
                onBackground = Color(0xFFF5F3FF),
                onSurface = Color(0xFFF5F3FF),
                onSurfaceVariant = Color(0xFFC4B5FD),
                outline = Color(0xFF4C1D95)
            )
            AppTheme.SUNSET_ROSE -> darkColorScheme(
                primary = Color(0xFFF43F5E),
                onPrimary = Color(0xFF2D0711),
                primaryContainer = Color(0xFF9F1239),
                onPrimaryContainer = Color(0xFFFFE4E6),
                secondary = Color(0xFFFB923C),
                background = Color(0xFF1E1019),
                surface = Color(0xFF2A1522),
                surfaceVariant = Color(0xFF3B1E30),
                onBackground = Color(0xFFFFF1F2),
                onSurface = Color(0xFFFFF1F2),
                onSurfaceVariant = Color(0xFFFDA4AF),
                outline = Color(0xFF881337)
            )
            AppTheme.EMERALD_MINT -> darkColorScheme(
                primary = Color(0xFF34D399),
                onPrimary = Color(0xFF022C22),
                primaryContainer = Color(0xFF065F46),
                onPrimaryContainer = Color(0xFFD1FAE5),
                secondary = Color(0xFF2DD4BF),
                background = Color(0xFF06231A),
                surface = Color(0xFF0C3125),
                surfaceVariant = Color(0xFF144736),
                onBackground = Color(0xFFECFDF5),
                onSurface = Color(0xFFECFDF5),
                onSurfaceVariant = Color(0xFFA7F3D0),
                outline = Color(0xFF047857)
            )
            AppTheme.ELEGANT_LIGHT -> lightColorScheme(
                primary = Color(0xFF0284C7),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFE0F2FE),
                onPrimaryContainer = Color(0xFF0369A1),
                secondary = Color(0xFF6366F1),
                background = Color(0xFFF8FAFC),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFF1F5F9),
                onBackground = Color(0xFF0F172A),
                onSurface = Color(0xFF0F172A),
                onSurfaceVariant = Color(0xFF64748B),
                outline = Color(0xFFCBD5E1)
            )
        }
    }
}
