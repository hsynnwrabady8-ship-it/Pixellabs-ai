package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val AmoledColorScheme = darkColorScheme(
    primary = StudioCyan,
    onPrimary = Color(0xFF04131D),
    primaryContainer = Color(0xFF0C4A6E),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = StudioViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4C1D95),
    onSecondaryContainer = Color(0xFFF3E8FF),
    tertiary = StudioRose,
    onTertiary = Color.White,
    background = Color(0xFF000000),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF06080E),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF0E131E),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF1E283A)
)

private val DarkColorScheme = darkColorScheme(
    primary = StudioCyan,
    onPrimary = Color(0xFF04131D),
    primaryContainer = Color(0xFF0C4A6E),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = StudioViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4C1D95),
    onSecondaryContainer = Color(0xFFF3E8FF),
    tertiary = StudioRose,
    onTertiary = Color.White,
    background = StudioObsidian,
    onBackground = StudioTextPrimary,
    surface = StudioSurface,
    onSurface = StudioTextPrimary,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = StudioTextSecondary,
    outline = StudioBorder
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFF7C3AED),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF5B21B6),
    tertiary = StudioRose,
    onTertiary = Color.White,
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
    themeMode: StudioThemeMode = StudioThemeMode.AMOLED,
    content: @Composable () -> Unit,
) {
    val (colorScheme, palette) = when (themeMode) {
        StudioThemeMode.AMOLED -> Pair(AmoledColorScheme, AmoledPalette)
        StudioThemeMode.DARK -> Pair(DarkColorScheme, DarkStudioPalette)
        StudioThemeMode.LIGHT -> Pair(LightColorScheme, LightStudioPalette)
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalStudioPalette provides palette
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
