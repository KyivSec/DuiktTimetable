package com.kyivsec.duikt_timetable.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.kyivsec.duikt_timetable.model.ThemeMode

private val DarkColors = darkColorScheme(
    primary = BluePrimaryDark, onPrimary = DarkBackground, primaryContainer = BlueContainerDark,
    onPrimaryContainer = Color(0xFFF2F2F2), background = DarkBackground, onBackground = Color(0xFFECECEC),
    secondary = Color(0xFFC7C7C7), onSecondary = DarkBackground, secondaryContainer = Color(0xFF3A3A3A),
    onSecondaryContainer = Color(0xFFE8E8E8), tertiary = InteractionAccentDark, onTertiary = Color(0xFF102A52),
    tertiaryContainer = Color(0xFF2B4773), onTertiaryContainer = Color(0xFFE1E9FF),
    surface = DarkSurface, onSurface = Color(0xFFECECEC), surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = Color(0xFFB4B4B4), outline = DarkOutline, outlineVariant = DarkOutlineVariant,
    error = Color(0xFFFFB4AB), onError = DarkBackground, errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6), surfaceDim = DarkBackground, surfaceBright = Color(0xFF444444),
    surfaceContainerLowest = Color(0xFF191919), surfaceContainerLow = Color(0xFF262626),
    surfaceContainer = Color(0xFF2A2A2A), surfaceContainerHigh = DarkSurfaceHigh,
    surfaceContainerHighest = Color(0xFF3A3A3A), inverseSurface = Color(0xFFECECEC),
    inverseOnSurface = DarkBackground, inversePrimary = LightPrimary,
)

private val OledColors = darkColorScheme(
    primary = BluePrimaryDark, onPrimary = DarkBackground, primaryContainer = BlueContainerDark,
    onPrimaryContainer = Color(0xFFF2F2F2), background = Color.Black, onBackground = Color(0xFFECECEC),
    surface = Color.Black, onSurface = Color(0xFFE6E6E6), surfaceVariant = Color.Black,
    secondary = Color(0xFFC7C7C7), onSecondary = DarkBackground, secondaryContainer = Color(0xFF3A3A3A),
    onSecondaryContainer = Color(0xFFE8E8E8), tertiary = InteractionAccentDark, onTertiary = Color(0xFF102A52),
    tertiaryContainer = Color(0xFF2B4773), onTertiaryContainer = Color(0xFFE1E9FF),
    onSurfaceVariant = Color(0xFFBDBDBD), outline = Color(0xFF666666), outlineVariant = Color(0xFF3D3D3D),
    surfaceTint = Color.Black, inverseSurface = Color(0xFFE6E6E6), inverseOnSurface = Color(0xFF1A1A1A),
    inversePrimary = LightPrimary, error = Color(0xFFFFB4AB), onError = DarkBackground,
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    surfaceDim = Color.Black, surfaceBright = Color(0xFF1A1A1A), surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF050505), surfaceContainer = Color(0xFF090909),
    surfaceContainerHigh = OledSurfaceHigh, surfaceContainerHighest = OledSurfaceHighest,
)

private val LightColors = lightColorScheme(
    primary = LightPrimary, onPrimary = Color.White, primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = Color(0xFF171717), background = LightBackground, onBackground = Color(0xFF171717),
    secondary = Color(0xFF4F4F4F), onSecondary = Color.White, secondaryContainer = Color(0xFFEEEEEE),
    onSecondaryContainer = Color(0xFF242424), tertiary = InteractionAccent, onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCE7FA), onTertiaryContainer = Color(0xFF102A52),
    surface = LightSurface, onSurface = Color(0xFF171717), surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = Color(0xFF646464), outline = LightOutline, outlineVariant = LightOutlineVariant,
    error = Color(0xFFB5404B), onError = Color.White, errorContainer = Color(0xFFFFDADB),
    onErrorContainer = Color(0xFF410008), surfaceDim = Color(0xFFDEDEDE), surfaceBright = Color.White,
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF7F7F7),
    surfaceContainer = Color(0xFFF3F3F3), surfaceContainerHigh = LightSurfaceHigh,
    surfaceContainerHighest = Color(0xFFE5E5E5), inverseSurface = Color(0xFF2A2A2A),
    inverseOnSurface = Color(0xFFF2F2F2), inversePrimary = BluePrimaryDark,
)

internal val HighContrastLightColors = lightColorScheme(
    primary = Color.Black, onPrimary = Color.White, primaryContainer = Color.White,
    onPrimaryContainer = Color.Black, background = Color.White, onBackground = Color.Black,
    secondary = Color.Black, onSecondary = Color.White, secondaryContainer = Color(0xFFE8E8E8),
    onSecondaryContainer = Color.Black, tertiary = InteractionAccent, onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCE7FA), onTertiaryContainer = Color(0xFF081A33),
    surface = Color.White, onSurface = Color.Black, surfaceVariant = Color.White,
    onSurfaceVariant = Color(0xFF333333), outline = Color.Black, outlineVariant = Color(0xFF737373),
    error = Color(0xFFB00020), onError = Color.White, errorContainer = Color.White,
    onErrorContainer = Color(0xFFB00020), surfaceDim = Color(0xFFE8E8E8), surfaceBright = Color.White,
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color.White,
    surfaceContainer = Color.White, surfaceContainerHigh = Color(0xFFF2F2F2),
    surfaceContainerHighest = Color(0xFFE8E8E8), inverseSurface = Color.Black,
    inverseOnSurface = Color.White, inversePrimary = Color.White,
)

internal val HighContrastDarkColors = darkColorScheme(
    primary = Color.White, onPrimary = Color.Black, primaryContainer = Color.Black,
    onPrimaryContainer = Color.White, background = Color.Black, onBackground = Color.White,
    secondary = Color.White, onSecondary = Color.Black, secondaryContainer = Color(0xFF1F1F1F),
    onSecondaryContainer = Color.White, tertiary = InteractionAccentDark, onTertiary = Color(0xFF081A33),
    tertiaryContainer = Color(0xFF102A52), onTertiaryContainer = Color.White,
    surface = Color.Black, onSurface = Color.White, surfaceVariant = Color.Black,
    onSurfaceVariant = Color(0xFFE0E0E0), outline = Color.White, outlineVariant = Color(0xFF8A8A8A),
    error = Color(0xFFFFB4AB), onError = Color.Black, errorContainer = Color.Black,
    onErrorContainer = Color(0xFFFFB4AB), surfaceDim = Color.Black, surfaceBright = Color(0xFF1F1F1F),
    surfaceContainerLowest = Color.Black, surfaceContainerLow = Color.Black,
    surfaceContainer = Color.Black, surfaceContainerHigh = Color(0xFF121212),
    surfaceContainerHighest = Color(0xFF1F1F1F), inverseSurface = Color.White,
    inverseOnSurface = Color.Black, inversePrimary = Color.Black,
)

@Composable
fun DuiktTimetableTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val colors = when (themeMode) {
        ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) DarkColors else LightColors
        ThemeMode.LIGHT -> LightColors
        ThemeMode.DARK -> DarkColors
        ThemeMode.HIGH_CONTRAST_LIGHT -> HighContrastLightColors
        ThemeMode.HIGH_CONTRAST_DARK -> HighContrastDarkColors
        ThemeMode.OLED -> OledColors
    }
    MaterialTheme(colorScheme = colors, typography = Typography, content = content)
}
