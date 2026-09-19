package com.dnsly.app.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
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

    // Dynamic water level: 18% when disconnected, 50% when connecting, 74% when connected
    val targetFillLevel = when {
        isConnecting -> 0.52f
        isConnected -> 0.74f
        else -> 0.18f
    }

    val fillLevel by animateFloatAsState(
        targetValue = targetFillLevel,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 120f),
        label = "fluid_fill_level"
    )

    // Continuous infinite transitions for organic wave and bubble motion
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

    // Background wave phase (shifted and different duration for parallax water depth)
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

    // Concentric atmospheric ripple scale (1.0 to 1.35)
    val rippleScale1 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isConnected || isConnecting) 1.32f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_scale_1"
    )
    val rippleAlpha1 by infiniteTransition.animateFloat(
        initialValue = if (isConnected || isConnecting) 0.28f else 0.0f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_alpha_1"
    )

    val rippleScale2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isConnected || isConnecting) 1.20f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_scale_2"
    )
    val rippleAlpha2 by infiniteTransition.animateFloat(
        initialValue = if (isConnected || isConnecting) 0.22f else 0.0f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_alpha_2"
    )

    // Color palettes
    val primaryCyan = Color(0xFF06B6D4)
    val primaryEmerald = Color(0xFF10B981)
    val darkEmerald = Color(0xFF059669)
    val deepCyan = Color(0xFF0891B2)
    val disconnectedWater = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
    val disconnectedDeep = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f)

    // Pre-calculated static bubble coordinates
    val bubbles = remember {
        listOf(
            Bubble(relX = 0.32f, speed = 0.9f, size = 3.5f, seed = 1.2f),
            Bubble(relX = 0.50f, speed = 1.1f, size = 4.5f, seed = 2.8f),
            Bubble(relX = 0.68f, speed = 0.8f, size = 3.0f, seed = 4.1f),
            Bubble(relX = 0.42f, speed = 1.3f, size = 2.5f, seed = 5.5f),
            Bubble(relX = 0.60f, speed = 1.0f, size = 3.8f, seed = 0.7f)
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(190.dp)
        ) {
            // ─── Outer Concentric Atmospheric Ripple Rings ───
            if (isConnected || isConnecting) {
                // Outer Ripple 1
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(rippleScale1)
                        .clip(CircleShape)
                        .background(primaryEmerald.copy(alpha = rippleAlpha1))
                )
                // Outer Ripple 2
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(rippleScale2)
                        .clip(CircleShape)
                        .background(primaryCyan.copy(alpha = rippleAlpha2))
                )
            }

            // ─── Main Glass Sphere Reservoir Button ───
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(140.dp)
                    .graphicsLayer {
                        scaleX = pressScale
                        scaleY = pressScale
                    }
                    .shadow(
                        elevation = if (isConnected) 12.dp else 4.dp,
                        shape = CircleShape,
                        spotColor = if (isConnected) primaryEmerald.copy(alpha = 0.4f) else Color(0x15000000)
                    )
                    .clip(CircleShape)
                    .background(
                        if (isConnected) Color(0xFF0F172A)
                        else MaterialTheme.colorScheme.surface
                    )
                    .then(
                        if (!isConnected) Modifier.border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                            shape = CircleShape
                        ) else Modifier.border(
                            width = 2.5.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(primaryCyan.copy(alpha = 0.8f), primaryEmerald.copy(alpha = 0.9f))
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
                    val circleCenter = Offset(w / 2f, h / 2f)
                    val circleRadius = w / 2f

                    // Clip all fluid drawing strictly to the glass circle
                    val clipCircle = Path().apply {
                        addOval(androidx.compose.ui.geometry.Rect(0f, 0f, w, h))
                    }

                    clipPath(clipCircle) {
                        // 1. Water Level calculation (from bottom upward)
                        val waterBaseY = h * (1f - fillLevel)
                        val waveAmplitude = if (isConnected || isConnecting) 7.dp.toPx() else 3.dp.toPx()

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

                        val bgBrush = if (isConnected || isConnecting) {
                            Brush.verticalGradient(
                                colors = listOf(deepCyan.copy(alpha = 0.75f), darkEmerald.copy(alpha = 0.85f)),
                                startY = waterBaseY,
                                endY = h
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(disconnectedDeep, disconnectedDeep),
                                startY = waterBaseY,
                                endY = h
                            )
                        }
                        drawPath(bgPath, bgBrush)

                        // 3. Draw Foreground Wave (Layer 1 - Vibrant Emerald/Cyan)
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

                        val fgBrush = if (isConnected || isConnecting) {
                            Brush.verticalGradient(
                                colors = listOf(
                                    primaryCyan.copy(alpha = 0.88f),
                                    primaryEmerald.copy(alpha = 0.95f)
                                ),
                                startY = waterBaseY - waveAmplitude,
                                endY = h
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(disconnectedWater, disconnectedWater),
                                startY = waterBaseY - waveAmplitude,
                                endY = h
                            )
                        }
                        drawPath(fgPath, fgBrush)

                        // 4. Draw Rising Micro-Bubbles
                        if (isConnected || isConnecting) {
                            bubbles.forEachIndexed { i, bubble ->
                                val bubbleCycle = (bubbleProgress * bubble.speed + (i * 0.2f)) % 1f
                                val bubbleY = h - (bubbleCycle * (h * fillLevel + 10f))
                                val wobbleX = sin(bubbleCycle * 4 * PI + bubble.seed).toFloat() * 6f
                                val bubbleX = (w * bubble.relX) + wobbleX

                                // Only draw if submerged beneath current water surface
                                if (bubbleY > waterBaseY - 4f) {
                                    val bubbleAlpha = (1f - (waterBaseY / bubbleY).coerceIn(0f, 1f)) * 0.8f
                                    drawCircle(
                                        color = Color.White.copy(alpha = bubbleAlpha.coerceIn(0.2f, 0.8f)),
                                        radius = bubble.size.dp.toPx(),
                                        center = Offset(bubbleX, bubbleY)
                                    )
                                }
                            }
                        }

                        // 5. Glass Specular Top Highlight (Curved 3D Glass Arc)
                        val glassGloss = Path().apply {
                            moveTo(w * 0.22f, h * 0.16f)
                            cubicTo(
                                w * 0.35f, h * 0.08f,
                                w * 0.65f, h * 0.08f,
                                w * 0.78f, h * 0.16f
                            )
                            cubicTo(
                                w * 0.65f, h * 0.24f,
                                w * 0.35f, h * 0.24f,
                                w * 0.22f, h * 0.16f
                            )
                            close()
                        }
                        drawPath(
                            path = glassGloss,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isConnected) 0.35f else 0.20f),
                                    Color.White.copy(alpha = 0f)
                                ),
                                startY = h * 0.08f,
                                endY = h * 0.24f
                            )
                        )
                    }
                }

                // ─── Center Icon (Shield or Power) with Drop Shadow ───
                Icon(
                    imageVector = if (isConnected) Icons.Default.Shield else Icons.Default.PowerSettingsNew,
                    contentDescription = if (isConnected) "Disconnect" else "Connect",
                    tint = if (isConnected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(50.dp)
                        .graphicsLayer {
                            shadowElevation = if (isConnected) 8f else 0f
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ─── Status Pill ───
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    if (isConnected) primaryEmerald.copy(alpha = 0.10f)
                    else MaterialTheme.colorScheme.surfaceContainerHigh
                )
                .padding(horizontal = 18.dp, vertical = 8.dp)
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
                            if (isConnected) primaryEmerald
                            else MaterialTheme.colorScheme.outline
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isConnected) "Protected" else if (isConnecting) "Connecting..." else "Not connected",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (isConnected) primaryEmerald
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
