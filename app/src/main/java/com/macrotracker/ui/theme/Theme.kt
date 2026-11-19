package com.macrotracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object MacroColors {
    val Background = Color(0xFF121316)
    val CardSurface = Color(0xFF1C1E24)
    val CardSurfaceElevated = Color(0xFF252830)
    val BorderSubtle = Color(0xFF2E323D)

    val ProteinTeal = Color(0xFF2DD4BF)
    val CarbsAmber = Color(0xFFFBBF24)
    val FatCoral = Color(0xFFFB7185)
    val WaterSkyBlue = Color(0xFF38BDF8)

    val SuccessGreen = Color(0xFF34D399)
    val WarningAmber = Color(0xFFF59E0B)
    val DangerRed = Color(0xFFEF4444)

    val TextPrimary = Color(0xFFF3F4F6)
    val TextSecondary = Color(0xFF9CA3AF)
}

private val DarkColorScheme = darkColorScheme(
    primary = MacroColors.ProteinTeal,
    onPrimary = Color(0xFF042F2E),
    secondary = MacroColors.WaterSkyBlue,
    onSecondary = Color(0xFF082F49),
    tertiary = MacroColors.CarbsAmber,
    onTertiary = Color(0xFF451A03),
    background = MacroColors.Background,
    onBackground = MacroColors.TextPrimary,
    surface = MacroColors.CardSurface,
    onSurface = MacroColors.TextPrimary,
    surfaceVariant = MacroColors.CardSurfaceElevated,
    onSurfaceVariant = MacroColors.TextSecondary,
    outline = MacroColors.BorderSubtle,
    error = MacroColors.DangerRed,
    onError = Color.White
)

private val AppTypography = Typography(
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

@Composable
fun MacroTrackerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
