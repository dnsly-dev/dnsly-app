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
     * Builds a synthetic NXDOMAIN or 0.0.0.0 response to block a tracker/ad domain.
     */
    fun createBlockedResponse(
        dnsQueryPayload: ByteArray,
        queryOffset: Int,
        queryLength: Int,
        asNxDomain: Boolean = true
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

        val out = ByteBuffer.allocate(queryLength + 16)
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
            // Add A record with 0.0.0.0 (TTL 300)
            out.putShort(0xC00C.toShort()) // Compression pointer to question name
            out.putShort(1.toShort())      // Type A
            out.putShort(1.toShort())      // Class IN
            out.putInt(300)                // TTL 300s
            out.putShort(4.toShort())      // Data length 4
            out.put(0.toByte())
            out.put(0.toByte())
            out.put(0.toByte())
            out.put(0.toByte())
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
}
