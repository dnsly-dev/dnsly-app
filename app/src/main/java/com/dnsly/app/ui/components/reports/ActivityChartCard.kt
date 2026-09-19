package com.dnsly.app.ui.components.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dnsly.app.model.QueryLog
import kotlin.math.max

@Composable
fun ActivityChartCard(
    queryLogs: List<QueryLog>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Activity",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Last 15 minutes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Safe",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Blocked",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Chart
            val bucketCount = 8
            val buckets = remember(queryLogs) {
                val now = System.currentTimeMillis()
                val windowMs = 15 * 60 * 1000L
                val bucketDuration = windowMs / bucketCount

                val resolvedCounts = IntArray(bucketCount)
                val blockedCounts = IntArray(bucketCount)

                queryLogs.forEach { log ->
                    val age = now - log.timestamp
                    if (age in 0..windowMs) {
                        val index = ((windowMs - age) / bucketDuration).toInt().coerceIn(0, bucketCount - 1)
                        if (log.isBlocked) {
                            blockedCounts[index]++
                        } else {
                            resolvedCounts[index]++
                        }
                    }
                }

                List(bucketCount) { i -> Pair(resolvedCounts[i], blockedCounts[i]) }
            }

            val maxVal = remember(buckets) {
                val maxObserved = buckets.maxOfOrNull { it.first + it.second } ?: 0
                max(maxObserved, 4)
            }

            val primaryColor = MaterialTheme.colorScheme.primary
            val errorColor = MaterialTheme.colorScheme.error
            val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val barWidth = (canvasWidth / (bucketCount * 1.8f)).coerceIn(14.dp.toPx(), 24.dp.toPx())
                val spacing = (canvasWidth - (barWidth * bucketCount)) / (bucketCount - 1)
                val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

                for (i in 0 until bucketCount) {
                    val x = i * (barWidth + spacing)
                    val (safe, blocked) = buckets[i]
                    val total = safe + blocked

                    // Background track
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth, canvasHeight),
                        cornerRadius = cornerRadius
                    )

                    if (total > 0) {
                        val totalHeight = (total.toFloat() / maxVal) * canvasHeight
                        val blockedHeight = (blocked.toFloat() / maxVal) * canvasHeight
                        val safeHeight = totalHeight - blockedHeight
                        val yStart = canvasHeight - totalHeight

                        if (safeHeight > 0) {
                            drawRoundRect(
                                color = primaryColor,
                                topLeft = Offset(x, canvasHeight - safeHeight),
                                size = Size(barWidth, safeHeight),
                                cornerRadius = cornerRadius
                            )
                        }

                        if (blockedHeight > 0) {
                            drawRoundRect(
                                color = errorColor,
                                topLeft = Offset(x, yStart),
                                size = Size(barWidth, blockedHeight),
                                cornerRadius = cornerRadius
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Time labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "15m ago",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "10m",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "5m",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Now",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
