package com.dnsly.app.model

data class QueryLog(
    val id: Long = System.currentTimeMillis() + (0..999).random(),
    val domain: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isBlocked: Boolean = false,
    val queryType: String = "A",
    val upstreamServer: String = "",
    val latencyMs: Long = 0L,
    val reason: String = if (isBlocked) "Ad / Telemetry Filter" else "Resolved",
    val protocol: String = if (isBlocked) "Local Filter" else "DoH"
)
