package com.dnsly.app.service

import java.nio.ByteBuffer

object DnsPacketParser {

    data class ParsedDnsQuery(
        val transactionId: Short,
        val domain: String,
        val queryType: Int,
        val queryClass: Int,
        val dnsPayloadOffset: Int,
        val questionLength: Int
    )

    fun parseQuery(dnsPayload: ByteArray, offset: Int = 0, length: Int = dnsPayload.size): ParsedDnsQuery? {
        if (length < 12) return null
        val buffer = ByteBuffer.wrap(dnsPayload, offset, length)

        val id = buffer.short
        val flags = buffer.short.toInt() and 0xFFFF
        val isResponse = (flags and 0x8000) != 0
        if (isResponse) return null // We only inspect outbound client queries

        val qdCount = buffer.short.toInt() and 0xFFFF
        if (qdCount < 1) return null

        // Skip ANCOUNT, NSCOUNT, ARCOUNT
        buffer.short
        buffer.short
        buffer.short

        // Parse Question Name (series of length-prefixed labels)
        val domainBuilder = StringBuilder()
        var questionStart = buffer.position()
        var labelLen = buffer.get().toInt() and 0xFF

        while (labelLen > 0) {
            if (buffer.remaining() < labelLen) return null
            val labelBytes = ByteArray(labelLen)
            buffer.get(labelBytes)
            if (domainBuilder.isNotEmpty()) {
                domainBuilder.append('.')
            }
            domainBuilder.append(String(labelBytes, Charsets.US_ASCII))

            if (!buffer.hasRemaining()) return null
            labelLen = buffer.get().toInt() and 0xFF
        }

        if (buffer.remaining() < 4) return null
        val qType = buffer.short.toInt() and 0xFFFF
        val qClass = buffer.short.toInt() and 0xFFFF

        val questionLength = buffer.position() - questionStart

        return ParsedDnsQuery(
            transactionId = id,
            domain = domainBuilder.toString(),
            queryType = qType,
            queryClass = qClass,
            dnsPayloadOffset = offset,
            questionLength = questionLength
        )
    }

    /**
     * Builds a synthetic NXDOMAIN or 0.0.0.0 / :: sinkhole response to block a tracker/ad domain.
     */
    fun createBlockedResponse(
        dnsQueryPayload: ByteArray,
        queryOffset: Int = 0,
        queryLength: Int = dnsQueryPayload.size,
        queryType: Int = 1,
        asNxDomain: Boolean = false,
        ttlSeconds: Int = 60
    ): ByteArray {
        val queryBuffer = ByteBuffer.wrap(dnsQueryPayload, queryOffset, queryLength)
        val transactionId = queryBuffer.short
        val queryFlags = queryBuffer.short

        // Flags: QR=1 (Response), RD=1, RA=1, RCODE = 3 (NXDOMAIN) or 0 (NoError)
        val responseFlags: Short = if (asNxDomain) {
            (0x8183).toShort()
        } else {
            (0x8180).toShort()
        }

        val extraBytes = if (!asNxDomain) (if (queryType == 28) 28 else 16) else 0
        val out = ByteBuffer.allocate(queryLength + extraBytes + 16)
        out.putShort(transactionId)
        out.putShort(responseFlags)
        out.putShort(1.toShort()) // QDCOUNT: 1 question
        out.putShort(if (asNxDomain) 0.toShort() else 1.toShort()) // ANCOUNT
        out.putShort(0.toShort()) // NSCOUNT
        out.putShort(0.toShort()) // ARCOUNT

        // Copy question section verbatim
        val questionBytes = ByteArray(queryLength - 12)
        queryBuffer.position(12)
        queryBuffer.get(questionBytes)
        out.put(questionBytes)

        if (!asNxDomain) {
            out.putShort(0xC00C.toShort()) // Compression pointer to question name
            if (queryType == 28) {
                // Type AAAA (IPv6) ::
                out.putShort(28.toShort())     // Type AAAA
                out.putShort(1.toShort())      // Class IN
                out.putInt(ttlSeconds)         // TTL
                out.putShort(16.toShort())     // Data length 16
                for (i in 0 until 16) {
                    out.put(0.toByte())
                }
            } else {
                // Type A (IPv4) 0.0.0.0
                out.putShort(1.toShort())      // Type A
                out.putShort(1.toShort())      // Class IN
                out.putInt(ttlSeconds)         // TTL
                out.putShort(4.toShort())      // Data length 4
                out.put(0.toByte())
                out.put(0.toByte())
                out.put(0.toByte())
                out.put(0.toByte())
            }
        }

        val result = ByteArray(out.position())
        System.arraycopy(out.array(), 0, result, 0, result.size)
        return result
    }

    fun getTypeName(type: Int): String = when (type) {
        1 -> "A"
        28 -> "AAAA"
        5 -> "CNAME"
        15 -> "MX"
        16 -> "TXT"
        65 -> "HTTPS"
        else -> "TYPE $type"
    }

    /**
     * Inspects a raw DNS response payload from an upstream resolver to check if it returned
     * a blocked/sinkholed IP (e.g. 0.0.0.0, 127.0.0.1, ::, ::1) or NXDOMAIN from filtering DNS.
     */
    fun isSinkholedResponse(dnsPayload: ByteArray, offset: Int = 0, length: Int = dnsPayload.size): Boolean {
        if (length < 12) return false
        return try {
            val buffer = ByteBuffer.wrap(dnsPayload, offset, length)
            buffer.short // Transaction ID
            val flags = buffer.short.toInt() and 0xFFFF
            val isResponse = (flags and 0x8000) != 0
            if (!isResponse) return false

            val qdCount = buffer.short.toInt() and 0xFFFF
            val anCount = buffer.short.toInt() and 0xFFFF
            buffer.short // NSCOUNT
            buffer.short // ARCOUNT

            // Skip question section(s)
            for (i in 0 until qdCount) {
                if (!skipName(buffer)) return false
                if (buffer.remaining() < 4) return false
                buffer.short // QTYPE
                buffer.short // QCLASS
            }

            // Parse Answer section(s)
            for (i in 0 until anCount) {
                if (!skipName(buffer)) return false
                if (buffer.remaining() < 10) return false
                val type = buffer.short.toInt() and 0xFFFF
                buffer.short // CLASS
                buffer.int   // TTL
                val rdLength = buffer.short.toInt() and 0xFFFF
                if (buffer.remaining() < rdLength) return false

                if (type == 1 && rdLength == 4) { // IPv4 A record
                    val b0 = buffer.get().toInt() and 0xFF
                    val b1 = buffer.get().toInt() and 0xFF
                    val b2 = buffer.get().toInt() and 0xFF
                    val b3 = buffer.get().toInt() and 0xFF
                    // 0.0.0.0 or 127.0.0.1 sinkholes
                    if ((b0 == 0 && b1 == 0 && b2 == 0 && b3 == 0) ||
                        (b0 == 127 && b1 == 0 && b2 == 0 && b3 == 1)) {
                        return true
                    }
                } else if (type == 28 && rdLength == 16) { // IPv6 AAAA record
                    var allZero = true
                    var isLoopback = true
                    for (j in 0 until 16) {
                        val b = buffer.get()
                        if (b != 0.toByte()) allZero = false
                        if (j == 15 && b != 1.toByte()) isLoopback = false
                        if (j < 15 && b != 0.toByte()) isLoopback = false
                    }
                    if (allZero || isLoopback) {
                        return true
                    }
                } else {
                    // Skip other record types (CNAME, TXT, etc.)
                    buffer.position(buffer.position() + rdLength)
                }
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun skipName(buffer: ByteBuffer): Boolean {
        while (buffer.hasRemaining()) {
            val len = buffer.get().toInt() and 0xFF
            if (len == 0) return true
            if ((len and 0xC0) == 0xC0) {
                // Compression pointer: 2 bytes in total (we already read 1 byte, now skip the second)
                if (!buffer.hasRemaining()) return false
                buffer.get()
                return true
            }
            if (buffer.remaining() < len) return false
            buffer.position(buffer.position() + len)
        }
        return false
    }
}
