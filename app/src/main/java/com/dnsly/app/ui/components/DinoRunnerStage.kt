package com.dnsly.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.random.Random

private data class DustParticle(
    val x: Float,
    val y: Float,
    val size: Float,
    val alpha: Float
)

@Composable
fun DinoRunnerStage(
    isConnected: Boolean,
    isConnecting: Boolean = false,
    onTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val jumpOffset = remember { Animatable(0f) }
    val jumpRotation = remember { Animatable(0f) }

    // Dust particles generated during jump launch
    val dustParticles = remember { mutableStateListOf<DustParticle>() }

    val infiniteTransition = rememberInfiniteTransition(label = "dino_runner")

    // Running leg toggle cycle: alternating 0 and 1
    val legCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when {
                    isConnecting -> 90
                    isConnected -> 140
                    else -> 100000 // stationary
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "leg_cycle"
    )

    // Terrain track scroll offset (0 to 1)
    val groundOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when {
                    isConnecting -> 350
                    isConnected -> 800
                    else -> 100000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "ground_scroll"
    )

    // Cloud drift offset
    val cloudOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when {
                    isConnecting -> 3000
                    isConnected -> 6000
                    else -> 18000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "cloud_scroll"
    )

    // Speed wind streak pulse
    val windStreakAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wind_streak"
    )

    // Function to trigger jump
    val doJump = {
        if (!jumpOffset.isRunning) {
            coroutineScope.launch {
                // Spawn dust
                dustParticles.clear()
                repeat(4) {
                    dustParticles.add(
                        DustParticle(
                            x = Random.nextFloat() * 20f - 10f,
                            y = Random.nextFloat() * 6f,
                            size = Random.nextFloat() * 3f + 2f,
                            alpha = 0.7f
                        )
                    )
                }

                // Upward jump arc
                launch {
                    jumpRotation.animateTo(
                        targetValue = -12f,
                        animationSpec = tween(150, easing = FastOutSlowInEasing)
                    )
                    jumpRotation.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(200, easing = FastOutLinearInEasing)
                    )
                }

                jumpOffset.animateTo(
                    targetValue = -42f,
                    animationSpec = tween(220, easing = FastOutSlowInEasing)
                )
                jumpOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(200, easing = FastOutLinearInEasing)
                )
                dustParticles.clear()
            }
        }
    }

    // Colors
    val dinoColor = when {
        isConnected -> MaterialTheme.colorScheme.tertiary
        isConnecting -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
    }

    val groundColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val cloudColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    val accentGreen = MaterialTheme.colorScheme.tertiary
    val speedLineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                doJump()
                onTap()
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .padding(horizontal = 16.dp)
        ) {
            val width = size.width
            val height = size.height
            val groundY = height - 26f
            val dinoX = 54f
            val currentJumpY = jumpOffset.value
            val isLegStepA = (legCycle.toInt() % 2 == 0) && (isConnected || isConnecting)

            // 1. Draw Clouds
            drawClouds(width, height, cloudOffset, cloudColor)

            // 2. Draw Speed Lines if Connecting
            if (isConnecting) {
                drawSpeedLines(width, groundY, windStreakAlpha, speedLineColor)
            }

            // 3. Draw Moving Ground Line and Dots
            drawGround(width, groundY, groundOffset, isConnected || isConnecting, groundColor)

            // 4. Draw Dust Particles if Jumping
            if (currentJumpY < -2f) {
                dustParticles.forEach { p ->
                    drawCircle(
                        color = groundColor.copy(alpha = p.alpha),
                        radius = p.size,
                        center = Offset(dinoX + p.x, groundY + p.y - 2f)
                    )
                }
            }

            // 5. Draw the Chrome Dino
            drawChromeDino(
                x = dinoX,
                y = groundY + currentJumpY,
                isLegStepA = isLegStepA,
                isConnected = isConnected,
                isConnecting = isConnecting,
                dinoColor = dinoColor,
                accentColor = accentGreen
            )

            // 6. Draw Passing Shield Token / Power-up if Connected
            if (isConnected) {
                val tokenX = ((1f - groundOffset) * (width + 60f)) - 30f
                if (tokenX > dinoX + 25f) {
                    drawShieldToken(tokenX, groundY - 18f, accentGreen)
                }
            }
        }
    }
}

/**
 * Draws the Pixel Chrome Dino with legs, eye/sunglasses, arms, and optional shield badge.
 */
private fun DrawScope.drawChromeDino(
    x: Float,
    y: Float,
    isLegStepA: Boolean,
    isConnected: Boolean,
    isConnecting: Boolean,
    dinoColor: Color,
    accentColor: Color
) {
    val pixelSize = 2.4f

    // Helper to draw a pixel block relative to dino origin (bottom-left of feet is at x, y)
    fun pixel(px: Int, py: Int, w: Int = 1, h: Int = 1, color: Color = dinoColor) {
        drawRect(
            color = color,
            topLeft = Offset(x + (px * pixelSize), y - ((py + h) * pixelSize)),
            size = Size(w * pixelSize, h * pixelSize)
        )
    }

    // ── Dino Body & Tail ──
    pixel(0, 8, 1, 3)    // Tail tip
    pixel(1, 7, 2, 4)    // Tail curve
    pixel(3, 6, 2, 5)
    pixel(5, 5, 8, 8)    // Main torso
    pixel(4, 7, 7, 7)

    // ── Dino Neck & Head ──
    pixel(10, 10, 4, 7)  // Neck
    pixel(10, 14, 9, 6)  // Upper head
    pixel(19, 13, 3, 5)  // Snout
    pixel(14, 12, 8, 2)  // Lower jaw
    pixel(17, 13, 1, 1, Color.White) // Tooth

    // ── Dino Eye / Sunglasses ──
    if (isConnected) {
        // Cool Black Sunglasses with Emerald Frame
        pixel(13, 16, 5, 2, Color(0xFF1E293B))
        pixel(14, 17, 1, 1, Color.White) // Glass shine
        pixel(12, 17, 1, 1, Color(0xFF1E293B)) // Frame arm
    } else {
        // Normal Pixel Eye
        pixel(13, 17, 2, 2, Color.White)
        pixel(14, 17, 1, 1, dinoColor)
    }

    // ── Dino Arm ──
    pixel(13, 8, 3, 1)
    pixel(15, 7, 1, 2)

    // ── Dino Legs & Feet Animation ──
    if (!isConnected && !isConnecting) {
        // Standing still (both legs planted)
        pixel(6, 0, 2, 5)   // Back leg
        pixel(6, 0, 3, 1)   // Back foot
        pixel(10, 0, 2, 5)  // Front leg
        pixel(10, 0, 3, 1)  // Front foot
    } else if (isLegStepA) {
        // Leg Step A: Front leg back, back leg forward
        pixel(5, 0, 2, 5)
        pixel(4, 0, 3, 1)
        pixel(11, 2, 2, 4)
        pixel(12, 2, 2, 1)
    } else {
        // Leg Step B: Front leg forward, back leg raised
        pixel(7, 2, 2, 4)
        pixel(6, 2, 2, 1)
        pixel(10, 0, 2, 5)
        pixel(10, 0, 3, 1)
    }

    // ── Shield Emblem (Chest Badge when Protected) ──
    if (isConnected) {
        pixel(8, 7, 3, 3, accentColor)
        pixel(9, 8, 1, 1, Color.White)
    }
}

/**
 * Draws pixel clouds drifting in the background.
 */
private fun DrawScope.drawClouds(width: Float, height: Float, offset: Float, color: Color) {
    val cloud1X = ((1f - offset) * (width + 120f) - 60f) % (width + 120f)
    val cloud2X = ((1f - (offset + 0.5f) % 1f) * (width + 120f) - 60f)

    fun drawCloudAt(cx: Float, cy: Float) {
        drawRoundRect(
            color = color,
            topLeft = Offset(cx, cy),
            size = Size(42f, 14f),
            cornerRadius = CornerRadius(7f, 7f)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(cx + 8f, cy - 8f),
            size = Size(24f, 16f),
            cornerRadius = CornerRadius(8f, 8f)
        )
    }

    drawCloudAt(cloud1X, 22f)
    drawCloudAt(cloud2X, 38f)
}

/**
 * Draws moving ground line and terrain pixel bumps.
 */
private fun DrawScope.drawGround(
    width: Float,
    groundY: Float,
    offset: Float,
    isMoving: Boolean,
    color: Color
) {
    // Continuous baseline
    drawLine(
        color = color,
        start = Offset(0f, groundY),
        end = Offset(width, groundY),
        strokeWidth = 2f
    )

    // Moving terrain dots/dashes
    val spacing = 28f
    val shift = if (isMoving) offset * spacing else 0f
    var curX = -shift

    while (curX < width + spacing) {
        if (curX >= 0f) {
            drawRect(
                color = color,
                topLeft = Offset(curX, groundY + 4f),
                size = Size(6f, 2f)
            )
            drawRect(
                color = color,
                topLeft = Offset(curX + 12f, groundY + 8f),
                size = Size(3f, 2f)
            )
        }
        curX += spacing
    }
}

/**
 * Draws aerodynamic speed lines behind the runner during connection transition.
 */
private fun DrawScope.drawSpeedLines(width: Float, groundY: Float, alpha: Float, color: Color) {
    val lineCol = color.copy(alpha = alpha)
    drawLine(
        color = lineCol,
        start = Offset(12f, groundY - 32f),
        end = Offset(42f, groundY - 32f),
        strokeWidth = 2f
    )
    drawLine(
        color = lineCol,
        start = Offset(4f, groundY - 18f),
        end = Offset(36f, groundY - 18f),
        strokeWidth = 2.5f
    )
    drawLine(
        color = lineCol,
        start = Offset(16f, groundY - 8f),
        end = Offset(46f, groundY - 8f),
        strokeWidth = 1.5f
    )
}

/**
 * Draws floating pixel shield collectable tokens on the track.
 */
private fun DrawScope.drawShieldToken(x: Float, y: Float, color: Color) {
    val path = Path().apply {
        moveTo(x, y - 6f)
        lineTo(x + 7f, y - 2f)
        lineTo(x + 5f, y + 6f)
        lineTo(x, y + 9f)
        lineTo(x - 5f, y + 6f)
        lineTo(x - 7f, y - 2f)
        close()
    }
    drawPath(path = path, color = color.copy(alpha = 0.85f), style = Fill)
}
