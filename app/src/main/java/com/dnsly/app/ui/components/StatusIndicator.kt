package com.dnsly.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

private data class Bubble(
    val relX: Float,
    val speed: Float,
    val size: Float,
    val seed: Float
)

@Composable
fun PowerButton(
    isConnected: Boolean,
    isConnecting: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Tactile press spring
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f),
        label = "press_scale"
    )

    // Dynamic water level: 16% when disconnected, 50% when connecting, 72% when connected
    val targetFillLevel = when {
        isConnecting -> 0.50f
        isConnected -> 0.72f
        else -> 0.16f
    }

    val fillLevel by animateFloatAsState(
        targetValue = targetFillLevel,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 120f),
        label = "fluid_fill_level"
    )

    // Continuous infinite transitions for wave and bubble motion
    val infiniteTransition = rememberInfiniteTransition(label = "fluid_waves")

    // Foreground wave phase (0 to 2*PI)
    val wavePhase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (isConnecting) 1200 else 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase_1"
    )

    // Background wave phase
    val wavePhase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (isConnecting) 1600 else 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase_2"
    )

    // Bubble vertical rise cycle (0 to 1)
    val bubbleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isConnecting) 1000 else 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubble_progress"
    )

    // Concentric atmospheric ripple scale
    val rippleScale1 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isConnected || isConnecting) 1.28f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_scale_1"
    )
    val rippleAlpha1 by infiniteTransition.animateFloat(
        initialValue = if (isConnected || isConnecting) 0.22f else 0.0f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_alpha_1"
    )

    // Exact Theme Colors matching Google Material 3 Design
    val googleGreen = MaterialTheme.colorScheme.tertiary               // Color(0xFF1E8E3E)
    val googleGreenLight = Color(0xFF34A853)                           // Vibrant Google Green
    val googleGreenDark = Color(0xFF137333)                            // Deeper Forest Green
    val googleBlue = MaterialTheme.colorScheme.primary                 // Color(0xFF1A73E8)
    val googleBlueLight = Color(0xFF4285F4)                            // Light Google Blue
    val googleBlueDark = Color(0xFF174EA6)                             // Deep Google Blue
    val disconnectedWater = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
    val disconnectedDeep = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f)

    // Active wave colors based on state
    val (activeFgStart, activeFgEnd, activeBgStart, activeBgEnd, activeRippleColor) = when {
        isConnected -> listOf(googleGreenLight, googleGreen, googleGreen, googleGreenDark, googleGreen)
        isConnecting -> listOf(googleBlueLight, googleBlue, googleBlue, googleBlueDark, googleBlue)
        else -> listOf(disconnectedWater, disconnectedWater, disconnectedDeep, disconnectedDeep, Color.Transparent)
    }

    // Pre-calculated static bubble coordinates
    val bubbles = remember {
        listOf(
            Bubble(relX = 0.32f, speed = 0.9f, size = 3.5f, seed = 1.2f),
            Bubble(relX = 0.50f, speed = 1.1f, size = 4.2f, seed = 2.8f),
            Bubble(relX = 0.68f, speed = 0.8f, size = 3.0f, seed = 4.1f),
            Bubble(relX = 0.42f, speed = 1.3f, size = 2.4f, seed = 5.5f),
            Bubble(relX = 0.60f, speed = 1.0f, size = 3.6f, seed = 0.7f)
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(176.dp)
        ) {
            // ─── Outer Concentric Atmospheric Ripple Ring ───
            if (isConnected || isConnecting) {
                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .scale(rippleScale1)
                        .clip(CircleShape)
                        .background(activeRippleColor.copy(alpha = rippleAlpha1))
                )
            }

            // ─── Main Glass Sphere Reservoir Button ───
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(132.dp)
                    .graphicsLayer {
                        scaleX = pressScale
                        scaleY = pressScale
                    }
                    .shadow(
                        elevation = if (isConnected) 8.dp else 2.dp,
                        shape = CircleShape,
                        spotColor = if (isConnected) googleGreen.copy(alpha = 0.3f) else Color(0x10000000)
                    )
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .then(
                        if (!isConnected) Modifier.border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        ) else Modifier.border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(googleGreenLight.copy(alpha = 0.6f), googleGreen.copy(alpha = 0.8f))
                            ),
                            shape = CircleShape
                        )
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
            ) {
                // ─── Canvas: Fluid Waves, Bubbles, and Glass Sphere Reflections ───
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Clip all fluid drawing strictly to the glass circle
                    val clipCircle = Path().apply {
                        addOval(androidx.compose.ui.geometry.Rect(0f, 0f, w, h))
                    }

                    clipPath(clipCircle) {
                        // 1. Water Level calculation (from bottom upward)
                        val waterBaseY = h * (1f - fillLevel)
                        val waveAmplitude = if (isConnected || isConnecting) 6.dp.toPx() else 3.dp.toPx()

                        // 2. Draw Background Wave (Layer 2 - Deep tone)
                        val bgPath = Path().apply {
                            moveTo(0f, h)
                            lineTo(0f, waterBaseY)
                            val step = 8f
                            var x = 0f
                            while (x <= w) {
                                val angle = (x / w) * (2.2 * PI) + wavePhase2
                                val y = waterBaseY + (sin(angle) * (waveAmplitude * 0.85f)).toFloat()
                                lineTo(x, y)
                                x += step
                            }
                            lineTo(w, h)
                            close()
                        }

                        val bgBrush = Brush.verticalGradient(
                            colors = listOf(activeBgStart.copy(alpha = 0.75f), activeBgEnd.copy(alpha = 0.85f)),
                            startY = waterBaseY,
                            endY = h
                        )
                        drawPath(bgPath, bgBrush)

                        // 3. Draw Foreground Wave (Layer 1 - Vibrant Tone)
                        val fgPath = Path().apply {
                            moveTo(0f, h)
                            lineTo(0f, waterBaseY)
                            val step = 8f
                            var x = 0f
                            while (x <= w) {
                                val angle = (x / w) * (2.0 * PI) + wavePhase1
                                val y = waterBaseY + (sin(angle) * waveAmplitude).toFloat()
                                lineTo(x, y)
                                x += step
                            }
                            lineTo(w, h)
                            close()
                        }

                        val fgBrush = Brush.verticalGradient(
                            colors = listOf(activeFgStart.copy(alpha = 0.90f), activeFgEnd.copy(alpha = 0.95f)),
                            startY = waterBaseY - waveAmplitude,
                            endY = h
                        )
                        drawPath(fgPath, fgBrush)

                        // 4. Draw Rising Micro-Bubbles
                        if (isConnected || isConnecting) {
                            bubbles.forEachIndexed { i, bubble ->
                                val bubbleCycle = (bubbleProgress * bubble.speed + (i * 0.2f)) % 1f
                                val bubbleY = h - (bubbleCycle * (h * fillLevel + 10f))
                                val wobbleX = sin(bubbleCycle * 4 * PI + bubble.seed).toFloat() * 5f
                                val bubbleX = (w * bubble.relX) + wobbleX

                                // Only draw if submerged beneath current water surface
                                if (bubbleY > waterBaseY - 2f) {
                                    val bubbleAlpha = (1f - (waterBaseY / bubbleY).coerceIn(0f, 1f)) * 0.75f
                                    drawCircle(
                                        color = Color.White.copy(alpha = bubbleAlpha.coerceIn(0.15f, 0.75f)),
                                        radius = bubble.size.dp.toPx(),
                                        center = Offset(bubbleX, bubbleY)
                                    )
                                }
                            }
                        }

                        // 5. Glass Specular Top Highlight (Curved 3D Glass Arc)
                        val glassGloss = Path().apply {
                            moveTo(w * 0.24f, h * 0.16f)
                            cubicTo(
                                w * 0.35f, h * 0.08f,
                                w * 0.65f, h * 0.08f,
                                w * 0.76f, h * 0.16f
                            )
                            cubicTo(
                                w * 0.65f, h * 0.23f,
                                w * 0.35f, h * 0.23f,
                                w * 0.24f, h * 0.16f
                            )
                            close()
                        }
                        drawPath(
                            path = glassGloss,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isConnected) 0.30f else 0.15f),
                                    Color.White.copy(alpha = 0f)
                                ),
                                startY = h * 0.08f,
                                endY = h * 0.23f
                            )
                        )
                    }
                }

                // ─── Center Icon (Shield or Power) without any rectangle border ───
                Icon(
                    imageVector = if (isConnected) Icons.Default.Shield else Icons.Default.PowerSettingsNew,
                    contentDescription = if (isConnected) "Disconnect" else "Connect",
                    tint = if (isConnected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ─── Status Pill (Google Material 3 Styled) ───
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    if (isConnected) MaterialTheme.colorScheme.tertiaryContainer
                    else if (isConnecting) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isConnected) MaterialTheme.colorScheme.tertiary
                            else if (isConnecting) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isConnected) "Protected" else if (isConnecting) "Connecting..." else "Not connected",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (isConnected) MaterialTheme.colorScheme.onTertiaryContainer
                    else if (isConnecting) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
