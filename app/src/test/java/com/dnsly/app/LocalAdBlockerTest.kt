package com.dnsly.app

import com.dnsly.app.service.LocalAdBlocker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAdBlockerTest {

    @Test
    fun testBlockedAdDomains() {
        assertTrue("doubleclick.net should be blocked", LocalAdBlocker.isDomainBlocked("doubleclick.net"))
        assertTrue("subdomain of doubleclick should be blocked", LocalAdBlocker.isDomainBlocked("ad.doubleclick.net"))
        assertTrue("deep subdomain should be blocked", LocalAdBlocker.isDomainBlocked("sub.ad.doubleclick.net"))
        assertTrue("googleadservices should be blocked", LocalAdBlocker.isDomainBlocked("googleadservices.com"))
        assertTrue("google-analytics should be blocked", LocalAdBlocker.isDomainBlocked("google-analytics.com"))
        assertTrue("criteo should be blocked", LocalAdBlocker.isDomainBlocked("static.criteo.com"))
        assertTrue("coinhive crypto miner should be blocked", LocalAdBlocker.isDomainBlocked("coinhive.com"))
    }

    @Test
    fun testAllowedDomains() {
        assertFalse("google.com should NOT be blocked", LocalAdBlocker.isDomainBlocked("google.com"))
        assertFalse("wikipedia.org should NOT be blocked", LocalAdBlocker.isDomainBlocked("wikipedia.org"))
        assertFalse("github.com should NOT be blocked", LocalAdBlocker.isDomainBlocked("github.com"))
        assertFalse("empty string should not crash or block", LocalAdBlocker.isDomainBlocked(""))
    }
}
