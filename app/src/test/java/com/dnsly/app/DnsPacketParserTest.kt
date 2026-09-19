package com.dnsly.app

import com.dnsly.app.service.DnsPacketParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer

class DnsPacketParserTest {

    @Test
    fun testParseStandardDnsQuery() {
        // Construct raw DNS query for "google.com" Type A
        val queryBytes = byteArrayOf(
            0x4A.toByte(), 0x2F.toByte(), // ID: 0x4A2F
            0x01, 0x00,                   // Standard query with RD=1
            0x00, 0x01,                   // 1 Question
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x06, 0x67, 0x6f, 0x6f, 0x67, 0x6c, 0x65, // "google"
            0x03, 0x63, 0x6f, 0x6d,                   // "com"
            0x00,                                     // root
            0x00, 0x01,                               // Type A
            0x00, 0x01                                // Class IN
        )

        val parsed = DnsPacketParser.parseQuery(queryBytes)
        assertNotNull(parsed)
        assertEquals("google.com", parsed?.domain)
        assertEquals(1, parsed?.queryType)
        assertEquals("A", DnsPacketParser.getTypeName(parsed!!.queryType))
    }

    @Test
    fun testCreateBlockedResponse() {
        val queryBytes = byteArrayOf(
            0x12, 0x34,
            0x01, 0x00,
            0x00, 0x01,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x07, 0x61, 0x64, 0x6e, 0x78, 0x73, 0x2e, 0x63, 0x6f, 0x6d, // "adnxs.com"
            0x00,
            0x00, 0x01,
            0x00, 0x01
        )

        val blockedResp = DnsPacketParser.createBlockedResponse(queryBytes, 0, queryBytes.size, asNxDomain = true)
        val buf = ByteBuffer.wrap(blockedResp)
        assertEquals(0x1234.toShort(), buf.short) // Same transaction ID
        val flags = buf.short.toInt() and 0xFFFF
        assertTrue("Must have QR=1 response flag", (flags and 0x8000) != 0)
        assertEquals(3, flags and 0x000F) // NXDOMAIN RCODE = 3
    }

    @Test
    fun testIsSinkholedResponse() {
        // Construct DNS response containing 0.0.0.0 (AdGuard/NextDNS sinkhole)
        val sinkholeResp = byteArrayOf(
            0x12, 0x34,                   // ID
            0x81.toByte(), 0x80.toByte(), // QR=1, RD=1, RA=1, RCODE=0
            0x00, 0x01,                   // QDCOUNT: 1
            0x00, 0x01,                   // ANCOUNT: 1
            0x00, 0x00,                   // NSCOUNT: 0
            0x00, 0x00,                   // ARCOUNT: 0
            // Question: "ads.com"
            0x03, 0x61, 0x64, 0x73,
            0x03, 0x63, 0x6f, 0x6d,
            0x00,
            0x00, 0x01, // Type A
            0x00, 0x01, // Class IN
            // Answer:
            0xC0.toByte(), 0x0C.toByte(), // Compression pointer to question
            0x00, 0x01,                   // Type A
            0x00, 0x01,                   // Class IN
            0x00, 0x00, 0x00, 0x3C,       // TTL 60
            0x00, 0x04,                   // RDLENGTH 4
            0x00, 0x00, 0x00, 0x00        // 0.0.0.0 (Sinkholed IP)
        )

        assertTrue(DnsPacketParser.isSinkholedResponse(sinkholeResp))

        // Normal response with real IP (142.250.190.46)
        val normalResp = byteArrayOf(
            0x12, 0x34,
            0x81.toByte(), 0x80.toByte(),
            0x00, 0x01,
            0x00, 0x01,
            0x00, 0x00,
            0x00, 0x00,
            0x06, 0x67, 0x6f, 0x6f, 0x67, 0x6c, 0x65,
            0x03, 0x63, 0x6f, 0x6d,
            0x00,
            0x00, 0x01,
            0x00, 0x01,
            0xC0.toByte(), 0x0C.toByte(),
            0x00, 0x01,
            0x00, 0x01,
            0x00, 0x00, 0x01, 0x2C,
            0x00, 0x04,
            142.toByte(), 250.toByte(), 190.toByte(), 46.toByte()
        )

        org.junit.Assert.assertFalse(DnsPacketParser.isSinkholedResponse(normalResp))
    }
}
