package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ClickitDarkColorScheme = darkColorScheme(
    primary = ClickitPeach,
    onPrimary = ClickitMidnight,
    primaryContainer = ClickitBurgundy,
    onPrimaryContainer = ClickitPeachSoft,
    secondary = ClickitSlate,
    onSecondary = ClickitIvory,
    secondaryContainer = ClickitSlateDeep,
    onSecondaryContainer = ClickitIvory,
    tertiary = ClickitInfo,
    onTertiary = ClickitMidnight,
    tertiaryContainer = ClickitInfoContainer,
    onTertiaryContainer = ClickitInfo,
    background = ClickitMidnight,
    onBackground = ClickitIvory,
    surface = ClickitSlateDeep,
    onSurface = ClickitIvory,
    surfaceVariant = ClickitSlate,
    onSurfaceVariant = ClickitMutedText,
    outline = ClickitSubtleLine,
    outlineVariant = ClickitSlate,
    error = ClickitCrimson,
    onError = Color.White,
    errorContainer = ClickitBurgundy,
    onErrorContainer = ClickitPeachSoft
)

private val ClickitLightColorScheme = lightColorScheme(
    primary = ClickitMidnight,
    onPrimary = ClickitIvory,
    primaryContainer = ClickitPeachSoft,
    onPrimaryContainer = ClickitMidnight,
    secondary = ClickitSlateDeep,
    onSecondary = ClickitIvory,
    secondaryContainer = ClickitLightSurfaceVar,
    onSecondaryContainer = ClickitMidnight,
    tertiary = ClickitBurgundy,
    onTertiary = Color.White,
    background = ClickitLightBg,
    onBackground = ClickitMidnight,
    surface = ClickitLightSurface,
    onSurface = ClickitMidnight,
    surfaceVariant = ClickitLightSurfaceVar,
    onSurfaceVariant = ClickitSlate,
    outline = Color(0xFFCBD2E0),
    error = ClickitCrimson,
    onError = Color.White,
    errorContainer = Color(0xFFFDE8EB),
    onErrorContainer = ClickitBurgundy
)

val ClickitShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ClickitTheme(
    darkTheme: Boolean = true,
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
