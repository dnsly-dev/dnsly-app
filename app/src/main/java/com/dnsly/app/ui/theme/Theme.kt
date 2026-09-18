package com.dnsly.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Google Pixel Material 3 Expressive Light Palette (Android 15)
private val PixelExpressiveLightScheme = lightColorScheme(
    primary = Color(0xFF00639B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC2E7FF),
    onPrimaryContainer = Color(0xFF001D33),

    secondary = Color(0xFF51606F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD4E4F6),
    onSecondaryContainer = Color(0xFF0D1D2A),

    tertiary = Color(0xFF006C4C),             // Active Shield Green
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC4EED0),    // Mint Active Container
    onTertiaryContainer = Color(0xFF002114),

    background = Color(0xFFF0F4F9),          // Pixel Soft Canvas
    onBackground = Color(0xFF191C1E),

    surface = Color(0xFFFFFFFF),             // Expressive Card White
    onSurface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFFDEE3EA),
    onSurfaceVariant = Color(0xFF41474D),

    surfaceContainer = Color(0xFFE9EEF5),
    surfaceContainerLow = Color(0xFFF4F7FC),
    surfaceContainerHigh = Color(0xFFDFE6F0),
    surfaceContainerHighest = Color(0xFFD3DCE8),

    outline = Color(0xFFC1C7CE),
    outlineVariant = Color(0xFFE0E5EC),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val DNSlyShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DNSlyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PixelExpressiveLightScheme,
        typography = Typography,
        shapes = DNSlyShapes,
        content = content
    )
}
