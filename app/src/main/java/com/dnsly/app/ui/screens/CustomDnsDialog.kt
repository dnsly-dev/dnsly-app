package com.dnsly.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text(
                    text = "Add Custom DNS",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Pi-hole, AdGuard Home, or Private DNS",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
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
                    label = { Text("Server Name") },
                    placeholder = { Text("e.g. Home Pi-hole") },
                    leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null, tint = Color(0xFF94A3B8)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = customDialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = primaryIp,
                    onValueChange = { primaryIp = it },
                    label = { Text("Primary IPv4") },
                    placeholder = { Text("192.168.1.100 or 1.1.1.1") },
                    leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, tint = Color(0xFF94A3B8)) },
                    trailingIcon = {
                        if (isValidIpv4(primaryIp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Valid", tint = Color(0xFF10B981))
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = customDialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = secondaryIp,
                    onValueChange = { secondaryIp = it },
                    label = { Text("Secondary IPv4 (Optional)") },
                    placeholder = { Text("1.0.0.1") },
                    leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, tint = Color(0xFF94A3B8)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = customDialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = hostname,
                    onValueChange = { hostname = it },
                    label = { Text("DoT Hostname (Optional)") },
                    placeholder = { Text("dns.example.com") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF94A3B8)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = customDialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = dohUrl,
                    onValueChange = { dohUrl = it },
                    label = { Text("DoH URL (Optional)") },
                    placeholder = { Text("https://example.com/dns-query") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF94A3B8)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = customDialogTextFieldColors(),
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
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0F172A),
                    disabledContainerColor = Color(0xFFE2E8F0)
                )
            ) {
                Text("Save & Connect", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF64748B))
            }
        }
    )
}

@Composable
private fun customDialogTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF2563EB),
    unfocusedBorderColor = Color(0xFFE2E8F0),
    focusedContainerColor = Color(0xFFF8FAFC),
    unfocusedContainerColor = Color(0xFFF8FAFC),
    focusedLabelColor = Color(0xFF2563EB),
    unfocusedLabelColor = Color(0xFF64748B)
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
