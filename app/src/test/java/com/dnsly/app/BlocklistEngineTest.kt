package com.dnsly.app

import com.dnsly.app.service.blocklist.BlocklistEngine
import com.dnsly.app.service.blocklist.BlocklistParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class BlocklistEngineTest {

    @Test
    fun testBlocklistParserFormats() {
        // 1. Plain domain list
        assertEquals("doubleclick.net", BlocklistParser.parseLine("doubleclick.net"))
        assertEquals("adservice.google.com", BlocklistParser.parseLine("*.adservice.google.com"))

        // 2. Hosts format
        assertEquals("googleadservices.com", BlocklistParser.parseLine("0.0.0.0 googleadservices.com"))
        assertEquals("criteo.com", BlocklistParser.parseLine("127.0.0.1 criteo.com # Ad tracker"))
        assertEquals("outbrain.com", BlocklistParser.parseLine(":: outbrain.com"))
        assertNull(BlocklistParser.parseLine("127.0.0.1 localhost"))

        // 3. AdBlock ABP filter syntax
        assertEquals("taboola.com", BlocklistParser.parseLine("||taboola.com^"))
        assertEquals("analytics.tiktok.com", BlocklistParser.parseLine("||analytics.tiktok.com^\$important"))

        // 4. Comments & invalid lines
        assertNull(BlocklistParser.parseLine("# This is a comment"))
        assertNull(BlocklistParser.parseLine("! Another comment"))
        assertNull(BlocklistParser.parseLine(""))
    }

    @Test
    fun testBlocklistParserStream() {
        val sampleList = """
            # Header comments
            ! Another comment
            0.0.0.0 doubleclick.net
            127.0.0.1 criteo.com
            ||google-analytics.com^
            subdomain.ads.example.com
            *.vungle.com
        """.trimIndent()

        val destination = HashSet<String>()
        ByteArrayInputStream(sampleList.toByteArray(Charsets.UTF_8)).use { stream ->
            BlocklistParser.parseStream(stream, destination)
        }

        assertTrue(destination.contains("doubleclick.net"))
        assertTrue(destination.contains("criteo.com"))
        assertTrue(destination.contains("google-analytics.com"))
        assertTrue(destination.contains("subdomain.ads.example.com"))
        assertTrue(destination.contains("vungle.com"))
        assertEquals(5, destination.size)
    }

    @Test
    fun testHierarchicalSubdomainMatching() {
        val domains = setOf(
            "doubleclick.net",
            "googleadservices.com",
            "criteo.com"
        )
        BlocklistEngine.updateDomains(domains)

        // Exact match
        assertTrue(BlocklistEngine.isBlocked("doubleclick.net"))
        assertTrue(BlocklistEngine.isBlocked("googleadservices.com"))

        // Subdomain hierarchical match
        assertTrue(BlocklistEngine.isBlocked("ad.doubleclick.net"))
        assertTrue(BlocklistEngine.isBlocked("sub.deep.ad.doubleclick.net"))
        assertTrue(BlocklistEngine.isBlocked("static.criteo.com"))

        // Case insensitivity & trailing dots
        assertTrue(BlocklistEngine.isBlocked("DoubleClick.NET"))
        assertTrue(BlocklistEngine.isBlocked("ad.doubleclick.net."))

        // Allowed domains
        assertFalse(BlocklistEngine.isBlocked("google.com"))
        assertFalse(BlocklistEngine.isBlocked("notdoubleclick.net"))
        assertFalse(BlocklistEngine.isBlocked("wikipedia.org"))
        assertFalse(BlocklistEngine.isBlocked(""))
    }

    @Test
    fun testPerformanceBenchmark() {
        // Populate with 50,000 domains
        val largeSet = HashSet<String>(50000)
        for (i in 0 until 50000) {
            largeSet.add("tracker-$i.adnetwork$i.com")
        }
        largeSet.add("target-ad-domain.com")
        BlocklistEngine.updateDomains(largeSet)

        val startTime = System.nanoTime()
        for (i in 0 until 1000) {
            BlocklistEngine.isBlocked("sub.deep.target-ad-domain.com")
            BlocklistEngine.isBlocked("legitimate-site.org")
        }
        val elapsedNano = System.nanoTime() - startTime
        val avgMicros = (elapsedNano / 2000.0) / 1000.0

        assertTrue("Each lookup must take < 50 microseconds", avgMicros < 50.0)
    }
}
