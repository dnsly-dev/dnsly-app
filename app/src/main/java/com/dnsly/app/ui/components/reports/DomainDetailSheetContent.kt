package com.dnsly.app.ui.components.reports

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dnsly.app.model.QueryLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DomainDetailSheetContent(
    log: QueryLog,
    onCopyDomain: () -> Unit,
    onFilterInFeed: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fullDateFormat = remember { SimpleDateFormat("EEEE, MMM d, yyyy · hh:mm:ss a", Locale.getDefault()) }
    val formattedDate = remember(log.timestamp) { fullDateFormat.format(Date(log.timestamp)) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (log.isBlocked) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.tertiaryContainer
                    )
            ) {
                Icon(
                    imageVector = if (log.isBlocked) Icons.Default.Block else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (log.isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.domain,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (log.isBlocked) "Blocked" else "Resolved",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (log.isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Details
        DetailSpecRow(label = "Status", value = if (log.isBlocked) "Blocked" else "Resolved")
        DetailSpecRow(
            label = "Protocol",
            value = if (log.isBlocked) "Local interception" else "UDP (port 53)"
        )
        DetailSpecRow(label = "Reason", value = log.reason)
        DetailSpecRow(label = "Query type", value = log.queryType)
        DetailSpecRow(label = "Time", value = formattedDate)
        DetailSpecRow(
            label = "Latency",
            value = if (log.isBlocked) "Instant" else "${log.latencyMs} ms"
        )
        if (log.upstreamServer.isNotBlank()) {
            DetailSpecRow(label = "Resolver", value = log.upstreamServer)
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCopyDomain,
                shape = CircleShape,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy")
            }

            Button(
                onClick = onFilterInFeed,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Filter")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun DetailSpecRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.65f)
        )
    }
}
