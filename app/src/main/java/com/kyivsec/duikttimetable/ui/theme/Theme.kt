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
    onSurfaceVariant = Color(0xFFBCC8D1), outline = DarkOutline, outlineVariant = Color(0xFF35424C),
    surfaceContainerHigh = DarkSurfaceHigh,
    error = Color(0xFFFFB4AB),
)

private val OledColors = darkColorScheme(
    primary = BluePrimaryDark, onPrimary = Color(0xFF092E66), primaryContainer = BlueContainerDark,
    onPrimaryContainer = Color(0xFFD9E2FF), background = Color.Black, onBackground = Color(0xFFE6E6E6),
    surface = Color.Black, onSurface = Color(0xFFE6E6E6), surfaceVariant = Color.Black,
    onSurfaceVariant = Color(0xFFBDBDBD), outline = Color(0xFF666666), outlineVariant = Color(0xFF2A2A2A),
    surfaceTint = Color.Black, inverseSurface = Color(0xFFE6E6E6), inverseOnSurface = Color(0xFF1A1A1A),
    error = Color(0xFFFFB4AB),
    surfaceDim = Color.Black, surfaceBright = OledSurfaceHighest, surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color.Black, surfaceContainer = Color.Black, surfaceContainerHigh = OledSurfaceHigh,
    surfaceContainerHighest = OledSurfaceHighest,
)

private val LightColors = lightColorScheme(
    primary = LightPrimary, onPrimary = Color.White, primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = Color(0xFF0B2F65), background = LightBackground, onBackground = Color(0xFF17212A),
    surface = LightSurface, onSurface = Color(0xFF17212A), surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = Color(0xFF52606C), outline = Color(0xFF7A8995), outlineVariant = Color(0xFFC2CBD3),
    surfaceContainerHigh = LightSurfaceHigh,
)

@Composable
fun DuiktTimetableTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val colors = when (themeMode) {
        ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) DarkColors else LightColors
        ThemeMode.LIGHT -> LightColors
        ThemeMode.DARK -> DarkColors
        ThemeMode.OLED -> OledColors
    }
    MaterialTheme(colorScheme = colors, typography = Typography, content = content)
}
