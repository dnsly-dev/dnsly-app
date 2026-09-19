package com.dnsly.app.service.blocklist

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Locale

object BlocklistParser {

    private val ESSENTIAL_WHITELIST = hashSetOf(
        "connectivitycheck.gstatic.com",
        "connectivitycheck.android.com",
        "clients3.google.com",
        "captive.apple.com",
        "msftconnecttest.com",
        "dns.google",
        "one.one.one.one",
        "localhost",
        "local"
    )

    private val COMMON_TLDS = hashSetOf(
        "com", "org", "net", "edu", "gov", "mil", "int", "io", "co", "ai", "app", "dev",
        "info", "biz", "me", "tv", "cc", "site", "online", "store", "tech", "xyz", "top",
        "uk", "us", "ca", "de", "fr", "au", "in", "cn", "jp", "br", "ru", "eu", "nl", "se", "no"
    )

    /**
     * Parses domains from an input stream in streaming fashion.
     * Supports:
     * - Standard Hosts format (`0.0.0.0 domain.com`, `127.0.0.1 domain.com`)
     * - Plain domain lists (`domain.com`, `*.domain.com`)
     * - AdBlock / ABP DNS filter syntax (`||domain.com^`, `||domain.com^$dnsrewrite=...`)
     */
    fun parseStream(inputStream: InputStream, destination: MutableSet<String>) {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8), 32768)
        var line: String? = reader.readLine()
        while (line != null) {
            val domain = parseLine(line)
            if (domain != null && domain.isNotBlank()) {
                destination.add(domain)
            }
            line = reader.readLine()
        }
    }

    fun parseLine(rawLine: String): String? {
        val trimmed = rawLine.trim()
        if (trimmed.isEmpty()) return null

        // Skip comments and directives
        val firstChar = trimmed[0]
        if (firstChar == '#' || firstChar == '!' || firstChar == ';' || firstChar == '/' || firstChar == '[') {
            return null
        }

        // 1. AdBlock DNS filter format (e.g. "||ads.example.com^")
        if (trimmed.startsWith("||")) {
            var domainPart = trimmed.substring(2)
            val caretIndex = domainPart.indexOf('^')
            if (caretIndex != -1) {
                domainPart = domainPart.substring(0, caretIndex)
            }
            val slashIndex = domainPart.indexOf('/')
            if (slashIndex != -1) {
                domainPart = domainPart.substring(0, slashIndex)
            }
            val dollarIndex = domainPart.indexOf('$')
            if (dollarIndex != -1) {
                domainPart = domainPart.substring(0, dollarIndex)
            }
            val cleaned = cleanDomain(domainPart)
            return if (isValidDomain(cleaned)) cleaned else null
        }

        // 2. Hosts format (e.g. "0.0.0.0 ads.example.com" or "127.0.0.1 ads.example.com")
        val commentIndex = trimmed.indexOf('#')
        val lineWithoutComment = if (commentIndex != -1) trimmed.substring(0, commentIndex).trim() else trimmed
        if (lineWithoutComment.isEmpty()) return null

        val parts = lineWithoutComment.split(Regex("\\s+"))
        if (parts.size >= 2) {
            val ipPart = parts[0]
            if (ipPart == "0.0.0.0" || ipPart == "127.0.0.1" || ipPart == "::" || ipPart == "::1" || ipPart == "broadcasthost") {
                val candidate = cleanDomain(parts[1])
                return if (isValidDomain(candidate)) candidate else null
            }
        }

        // 3. Plain domain wordlist (e.g. "ads.example.com" or "*.ads.example.com")
        if (parts.size == 1) {
            val candidate = cleanDomain(parts[0])
            return if (isValidDomain(candidate)) candidate else null
        }

        return null
    }

    private fun cleanDomain(raw: String): String {
        return raw.trim()
            .lowercase(Locale.US)
            .removePrefix("*.")
            .removePrefix(".")
            .removeSuffix(".")
    }

    fun isValidDomain(domain: String): Boolean {
        if (domain.length < 3 || domain.length > 253) return false
        if (ESSENTIAL_WHITELIST.contains(domain)) return false

        // Check labels
        val labels = domain.split('.')
        if (labels.size < 2) return false // Must have at least name and TLD (e.g. "example.com")

        // Reject if any label is empty
        for (label in labels) {
            if (label.isEmpty() || label.length > 63) return false
            if (label.startsWith("-") || label.endsWith("-")) return false
        }

        val tld = labels.last()
        // TLD cannot be numeric (prevents 0.0.0.0, 127.0.0.1 IPs)
        if (tld.length < 2 || tld.any { it !in 'a'..'z' }) return false

        // Prevent blocking whole TLDs directly
        if (labels.size == 2 && COMMON_TLDS.contains(labels[0])) return false

        // Basic domain character check
        for (i in domain.indices) {
            val c = domain[i]
            if (!((c in 'a'..'z') || (c in '0'..'9') || c == '.' || c == '-' || c == '_')) {
                return false
            }
        }
        return true
    }
}
