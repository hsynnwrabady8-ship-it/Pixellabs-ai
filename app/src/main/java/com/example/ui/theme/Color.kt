package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class StudioThemeMode(val labelFa: String) {
    AMOLED("امولد مشکی"),
    DARK("دارک استودیو"),
    LIGHT("لایت مدرن")
}

val StudioCyan = Color(0xFF38BDF8)
val StudioCyanDark = Color(0xFF0284C7)
val StudioViolet = Color(0xFFA855F7)
val StudioRose = Color(0xFFF43F5E)
val StudioAmber = Color(0xFFFBBF24)
val StudioEmerald = Color(0xFF34D399)

val StudioObsidian = Color(0xFF0B0E14)
val StudioSurface = Color(0xFF131822)
val StudioSurfaceElevated = Color(0xFF1C2331)
val StudioCard = Color(0xFF232C3D)

val StudioTextPrimary = Color(0xFFF8FAFC)
val StudioTextSecondary = Color(0xFF94A3B8)
val StudioBorder = Color(0xFF2E3A4F)

@Immutable
data class StudioPalette(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val card: Color,
    val glass: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val primaryAccent: Color = StudioCyan,
    val secondaryAccent: Color = StudioViolet,
    val success: Color = StudioEmerald,
    val warning: Color = StudioAmber,
    val danger: Color = StudioRose
) {
    val surfaceVariant: Color get() = surfaceElevated
}

val AmoledPalette = StudioPalette(
    background = Color(0xFF000000),
    surface = Color(0xFF06080E),
    surfaceElevated = Color(0xFF0E131E),
    card = Color(0xFF111724),
    glass = Color(0xEE05070C),
    border = Color(0xFF1E283A),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8)
)

val DarkStudioPalette = StudioPalette(
    background = Color(0xFF0B0E14),
    surface = Color(0xFF131822),
    surfaceElevated = Color(0xFF1C2331),
    card = Color(0xFF232C3D),
    glass = Color(0xEE131822),
    border = Color(0xFF2E3A4F),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8)
)

val LightStudioPalette = StudioPalette(
    background = Color(0xFFF1F5F9),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFE2E8F0),
    card = Color(0xFFFFFFFF),
    glass = Color(0xF2FFFFFF),
    border = Color(0xFFCBD5E1),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF64748B),
    primaryAccent = Color(0xFF0284C7),
    secondaryAccent = Color(0xFF7C3AED)
)

val LocalStudioPalette = staticCompositionLocalOf { AmoledPalette }
