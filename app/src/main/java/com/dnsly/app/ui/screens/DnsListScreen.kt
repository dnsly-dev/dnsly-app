package com.dnsly.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dnsly.app.data.DnsRepository
import com.dnsly.app.model.DnsServer
import com.dnsly.app.model.DnsType
import com.dnsly.app.ui.components.DnsCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsListScreen(
    repository: DnsRepository,
    onBack: () -> Unit,
    onServerSelected: (DnsServer) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val servers by repository.servers.collectAsState()
    val selectedServer by repository.selectedServer.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<DnsType?>(null) }
    var showCustomDialog by remember { mutableStateOf(false) }

    val filteredServers = remember(servers, searchQuery, selectedTypeFilter) {
        servers.filter { server ->
            val matchesQuery = searchQuery.isBlank() ||
                    server.name.contains(searchQuery, ignoreCase = true) ||
                    server.ipv4Primary.contains(searchQuery, ignoreCase = true) ||
                    server.categories.contains(searchQuery, ignoreCase = true) ||
                    server.hostname.contains(searchQuery, ignoreCase = true)

            val matchesType = selectedTypeFilter == null || server.type == selectedTypeFilter

            matchesQuery && matchesType
        }
    }

    if (showCustomDialog) {
        CustomDnsDialog(
            onDismiss = { showCustomDialog = false },
            onSave = { name, pIp, sIp, host, doh ->
                val newServer = repository.addCustomServer(name, pIp, sIp, host, doh)
                onServerSelected(newServer)
                newServer
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "DNS providers",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCustomDialog = true },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Custom DNS")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            )

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTypeFilter == null,
                    onClick = { selectedTypeFilter = null },
                    label = { Text("All (${servers.size})") },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )

                DnsType.entries.forEach { type ->
                    val count = servers.count { it.type == type }
                    if (count > 0) {
                        FilterChip(
                            selected = selectedTypeFilter == type,
                            onClick = {
                                selectedTypeFilter = if (selectedTypeFilter == type) null else type
                            },
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

            Spacer(modifier = Modifier.height(4.dp))

            // DNS Server List
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredServers, key = { it.id }) { server ->
                    DnsCard(
                        server = server,
                        isSelected = server.id == selectedServer.id,
                        onSelect = {
                            repository.selectServer(server)
                            onServerSelected(server)
                            Toast.makeText(context, "Selected ${server.name}", Toast.LENGTH_SHORT).show()
                        },
                        onCopyHostname = { host ->
                            clipboardManager.setText(AnnotatedString(host))
                            Toast.makeText(context, "Copied: $host", Toast.LENGTH_SHORT).show()
                        },
                        onOpenUrl = { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDelete = if (server.isCustom) {
                            {
                                repository.removeCustomServer(server.id)
                                Toast.makeText(context, "Deleted ${server.name}", Toast.LENGTH_SHORT).show()
                            }
                        } else null
                    )
                }
            }
        }
    }
}
