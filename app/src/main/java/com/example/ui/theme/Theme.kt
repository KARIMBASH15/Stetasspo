package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun MyApplicationTheme(
    selectedTheme: AppTheme? = null,
    content: @Composable () -> Unit
) {
    val currentThemeState by ThemeManager.currentTheme.collectAsState()
    val themeToUse = selectedTheme ?: currentThemeState
    val colorScheme = ThemeManager.getColorScheme(themeToUse)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
