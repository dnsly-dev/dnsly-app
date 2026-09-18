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
}
