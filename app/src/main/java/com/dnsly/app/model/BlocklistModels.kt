package com.dnsly.app.model

enum class BlocklistCategory(val id: String, val displayName: String) {
    ADS("ads", "Advertising"),
    TRACKING("tracking", "Tracking & Telemetry"),
    SECURITY("security", "Security & Threats"),
    SCAMS("scams", "Scams & Suspicious"),
    CONTENT("content", "Content & Safety")
}

data class BlocklistPreset(
    val id: String,
    val name: String,
    val description: String,
    val url: String,
    val category: BlocklistCategory,
    val isEnabled: Boolean,
    val domainCount: Int = 0,
    val lastUpdated: Long = 0L
)

data class CustomBlocklistUrl(
    val id: String,
    val name: String,
    val url: String,
    val isEnabled: Boolean = true,
    val domainCount: Int = 0
)
