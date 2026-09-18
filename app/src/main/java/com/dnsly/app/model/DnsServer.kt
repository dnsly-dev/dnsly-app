package com.dnsly.app.model

import androidx.compose.ui.graphics.Color

enum class DnsType(val displayName: String) {
    FILTERED("Filtered"),
    FAMILY("Family"),
    SECURITY("Security"),
    PRIVACY("Privacy"),
    CONFIGURABLE("Configurable"),
    NON_FILTERING("Non-filtering"),
    CUSTOM("Custom")
}

data class DnsServer(
    val id: String,
    val name: String,
    val type: DnsType,
    val categories: String,
    val ipv4Primary: String,
    val ipv4Secondary: String = "",
    val hostname: String = "",
    val dohUrl: String = "",
    val officialLink: String = "",
    val isCustom: Boolean = false,
    val latencyMs: Int? = null,
    val isEnabled: Boolean = true
) {
    val displayIp: String
        get() = when {
            ipv4Primary.isNotBlank() && ipv4Secondary.isNotBlank() -> "$ipv4Primary • $ipv4Secondary"
            ipv4Primary.isNotBlank() -> ipv4Primary
            hostname.isNotBlank() -> hostname
            else -> "N/A"
        }

    val initials: String
        get() = when {
            name.startsWith("AdGuard", ignoreCase = true) -> "AG"
            name.startsWith("Cloudflare", ignoreCase = true) -> "CF"
            name.startsWith("Quad9", ignoreCase = true) -> "Q9"
            name.startsWith("Control D", ignoreCase = true) -> "CD"
            name.startsWith("CleanBrowsing", ignoreCase = true) -> "CB"
            name.startsWith("Rethink", ignoreCase = true) -> "RD"
            name.startsWith("OpenDNS", ignoreCase = true) -> "OD"
            name.startsWith("Google", ignoreCase = true) -> "G"
            name.startsWith("Mullvad", ignoreCase = true) -> "MV"
            name.startsWith("SWITCH", ignoreCase = true) -> "SW"
            name.startsWith("DNS.WATCH", ignoreCase = true) -> "DW"
            name.startsWith("Hurricane", ignoreCase = true) -> "HE"
            name.startsWith("Quad101", ignoreCase = true) -> "Q1"
            name.startsWith("Safe Surfer", ignoreCase = true) -> "SS"
            name.startsWith("Nawala", ignoreCase = true) -> "NW"
            name.startsWith("SafeDNS", ignoreCase = true) -> "SD"
            isCustom -> "CU"
            else -> name.take(2).uppercase()
        }

    val brandColor: Color
        get() = when {
            name.startsWith("AdGuard", ignoreCase = true) -> Color(0xFF10B981) // Emerald
            name.startsWith("Cloudflare", ignoreCase = true) -> Color(0xFFF97316) // Orange
            name.startsWith("Quad9", ignoreCase = true) -> Color(0xFF6366F1) // Indigo
            name.startsWith("Control D", ignoreCase = true) -> Color(0xFF06B6D4) // Cyan
            name.startsWith("Google", ignoreCase = true) -> Color(0xFF3B82F6) // Blue
            name.startsWith("CleanBrowsing", ignoreCase = true) -> Color(0xFF8B5CF6) // Purple
            name.startsWith("Mullvad", ignoreCase = true) -> Color(0xFFF59E0B) // Amber
            name.startsWith("Rethink", ignoreCase = true) -> Color(0xFFEC4899) // Pink
            isCustom -> Color(0xFF2563EB) // Royal Blue
            else -> Color(0xFF64748B) // Slate
        }
}
