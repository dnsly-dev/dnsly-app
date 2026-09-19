package com.dnsly.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import java.text.NumberFormat
import java.util.Locale
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnsly.app.data.DnsRepository
import com.dnsly.app.model.DnsServer
import com.dnsly.app.model.DnsType
import com.dnsly.app.ui.components.DnsCard
import com.dnsly.app.ui.components.PowerButton
import kotlinx.coroutines.launch

import com.dnsly.app.service.blocklist.BlocklistManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repository: DnsRepository,
    onToggleVpn: () -> Unit,
    onNavigateToList: () -> Unit,
    onNavigateToReports: () -> Unit = {},
    onNavigateToShield: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val blocklistManager = remember { BlocklistManager.getInstance(context) }
    val isShieldEnabled by blocklistManager.isShieldEnabled.collectAsState()
    val shieldRulesCount by blocklistManager.totalBlockedCount.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val isConnected by repository.isVpnConnected.collectAsState()
    val selectedServer by repository.selectedServer.collectAsState()
    val totalQueries by repository.totalQueries.collectAsState()
    val blockedQueries by repository.blockedQueries.collectAsState()
    val servers by repository.servers.collectAsState()

    var showCustomDialog by remember { mutableStateOf(false) }
    var showDnsSheet by remember { mutableStateOf(false) }
    var showShieldSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val shieldSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    val quickPickServers = remember(servers) {
        val topIds = listOf("adguard_default", "cloudflare_security", "quad9_security", "controld_ads", "google_public", "mullvad_adblock")
        servers.filter { it.id in topIds || it.isCustom }.take(6)
    }

    if (showCustomDialog) {
        CustomDnsDialog(
            onDismiss = { showCustomDialog = false },
            onSave = { name, pIp, sIp, host, doh ->
                val newServer = repository.addCustomServer(name, pIp, sIp, host, doh)
                repository.selectServer(newServer)
                if (isConnected) {
                    onToggleVpn()
                    onToggleVpn()
                }
                newServer
            }
        )
    }

    if (showDnsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDnsSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            DnsSelectorSheetContent(
                servers = servers,
                selectedServer = selectedServer,
                onSelectServer = { server ->
                    repository.selectServer(server)
                    if (isConnected) {
                        onToggleVpn()
                        onToggleVpn()
                    }
                    coroutineScope.launch {
                        sheetState.hide()
                        showDnsSheet = false
                    }
                },
                onAddCustomClick = {
                    showDnsSheet = false
                    showCustomDialog = true
                }
            )
        }
    }

    if (showShieldSheet) {
        ModalBottomSheet(
            onDismissRequest = { showShieldSheet = false },
            sheetState = shieldSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            ShieldSheetContent(
                blocklistManager = blocklistManager,
                onClose = {
                    coroutineScope.launch {
                        shieldSheetState.hide()
                        showShieldSheet = false
                    }
                }
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = innerPadding.calculateTopPadding() + 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 32.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ─── Section 1: Hero Status ───
            HeroStatusSection(
                isConnected = isConnected,
                selectedServer = selectedServer,
                onToggle = onToggleVpn,
                onOpenDnsSheet = { showDnsSheet = true }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ─── Section 2: DNS Provider Card ───
            DnsProviderCard(
                servers = servers,
                selectedServer = selectedServer,
                quickPickServers = quickPickServers,
                onSelectServer = { server ->
                    repository.selectServer(server)
                    if (isConnected) {
                        onToggleVpn()
                        onToggleVpn()
                    }
                },
                onOpenDirectory = { showDnsSheet = true },
                onAddCustom = { showCustomDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ─── Section 3: Local Ad & Tracker Shield ───
            LocalShieldQuickCard(
                isEnabled = isShieldEnabled,
                rulesCount = shieldRulesCount,
                onToggle = { blocklistManager.setShieldEnabled(it) },
                onConfigure = { showShieldSheet = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ─── Section 4: Quick Stats ───
            QuickStatsRow(
                totalQueries = totalQueries,
                blockedQueries = blockedQueries,
                onTap = onNavigateToReports
            )
        }
    }
}

// ═══════════════════════════════════════════
// Hero Status Section — Clean, centered Fluid Glass Sphere
// ═══════════════════════════════════════════
@Composable
private fun HeroStatusSection(
    isConnected: Boolean,
    selectedServer: DnsServer,
    onToggle: () -> Unit,
    onOpenDnsSheet: () -> Unit
) {
    var isConnectingTransient by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Fluid Glass Sphere Power Button
        PowerButton(
            isConnected = isConnected,
            isConnecting = isConnectingTransient,
            onClick = {
                if (!isConnected) {
                    isConnectingTransient = true
                    coroutineScope.launch {
                        kotlinx.coroutines.delay(1200)
                        isConnectingTransient = false
                    }
                }
                onToggle()
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // DNS provider pill (tappable to switch)
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onOpenDnsSheet)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(selectedServer.brandColor.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = selectedServer.initials.take(2),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = selectedServer.brandColor
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedServer.name,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Change DNS",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════
// DNS Provider Card — Clean, single card
// ═══════════════════════════════════════════
@Composable
private fun DnsProviderCard(
    servers: List<DnsServer>,
    selectedServer: DnsServer,
    quickPickServers: List<DnsServer>,
    onSelectServer: (DnsServer) -> Unit,
    onOpenDirectory: () -> Unit,
    onAddCustom: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "DNS Provider",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                TextButton(
                    onClick = onOpenDirectory,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "See all",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Active provider display
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(onClick = onOpenDirectory)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(selectedServer.brandColor.copy(alpha = 0.12f))
                    ) {
                        Text(
                            text = selectedServer.initials,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = selectedServer.brandColor
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedServer.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = selectedServer.categories,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onOpenDirectory,
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Switch Chips
            Text(
                text = "Quick switch",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickPickServers.forEach { server ->
                    val isCurrent = server.id == selectedServer.id
                    QuickPickChip(
                        server = server,
                        isSelected = isCurrent,
                        onClick = { onSelectServer(server) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Add custom DNS link
            TextButton(
                onClick = onAddCustom,
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Add custom DNS",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ═══════════════════════════════════════════
// Quick Pick Chip — Clean tonal chip
// ═══════════════════════════════════════════
@Composable
private fun QuickPickChip(
    server: DnsServer,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color.White.copy(alpha = 0.2f)
                        else server.brandColor.copy(alpha = 0.12f)
                    )
            ) {
                Text(
                    text = server.initials.take(2),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = if (isSelected) Color.White else server.brandColor
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = server.name.take(14),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )

            if (isSelected) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════
// Quick Stats Row — 3 tonal metric boxes
// ═══════════════════════════════════════════
@Composable
private fun QuickStatsRow(
    totalQueries: Long,
    blockedQueries: Long,
    onTap: () -> Unit
) {
    val blockPercent = if (totalQueries > 0) ((blockedQueries.toFloat() / totalQueries) * 100).toInt() else 0

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onTap)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Activity",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "View details",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total
                StatPill(
                    label = "Queries",
                    value = formatCount(totalQueries),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                // Blocked
                StatPill(
                    label = "Blocked",
                    value = formatCount(blockedQueries),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )

                // Block Rate
                StatPill(
                    label = "Block rate",
                    value = "$blockPercent%",
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = color.copy(alpha = 0.08f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatCount(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

// ═══════════════════════════════════════════
// DNS Selector Bottom Sheet
// ═══════════════════════════════════════════
@Composable
fun DnsSelectorSheetContent(
    servers: List<DnsServer>,
    selectedServer: DnsServer,
    onSelectServer: (DnsServer) -> Unit,
    onAddCustomClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<DnsType?>(null) }

    val filtered = remember(servers, searchQuery, selectedType) {
        servers.filter { s ->
            val matchQuery = searchQuery.isBlank() ||
                    s.name.contains(searchQuery, ignoreCase = true) ||
                    s.ipv4Primary.contains(searchQuery, ignoreCase = true) ||
                    s.categories.contains(searchQuery, ignoreCase = true)
            val matchType = selectedType == null || s.type == selectedType
            matchQuery && matchType
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .padding(horizontal = 20.dp)
    ) {
        // Title Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Choose DNS provider",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onAddCustomClick)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Custom",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    "Search providers...",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = CircleShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedType == null,
                onClick = { selectedType = null },
                label = { Text("All (${servers.size})") },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )

            DnsType.values().forEach { type ->
                val count = servers.count { it.type == type }
                if (count > 0) {
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = if (selectedType == type) null else type },
                        label = { Text("${type.displayName} ($count)") },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Provider list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filtered, key = { it.id }) { server ->
                DnsCard(
                    server = server,
                    isSelected = server.id == selectedServer.id,
                    onSelect = { onSelectServer(server) }
                )
            }
        }
    }
}

@Composable
private fun LocalShieldQuickCard(
    isEnabled: Boolean,
    rulesCount: Int,
    onToggle: (Boolean) -> Unit,
    onConfigure: () -> Unit
) {
    val numberFormatter = remember { NumberFormat.getNumberInstance(Locale.US) }
    val googleGreen = MaterialTheme.colorScheme.tertiary

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row: Title + "Manage" link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Ad & Tracker Shield",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                TextButton(
                    onClick = onConfigure,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Configure",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Status Box
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(onClick = onConfigure)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isEnabled) googleGreen.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (isEnabled) googleGreen
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isEnabled) "Shield Active" else "Shield Paused",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (isEnabled && rulesCount > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = MaterialTheme.colorScheme.tertiaryContainer
                                    ) {
                                        Text(
                                            text = "${numberFormatter.format(rulesCount)} rules",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isEnabled) "Zero-latency on-device filtering (<0.1ms)" else "Tap to configure or enable shield",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Switch(
                        checked = isEnabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = googleGreen
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Protection Category Tags
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tags = listOf("Ads", "Trackers", "Malware", "Phishing", "Scams", "Whitelist")
                tags.forEach { tag ->
                    Surface(
                        shape = CircleShape,
                        color = if (isEnabled) MaterialTheme.colorScheme.surfaceContainerHigh
                        else MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onConfigure)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            if (isEnabled) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = googleGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = if (isEnabled) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
