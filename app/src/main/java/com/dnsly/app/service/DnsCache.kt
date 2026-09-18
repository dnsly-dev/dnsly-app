package com.dnsly.app.service

import java.util.concurrent.ConcurrentHashMap

data class CachedDnsEntry(
    val payload: ByteArray,
    val expiresAt: Long
)

/**
 * Ultra-fast in-memory DNS cache with TTL expiration.
 * Resolves repeat queries (e.g. multi-socket CDN downloads in Speedtest) in <1ms without network roundtrips.
 */
object DnsCache {
    private const val MAX_ENTRIES = 1024
    private val cache = ConcurrentHashMap<String, CachedDnsEntry>()

    private fun makeKey(domain: String, qType: Int): String = "${domain.lowercase()}:$qType"

    /**
     * Looks up cached DNS response payload.
     * Returns a copy of the payload with TTL valid, or null if expired or not present.
     */
    fun get(domain: String, qType: Int): ByteArray? {
        val key = makeKey(domain, qType)
        val entry = cache[key] ?: return null
        if (System.currentTimeMillis() > entry.expiresAt) {
            cache.remove(key)
            return null
        }
        return entry.payload.copyOf()
    }

    /**
     * Caches response payload for domain and query type.
     * Only caches valid responses containing at least one answer record.
     */
    fun put(domain: String, qType: Int, payload: ByteArray, ttlSeconds: Long = 120) {
        if (payload.size < 12) return

        // Verify DNS Answer Count (bytes 6 and 7 in DNS Header)
        val anCount = ((payload[6].toInt() and 0xFF) shl 8) or (payload[7].toInt() and 0xFF)
        if (anCount <= 0) return // Do not cache empty or failed responses

        if (cache.size >= MAX_ENTRIES) {
            val now = System.currentTimeMillis()
            val expiredKeys = cache.filter { it.value.expiresAt <= now }.keys
            for (k in expiredKeys) {
                cache.remove(k)
            }
            if (cache.size >= MAX_ENTRIES) {
                cache.keys().toList().firstOrNull()?.let { cache.remove(it) }
            }
        }

        val clampedTtl = ttlSeconds.coerceIn(30, 300) * 1000L
        val entry = CachedDnsEntry(
            payload = payload.copyOf(),
            expiresAt = System.currentTimeMillis() + clampedTtl
        )
        cache[makeKey(domain, qType)] = entry
    }

    fun clear() {
        cache.clear()
    }
}
