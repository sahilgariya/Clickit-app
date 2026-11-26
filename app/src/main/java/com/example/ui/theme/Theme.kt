package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ClickitLightColorScheme = lightColorScheme(
    primary = ClickitBrandPrimary,
    onPrimary = Color.White,
    primaryContainer = ClickitBrandTint,
    onPrimaryContainer = ClickitBrandDark,
    secondary = ClickitTextPrimary,
    onSecondary = Color.White,
    secondaryContainer = ClickitLightSurfaceVar,
    onSecondaryContainer = ClickitTextPrimary,
    tertiary = ClickitBrandDark,
    onTertiary = Color.White,
    tertiaryContainer = ClickitInfoContainer,
    onTertiaryContainer = ClickitBrandDark,
    background = ClickitLightBg,
    onBackground = ClickitTextPrimary,
    surface = ClickitLightSurface,
    onSurface = ClickitTextPrimary,
    surfaceVariant = ClickitLightSurfaceVar,
    onSurfaceVariant = ClickitTextSecondary,
    outline = ClickitBorder,
    outlineVariant = ClickitBorderStrong,
    error = ClickitCrimson,
    onError = Color.White,
    errorContainer = ClickitDangerContainer,
    onErrorContainer = Color(0xFF991B1B)
)

private val ClickitDarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF334155),
    onSecondary = Color(0xFFF8FAFC),
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFFF8FAFC),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    error = Color(0xFFEF4444),
    onError = Color.White,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

val ClickitShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

@Composable
fun ClickitTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ClickitDarkColorScheme else ClickitLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = ClickitTypography,
        shapes = ClickitShapes,
        content = content
    )
}
