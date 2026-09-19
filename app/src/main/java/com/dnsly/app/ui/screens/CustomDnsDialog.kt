package com.dnsly.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnsly.app.model.DnsServer

@Composable
fun CustomDnsDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, primaryIp: String, secondaryIp: String, hostname: String, dohUrl: String) -> DnsServer
) {
    var name by remember { mutableStateOf("") }
    var dohUrl by remember { mutableStateOf("") }
    var dotHostname by remember { mutableStateOf("") }
    var primaryIp by remember { mutableStateOf("") }
    var secondaryIp by remember { mutableStateOf("") }

    val cleanName = name.trim()
    val cleanDoh = dohUrl.trim()
    val cleanDot = dotHostname.trim()
    val cleanPrimary = primaryIp.trim()
    val cleanSecondary = secondaryIp.trim()

    val hasEncrypted = cleanDoh.isNotBlank() || cleanDot.isNotBlank()
    val hasValidIpv4 = isValidIpv4(cleanPrimary)
    val canSave = cleanName.isNotBlank() && (hasEncrypted || hasValidIpv4)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Add Custom DNS",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Setup your custom DNS resolver",
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
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ─── NextDNS-Style Card 1: Server Identification ───
                NextDnsSectionCard(
                    icon = Icons.Default.Dns,
                    title = "Identification",
                    subtitle = "Name and label your resolver"
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Server Name") },
                        placeholder = { Text("e.g. My NextDNS / Pi-hole / AdGuard") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = nextDnsFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ─── NextDNS-Style Card 2: Encrypted Endpoints (DoH / DoT) ───
                NextDnsSectionCard(
                    icon = Icons.Default.Lock,
                    title = "Endpoints",
                    subtitle = "Encrypted protocols (DoH & DoT)",
                    badge = "Encrypted"
                ) {
                    OutlinedTextField(
                        value = dohUrl,
                        onValueChange = { dohUrl = it },
                        label = { Text("DNS-over-HTTPS (DoH)") },
                        placeholder = { Text("https://dns.nextdns.io/xxxxxx") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Http,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (cleanDoh.startsWith("https://")) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Valid DoH",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = nextDnsFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = dotHostname,
                        onValueChange = { dotHostname = it },
                        label = { Text("DNS-over-TLS (DoT) Hostname") },
                        placeholder = { Text("xxxxxx.dns.nextdns.io") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (cleanDot.contains(".") && !cleanDot.contains("/")) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Valid DoT",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = nextDnsFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ─── NextDNS-Style Card 3: IPv4 Addresses (Anycast / Direct) ───
                NextDnsSectionCard(
                    icon = Icons.Default.Public,
                    title = "IPv4 Anycast & Bootstrap",
                    subtitle = "Direct IP addresses for standard lookup"
                ) {
                    OutlinedTextField(
                        value = primaryIp,
                        onValueChange = { primaryIp = it },
                        label = { Text("Primary IPv4") },
                        placeholder = { Text("45.90.28.0") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (isValidIpv4(cleanPrimary)) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Valid IPv4",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = nextDnsFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = secondaryIp,
                        onValueChange = { secondaryIp = it },
                        label = { Text("Secondary IPv4 (Optional)") },
                        placeholder = { Text("45.90.30.0") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (isValidIpv4(cleanSecondary)) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Valid IPv4",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = nextDnsFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Helper tip box matching NextDNS layout style
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Provide either a DoH/DoT encrypted endpoint or at least one valid IPv4 address.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canSave) {
                        onSave(
                            cleanName,
                            cleanPrimary.ifBlank { "45.90.28.0" },
                            cleanSecondary.ifBlank { "45.90.30.0" },
                            cleanDot,
                            cleanDoh
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
                Text("Save Server", fontWeight = FontWeight.SemiBold)
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
private fun NextDnsSectionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                if (badge != null) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, bottom = 12.dp)
            )

            content()
        }
    }
}

@Composable
private fun nextDnsFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
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
