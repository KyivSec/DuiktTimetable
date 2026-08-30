package com.kyivsec.duikttimetable.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.kyivsec.duikttimetable.model.ThemeMode

private val DarkColors = darkColorScheme(
    primary = BluePrimaryDark, onPrimary = Color(0xFF092E66), primaryContainer = BlueContainerDark,
    onPrimaryContainer = Color(0xFFD9E2FF), background = DarkBackground, onBackground = Color(0xFFE3E9EE),
    surface = DarkSurface, onSurface = Color(0xFFE3E9EE), surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = Color(0xFFBCC8D1), outline = DarkOutline, error = Color(0xFFFFB4AB),
)

private val LightColors = lightColorScheme(
    primary = LightPrimary, onPrimary = Color.White, primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = Color(0xFF0B2F65), background = LightBackground, onBackground = Color(0xFF17212A),
    surface = LightSurface, onSurface = Color(0xFF17212A), surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = Color(0xFF52606C), outline = Color(0xFF7A8995),
)

@Composable
fun DuiktTimetableTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, typography = Typography, content = content)
}
