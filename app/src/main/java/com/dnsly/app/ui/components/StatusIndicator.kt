package com.dnsly.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PowerButton(
    isConnected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "press_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isConnected) 1.15f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (isConnected) 0.18f else 0.0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    val googleBlue = MaterialTheme.colorScheme.primary
    val googleGreen = MaterialTheme.colorScheme.tertiary

    val buttonBg by animateColorAsState(
        targetValue = if (isConnected) googleGreen else Color.White,
        animationSpec = tween(350),
        label = "bg_color"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isConnected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(350),
        label = "icon_color"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(160.dp)
        ) {
            // Gentle pulse ring when connected
            if (isConnected) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(googleGreen.copy(alpha = pulseAlpha))
                )
            }

            // Main Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .shadow(
                        elevation = if (isConnected) 6.dp else 2.dp,
                        shape = CircleShape,
                        spotColor = if (isConnected) googleGreen.copy(alpha = 0.3f) else Color(0x0D000000)
                    )
                    .clip(CircleShape)
                    .background(buttonBg)
                    .then(
                        if (!isConnected) Modifier.border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        ) else Modifier
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.Shield else Icons.Default.PowerSettingsNew,
                    contentDescription = if (isConnected) "Disconnect" else "Connect",
                    tint = iconColor,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Status Pill
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    if (isConnected) googleGreen.copy(alpha = 0.08f)
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
                            if (isConnected) googleGreen
                            else MaterialTheme.colorScheme.outline
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isConnected) "Protected" else "Not connected",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (isConnected) googleGreen
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
