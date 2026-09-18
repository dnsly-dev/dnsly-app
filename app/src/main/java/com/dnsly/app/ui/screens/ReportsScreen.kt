package com.dnsly.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnsly.app.data.DnsRepository
import com.dnsly.app.model.QueryLog
import com.dnsly.app.ui.components.reports.ActivityChartCard
import com.dnsly.app.ui.components.reports.DomainDetailSheetContent
import com.dnsly.app.ui.components.reports.KpiCard
import com.dnsly.app.ui.components.reports.QueryLogRow
import com.dnsly.app.ui.components.reports.TopBlockedTrackersCard
import kotlinx.coroutines.launch
import java.util.Locale

private enum class QueryFilter { ALL, BLOCKED, PASSED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    repository: DnsRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val totalQueries by repository.totalQueries.collectAsState()
    val blockedQueries by repository.blockedQueries.collectAsState()
    val queryLogs by repository.queryLogs.collectAsState()
    val selectedServer by repository.selectedServer.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(QueryFilter.ALL) }
    var selectedLogForInspection by remember { mutableStateOf<QueryLog?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }
    var isLogsExpanded by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Compute top blocked domains leaderboard
    val topBlocked = remember(queryLogs) {
        queryLogs.filter { it.isBlocked }
            .groupingBy { it.domain }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(5)
    }

    // Filter query logs according to search text and filter chip
    val filteredLogs = remember(queryLogs, searchQuery, activeFilter) {
        queryLogs.filter { log ->
            val matchesSearch = searchQuery.isBlank() || log.domain.contains(searchQuery.trim(), ignoreCase = true)
            val matchesType = when (activeFilter) {
                QueryFilter.ALL -> true
                QueryFilter.BLOCKED -> log.isBlocked
                QueryFilter.PASSED -> !log.isBlocked
            }
            matchesSearch && matchesType
        }
    }

    val displayLogs = remember(filteredLogs, isLogsExpanded) {
        if (isLogsExpanded) filteredLogs.take(30) else filteredLogs.take(6)
    }

    // Saved data calculation (~50KB per prevented payload)
    val savedDataKb = blockedQueries * 50
    val savedDataString = if (savedDataKb >= 1024) {
        String.format(Locale.US, "%.1f MB", savedDataKb / 1024f)
    } else {
        "$savedDataKb KB"
    }

    val blockPercentage = if (totalQueries > 0) {
        ((blockedQueries.toFloat() / totalQueries) * 100).toInt()
    } else 0

    // Inspection Bottom Sheet
    if (selectedLogForInspection != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedLogForInspection = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            val log = selectedLogForInspection!!
            DomainDetailSheetContent(
                log = log,
                onCopyDomain = {
                    clipboardManager.setText(AnnotatedString(log.domain))
                    Toast.makeText(context, "Copied: ${log.domain}", Toast.LENGTH_SHORT).show()
                },
                onFilterInFeed = {
                    searchQuery = log.domain
                    coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                        selectedLogForInspection = null
                    }
                },
                onDismiss = {
                    coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                        selectedLogForInspection = null
                    }
                }
            )
        }
    }

    // Clear Logs Confirmation Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = "Clear Reports & Logs",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "You can clear the active query stream logs, or reset all cumulative statistics back to zero.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.resetAllStats()
                        showClearDialog = false
                        Toast.makeText(context, "All stats & logs reset", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Reset All")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        repository.clearLogs()
                        showClearDialog = false
                        Toast.makeText(context, "Query stream logs cleared", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear Stream Only")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Security Reports",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Live telemetry & query analytics",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear logs",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 4 KPI Summary Cards (2x2 Grid)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiCard(
                            title = "TOTAL QUERIES",
                            value = totalQueries.toString(),
                            subtext = "Encrypted port 53",
                            icon = Icons.Default.Language,
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            title = "THREATS BLOCKED",
                            value = blockedQueries.toString(),
                            subtext = "$blockPercentage% block rate",
                            badge = if (blockedQueries > 0) "$blockPercentage%" else null,
                            icon = Icons.Default.Security,
                            accentColor = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiCard(
                            title = "DATA SAVED",
                            value = savedDataString,
                            subtext = "Prevented payloads",
                            icon = Icons.Default.DataSaverOn,
                            accentColor = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            title = "RESOLVER PING",
                            value = if (selectedServer.latencyMs != null) "${selectedServer.latencyMs} ms" else "Fast",
                            subtext = selectedServer.name.take(15),
                            icon = Icons.Default.Speed,
                            accentColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Activity Timeline Chart
            item {
                ActivityChartCard(queryLogs = queryLogs)
            }

            // Top Blocked Domains Leaderboard Header
            item {
                Column {
                    Text(
                        text = "Top Blocked Trackers",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Highest volume advertising & telemetry hosts intercepted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Top Blocked Domains Leaderboard Card
            item {
                TopBlockedTrackersCard(
                    topBlocked = topBlocked,
                    onSelectDomain = { domain ->
                        selectedLogForInspection = queryLogs.firstOrNull { it.domain == domain }
                            ?: QueryLog(domain = domain, isBlocked = true)
                    }
                )
            }

            // Recent Activity Feed Header & Controls
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent DNS Activity",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${filteredLogs.size} logs recorded",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Filter by domain (e.g. google, telemetry...)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Filter Chips
                    val blockedCount = remember(queryLogs) { queryLogs.count { it.isBlocked } }
                    val passedCount = remember(queryLogs) { queryLogs.count { !it.isBlocked } }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = activeFilter == QueryFilter.ALL,
                            onClick = { activeFilter = QueryFilter.ALL },
                            label = { Text("All (${queryLogs.size})") },
                            shape = MaterialTheme.shapes.medium,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = activeFilter == QueryFilter.ALL,
                                borderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        FilterChip(
                            selected = activeFilter == QueryFilter.BLOCKED,
                            onClick = { activeFilter = QueryFilter.BLOCKED },
                            label = { Text("Blocked ($blockedCount)") },
                            shape = MaterialTheme.shapes.medium,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.error,
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = activeFilter == QueryFilter.BLOCKED,
                                borderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        FilterChip(
                            selected = activeFilter == QueryFilter.PASSED,
                            onClick = { activeFilter = QueryFilter.PASSED },
                            label = { Text("Passed ($passedCount)") },
                            shape = MaterialTheme.shapes.medium,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.tertiary,
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = activeFilter == QueryFilter.PASSED,
                                borderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }
                }
            }

            // Recent Queries Stream
            if (displayLogs.isEmpty()) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (queryLogs.isEmpty()) "No queries recorded yet" else "No queries matching filter",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (queryLogs.isEmpty()) "Connect VPN to monitor real-time network requests" else "Try clearing your search query",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(displayLogs, key = { it.id }) { log ->
                    QueryLogRow(
                        log = log,
                        onClick = { selectedLogForInspection = log }
                    )
                }

                // Expand / Collapse Toggle Button
                if (filteredLogs.size > 6) {
                    item {
                        TextButton(
                            onClick = { isLogsExpanded = !isLogsExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (isLogsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isLogsExpanded) "Show Less Recent Queries" else "Show More (${filteredLogs.size - 6} more)",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }
        }
    }
}
