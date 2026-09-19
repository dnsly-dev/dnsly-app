package com.dnsly.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dnsly.app.model.DnsServer

@Composable
fun CustomDnsDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, primaryIp: String, secondaryIp: String, hostname: String, dohUrl: String) -> DnsServer
) {
    var name by remember { mutableStateOf("") }
    var primaryIp by remember { mutableStateOf("") }
    var secondaryIp by remember { mutableStateOf("") }
    var hostname by remember { mutableStateOf("") }
    var dohUrl by remember { mutableStateOf("") }

    val isIpValid = remember(primaryIp, hostname) {
        isValidIpv4(primaryIp) || hostname.trim().contains(".")
    }

    val canSave = name.isNotBlank() && isIpValid

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Column {
                Text(
                    text = "Add custom DNS",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pi-hole, AdGuard Home, or private DNS",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Server name") },
                    placeholder = { Text("e.g. Home Pi-hole") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Dns,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    colors = dialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

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

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = secondaryIp,
                    onValueChange = { secondaryIp = it },
                    label = { Text("Secondary IPv4 (optional)") },
                    placeholder = { Text("1.0.0.1") },
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

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = hostname,
                    onValueChange = { hostname = it },
                    label = { Text("DoT hostname (optional)") },
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

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = dohUrl,
                    onValueChange = { dohUrl = it },
                    label = { Text("DoH URL (optional)") },
                    placeholder = { Text("https://example.com/dns-query") },
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canSave) {
                        onSave(name, primaryIp, secondaryIp, hostname, dohUrl)
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
                Text("Save & connect", fontWeight = FontWeight.Medium)
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
