package com.dnsly.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Google-Inspired Pixel Light Theme — Airy, Flat, Spacious
private val GoogleLightScheme = lightColorScheme(
    primary = Color(0xFF1A73E8),              // Google Blue
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E3FD),      // Soft Blue Tint
    onPrimaryContainer = Color(0xFF041E49),

    secondary = Color(0xFF5F6368),             // Google Grey 700
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8EAED),    // Google Grey 200
    onSecondaryContainer = Color(0xFF202124),

    tertiary = Color(0xFF1E8E3E),              // Google Green
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCEEAD6),     // Soft Green Tint
    onTertiaryContainer = Color(0xFF0D652D),

    background = Color(0xFFFAFBFD),            // Near-white with barely perceptible warmth
    onBackground = Color(0xFF202124),          // Google Grey 900

    surface = Color(0xFFFFFFFF),               // Pure White
    onSurface = Color(0xFF202124),
    surfaceVariant = Color(0xFFF1F3F4),        // Google Grey 100
    onSurfaceVariant = Color(0xFF5F6368),      // Google Grey 700

    surfaceContainer = Color(0xFFF8F9FA),      // Google Grey 50
    surfaceContainerLow = Color(0xFFFCFCFD),
    surfaceContainerHigh = Color(0xFFF1F3F4),  // Google Grey 100
    surfaceContainerHighest = Color(0xFFE8EAED), // Google Grey 200

    outline = Color(0xFFDADCE0),               // Google Grey 300
    outlineVariant = Color(0xFFF1F3F4),        // Very subtle

    error = Color(0xFFD93025),                 // Google Red
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFCE8E6),        // Soft Red Tint
    onErrorContainer = Color(0xFF5F2120),

    surfaceTint = Color.Transparent            // Prevent tonal elevation lift
)

private val GoogleShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun DNSlyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GoogleLightScheme,
        typography = Typography,
        shapes = GoogleShapes,
        content = content
    )
}
