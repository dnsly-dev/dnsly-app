package com.dnsly.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnsly.app.model.DnsServer

private enum class CustomDnsMode {
    NEXTDNS_ID,
    DOH_DOT,
    IPV4_RAW
}

@Composable
fun CustomDnsDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, primaryIp: String, secondaryIp: String, hostname: String, dohUrl: String) -> DnsServer
) {
    var mode by remember { mutableStateOf(CustomDnsMode.NEXTDNS_ID) }

    // NextDNS ID Mode
    var nextDnsId by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }

    // Advanced / Manual Fields
    var primaryIp by remember { mutableStateOf("") }
    var secondaryIp by remember { mutableStateOf("") }
    var hostname by remember { mutableStateOf("") }
    var dohUrl by remember { mutableStateOf("") }

    val cleanNextDnsId = nextDnsId.trim().lowercase()

    // Calculated derived values
    val effectiveName = when (mode) {
        CustomDnsMode.NEXTDNS_ID -> customName.ifBlank { if (cleanNextDnsId.isNotBlank()) "NextDNS ($cleanNextDnsId)" else "NextDNS Custom" }
        else -> customName.ifBlank { "Custom DNS Server" }
    }

    val effectivePrimaryIp = when (mode) {
        CustomDnsMode.NEXTDNS_ID -> "45.90.28.0"
        CustomDnsMode.DOH_DOT -> primaryIp.ifBlank { "45.90.28.0" }
        CustomDnsMode.IPV4_RAW -> primaryIp.trim()
    }

    val effectiveSecondaryIp = when (mode) {
        CustomDnsMode.NEXTDNS_ID -> "45.90.30.0"
        CustomDnsMode.DOH_DOT -> secondaryIp.ifBlank { "45.90.30.0" }
        CustomDnsMode.IPV4_RAW -> secondaryIp.trim()
    }

    val effectiveHostname = when (mode) {
        CustomDnsMode.NEXTDNS_ID -> if (cleanNextDnsId.isNotBlank()) "$cleanNextDnsId.dns.nextdns.io" else ""
        CustomDnsMode.DOH_DOT -> hostname.trim()
        CustomDnsMode.IPV4_RAW -> hostname.trim()
    }

    val effectiveDohUrl = when (mode) {
        CustomDnsMode.NEXTDNS_ID -> if (cleanNextDnsId.isNotBlank()) "https://dns.nextdns.io/$cleanNextDnsId" else ""
        CustomDnsMode.DOH_DOT -> dohUrl.trim()
        CustomDnsMode.IPV4_RAW -> dohUrl.trim()
    }

    val canSave = when (mode) {
        CustomDnsMode.NEXTDNS_ID -> cleanNextDnsId.length in 5..8 && cleanNextDnsId.all { it.isLetterOrDigit() }
        CustomDnsMode.DOH_DOT -> (effectiveDohUrl.startsWith("https://") || effectiveHostname.contains(".")) && effectiveName.isNotBlank()
        CustomDnsMode.IPV4_RAW -> isValidIpv4(effectivePrimaryIp) && effectiveName.isNotBlank()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Add Custom DNS",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "NextDNS, Pi-hole, or AdGuard Home",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                // ─── Setup Mode Selector Chips ───
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    FilterChip(
                        selected = mode == CustomDnsMode.NEXTDNS_ID,
                        onClick = { mode = CustomDnsMode.NEXTDNS_ID },
                        label = { Text("NextDNS ID", fontWeight = if (mode == CustomDnsMode.NEXTDNS_ID) FontWeight.SemiBold else FontWeight.Normal) },
                        leadingIcon = {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = null
                    )

                    FilterChip(
                        selected = mode == CustomDnsMode.DOH_DOT,
                        onClick = { mode = CustomDnsMode.DOH_DOT },
                        label = { Text("DoH / DoT", fontWeight = if (mode == CustomDnsMode.DOH_DOT) FontWeight.SemiBold else FontWeight.Normal) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = null
                    )

                    FilterChip(
                        selected = mode == CustomDnsMode.IPV4_RAW,
                        onClick = { mode = CustomDnsMode.IPV4_RAW },
                        label = { Text("IPv4 Direct", fontWeight = if (mode == CustomDnsMode.IPV4_RAW) FontWeight.SemiBold else FontWeight.Normal) },
                        leadingIcon = {
                            Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = null
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ─── Mode 1: NextDNS ID ───
                if (mode == CustomDnsMode.NEXTDNS_ID) {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "NextDNS Profile Configuration",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Enter your 6-character NextDNS Profile ID from my.nextdns.io/setup",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = nextDnsId,
                                onValueChange = { if (it.length <= 8) nextDnsId = it },
                                label = { Text("NextDNS Profile ID") },
                                placeholder = { Text("e.g. 28a9b1") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    if (cleanNextDnsId.length in 5..8 && cleanNextDnsId.all { it.isLetterOrDigit() }) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Valid",
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                },
                                singleLine = true,
                                shape = MaterialTheme.shapes.small,
                                colors = dialogTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = customName,
                                onValueChange = { customName = it },
                                label = { Text("Friendly Name (optional)") },
                                placeholder = { Text("e.g. My NextDNS") },
                                singleLine = true,
                                shape = MaterialTheme.shapes.small,
                                colors = dialogTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (cleanNextDnsId.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // NextDNS Live Resolved Preview Box
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Generated NextDNS Endpoints:",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• DoH: https://dns.nextdns.io/$cleanNextDnsId",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "• DoT: $cleanNextDnsId.dns.nextdns.io",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "• Anycast IPs: 45.90.28.0 / 45.90.30.0",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }

                // ─── Mode 2: DoH / DoT Encrypted URL ───
                else if (mode == CustomDnsMode.DOH_DOT) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Server Name") },
                        placeholder = { Text("e.g. My Private DoH") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        colors = dialogTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = dohUrl,
                        onValueChange = { dohUrl = it },
                        label = { Text("DNS-over-HTTPS (DoH) URL") },
                        placeholder = { Text("https://example.com/dns-query") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Http,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        colors = dialogTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = hostname,
                        onValueChange = { hostname = it },
                        label = { Text("DNS-over-TLS (DoT) Hostname (optional)") },
                        placeholder = { Text("dns.example.com") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        colors = dialogTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = primaryIp,
                        onValueChange = { primaryIp = it },
                        label = { Text("Bootstrap IPv4 (optional)") },
                        placeholder = { Text("1.1.1.1") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        colors = dialogTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ─── Mode 3: IPv4 Direct (Pi-hole / Router) ───
                else if (mode == CustomDnsMode.IPV4_RAW) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Server Name") },
                        placeholder = { Text("e.g. Home Pi-hole") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        colors = dialogTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = primaryIp,
                        onValueChange = { primaryIp = it },
                        label = { Text("Primary IPv4") },
                        placeholder = { Text("192.168.1.100") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (isValidIpv4(primaryIp)) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Valid",
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        colors = dialogTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = secondaryIp,
                        onValueChange = { secondaryIp = it },
                        label = { Text("Secondary IPv4 (optional)") },
                        placeholder = { Text("192.168.1.101") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        colors = dialogTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canSave) {
                        onSave(
                            effectiveName,
                            effectivePrimaryIp,
                            effectiveSecondaryIp,
                            effectiveHostname,
                            effectiveDohUrl
                        )
                        onDismiss()
                    }
                },
                enabled = canSave,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            ) {
                Text("Save & Connect", fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
private fun dialogTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
)

private fun isValidIpv4(ip: String): Boolean {
    val trimmed = ip.trim()
    val parts = trimmed.split(".")
    if (parts.size != 4) return false
    return parts.all {
        val num = it.toIntOrNull()
        num != null && num in 0..255 && (it == "0" || !it.startsWith("0"))
    }
}
