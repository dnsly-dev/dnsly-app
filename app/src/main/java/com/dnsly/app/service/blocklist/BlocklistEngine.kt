package com.dnsly.app.service.blocklist

import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

/**
 * Ultra-fast in-memory blocklist lookup engine.
 * Provides lock-free, sub-microsecond hierarchical suffix matching (e.g. sub.ad.doubleclick.net -> doubleclick.net).
 */
object BlocklistEngine {

    private val blockedSetRef = AtomicReference<Set<String>>(emptySet())

    /**
     * Replaces the active blocklist set atomically.
     */
    fun updateDomains(domains: Set<String>) {
        blockedSetRef.set(domains)
    }

    /**
     * Gets the total count of loaded blocked domains.
     */
    val domainCount: Int
        get() = blockedSetRef.get().size

    /**
     * Checks if a domain or any of its parent suffixes is present in the blocklist.
     * Takes < 1 microsecond.
     */
    fun isBlocked(rawDomain: String): Boolean {
        if (rawDomain.isBlank()) return false
        val set = blockedSetRef.get()
        if (set.isEmpty()) return false

        val domain = rawDomain.trim().lowercase(Locale.US).removeSuffix(".")

        var current = domain
        while (current.isNotEmpty()) {
            if (set.contains(current)) {
                return true
            }
            val dotIndex = current.indexOf('.')
            if (dotIndex == -1 || dotIndex == current.length - 1) {
                break
            }
            current = current.substring(dotIndex + 1)
        }
        return false
    }

    /**
     * Clears all domains from the engine.
     */
    fun clear() {
        blockedSetRef.set(emptySet())
    }
}
