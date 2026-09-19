package com.dnsly.app.ui.components.reports

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnsly.app.model.QueryLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

enum class ChartTimeRange(val label: String, val durationMs: Long, val bucketCount: Int) {
    LAST_1H("1h", 60 * 60 * 1000L, 12),
    LAST_6H("6h", 6 * 60 * 60 * 1000L, 12),
    LAST_24H("24h", 24 * 60 * 60 * 1000L, 12),
    LAST_7D("7d", 7 * 24 * 60 * 60 * 1000L, 14),
    LAST_30D("30d", 30L * 24 * 60 * 60 * 1000L, 15)
}

private data class ChartBucket(
    val timeLabel: String,
    val safeCount: Int,
    val blockedCount: Int,
    val totalCount: Int
)

@Composable
fun ActivityChartCard(
    queryLogs: List<QueryLog>,
    modifier: Modifier = Modifier
) {
    var selectedRange by remember { mutableStateOf(ChartTimeRange.LAST_24H) }
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    val buckets = remember(queryLogs, selectedRange) {
        val now = System.currentTimeMillis()
        val windowMs = selectedRange.durationMs
        val count = selectedRange.bucketCount
        val bucketDuration = windowMs / count

        val safeCounts = IntArray(count)
        val blockedCounts = IntArray(count)

        queryLogs.forEach { log ->
            val age = now - log.timestamp
            if (age in 0..windowMs) {
                val bucketIdx = ((windowMs - age) / bucketDuration).toInt().coerceIn(0, count - 1)
                if (log.isBlocked) {
                    blockedCounts[bucketIdx]++
                } else {
                    safeCounts[bucketIdx]++
                }
            }
        }

        val timeFormat = when (selectedRange) {
            ChartTimeRange.LAST_1H, ChartTimeRange.LAST_6H -> SimpleDateFormat("HH:mm", Locale.US)
            ChartTimeRange.LAST_24H -> SimpleDateFormat("HH:mm", Locale.US)
            ChartTimeRange.LAST_7D -> SimpleDateFormat("EEE", Locale.US)
            ChartTimeRange.LAST_30D -> SimpleDateFormat("MMM d", Locale.US)
        }

        List(count) { i ->
            val bucketEndTime = now - (count - 1 - i) * bucketDuration
            ChartBucket(
                timeLabel = timeFormat.format(Date(bucketEndTime)),
                safeCount = safeCounts[i],
                blockedCount = blockedCounts[i],
                totalCount = safeCounts[i] + blockedCounts[i]
            )
        }
    }

    val maxTotal = remember(buckets) {
        max(buckets.maxOfOrNull { it.totalCount } ?: 0, 5)
    }

    val totalQueriesInRange = remember(buckets) { buckets.sumOf { it.totalCount } }
    val totalBlockedInRange = remember(buckets) { buckets.sumOf { it.blockedCount } }

    val primaryColor = MaterialTheme.colorScheme.primary       // Google Blue #1A73E8
    val primaryCyan = Color(0xFF00B4D8)
    val blockedRed = MaterialTheme.colorScheme.error           // Google Red #D93025
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row: Title & Range Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Traffic Activity",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$totalQueriesInRange queries • $totalBlockedInRange blocked",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Range Selector Pills
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ChartTimeRange.entries.forEach { range ->
                        val isSelected = selectedRange == range
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .clip(CircleShape)
                                .pointerInput(range) {
                                    detectTapGestures {
                                        selectedRange = range
                                        selectedPointIndex = null
                                    }
                                }
                        ) {
                            Text(
                                text = range.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 11.sp
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Point Info Banner (When Scrubbing/Tapping)
            if (selectedPointIndex != null && selectedPointIndex in buckets.indices) {
                val b = buckets[selectedPointIndex!!]
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Time: ${b.timeLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "${b.safeCount} safe",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = primaryColor
                            )
                            Text(
                                text = "${b.blockedCount} blocked",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = blockedRed
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                // Legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(primaryColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Safe Queries",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(blockedRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Blocked Threats",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ─── Spline Area Canvas Chart ───
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .pointerInput(buckets) {
                            detectTapGestures(
                                onPress = { offset ->
                                    val count = buckets.size
                                    val step = size.width / (count - 1).coerceAtLeast(1)
                                    val touchedIdx = (offset.x / step).toInt().coerceIn(0, count - 1)
                                    selectedPointIndex = touchedIdx
                                }
                            )
                        }
                        .pointerInput(buckets) {
                            detectDragGestures { change, _ ->
                                val count = buckets.size
                                val step = size.width / (count - 1).coerceAtLeast(1)
                                val touchedIdx = (change.position.x / step).toInt().coerceIn(0, count - 1)
                                selectedPointIndex = touchedIdx
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val bottomPadding = 8f
                    val topPadding = 12f
                    val chartHeight = h - topPadding - bottomPadding
                    val count = buckets.size
                    val stepX = w / (count - 1).coerceAtLeast(1)

                    // 1. Horizontal Dotted Gridlines
                    val gridLines = 3
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    for (i in 0..gridLines) {
                        val gridY = topPadding + (i * (chartHeight / gridLines))
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, gridY),
                            end = Offset(w, gridY),
                            strokeWidth = 1f,
                            pathEffect = dashEffect
                        )
                    }

                    // Pre-compute points
                    val totalPoints = mutableListOf<Offset>()
                    val blockedPoints = mutableListOf<Offset>()

                    for (i in 0 until count) {
                        val px = i * stepX
                        val total = buckets[i].totalCount.toFloat()
                        val blocked = buckets[i].blockedCount.toFloat()

                        val totalY = (h - bottomPadding) - ((total / maxTotal) * chartHeight)
                        val blockedY = (h - bottomPadding) - ((blocked / maxTotal) * chartHeight)

                        totalPoints.add(Offset(px, totalY))
                        blockedPoints.add(Offset(px, blockedY))
                    }

                    // Helper to build smooth cubic spline
                    fun buildSplinePath(points: List<Offset>, isClosed: Boolean): Path {
                        val path = Path()
                        if (points.isEmpty()) return path
                        path.moveTo(points[0].x, points[0].y)

                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val controlX1 = p0.x + (p1.x - p0.x) / 2f
                            val controlY1 = p0.y
                            val controlX2 = p0.x + (p1.x - p0.x) / 2f
                            val controlY2 = p1.y
                            path.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                        }

                        if (isClosed) {
                            path.lineTo(points.last().x, h - bottomPadding)
                            path.lineTo(points.first().x, h - bottomPadding)
                            path.close()
                        }
                        return path
                    }

                    // 2. Draw Total Safe Curve & Gradient Fill
                    val totalFilledPath = buildSplinePath(totalPoints, isClosed = true)
                    val totalStrokePath = buildSplinePath(totalPoints, isClosed = false)

                    drawPath(
                        path = totalFilledPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.28f), primaryColor.copy(alpha = 0.0f)),
                            startY = topPadding,
                            endY = h - bottomPadding
                        )
                    )

                    drawPath(
                        path = totalStrokePath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(primaryCyan, primaryColor)
                        ),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 3. Draw Blocked Curve & Gradient Fill (if threats exist)
                    if (totalBlockedInRange > 0) {
                        val blockedFilledPath = buildSplinePath(blockedPoints, isClosed = true)
                        val blockedStrokePath = buildSplinePath(blockedPoints, isClosed = false)

                        drawPath(
                            path = blockedFilledPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(blockedRed.copy(alpha = 0.32f), blockedRed.copy(alpha = 0.0f)),
                                startY = topPadding,
                                endY = h - bottomPadding
                            )
                        )

                        drawPath(
                            path = blockedStrokePath,
                            color = blockedRed,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // 4. Draw Selected Point Cursor / Guide Line
                    if (selectedPointIndex != null && selectedPointIndex in totalPoints.indices) {
                        val idx = selectedPointIndex!!
                        val highlightX = totalPoints[idx].x
                        val totalPt = totalPoints[idx]
                        val blockedPt = blockedPoints[idx]

                        // Vertical guide line
                        drawLine(
                            color = primaryColor.copy(alpha = 0.6f),
                            start = Offset(highlightX, topPadding),
                            end = Offset(highlightX, h - bottomPadding),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )

                        // Safe Point Dot
                        drawCircle(
                            color = Color.White,
                            radius = 5.5.dp.toPx(),
                            center = totalPt
                        )
                        drawCircle(
                            color = primaryColor,
                            radius = 4.dp.toPx(),
                            center = totalPt
                        )

                        // Blocked Point Dot
                        if (buckets[idx].blockedCount > 0) {
                            drawCircle(
                                color = Color.White,
                                radius = 5.5.dp.toPx(),
                                center = blockedPt
                            )
                            drawCircle(
                                color = blockedRed,
                                radius = 4.dp.toPx(),
                                center = blockedPt
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Time Axis Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = buckets.firstOrNull()?.timeLabel ?: "",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = buckets.getOrNull(buckets.size / 2)?.timeLabel ?: "",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Now",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
