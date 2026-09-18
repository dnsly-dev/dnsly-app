package com.dnsly.app.service

import java.util.Locale

object LocalAdBlocker {

    private val blockedDomains: Set<String> = setOf(
        // Common Ad Networks
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
        "adnxs.com",
        "criteo.com",
        "criteo.net",
        "outbrain.com",
        "taboola.com",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "casalemedia.com",
        "adcolony.com",
        "unityads.unity3d.com",
        "applovin.com",
        "applvn.com",
        "vungle.com",
        "ironsrc.com",
        "chartboost.com",
        "inmobi.com",
        "tapjoy.com",
        "mopub.com",
        "admob.com",
        "smartadserver.com",
        "adtechus.com",
        "adtech.de",
        "bidswitch.net",
        "indexexchange.com",
        "yieldmo.com",
        "sovrn.com",
        "lijit.com",
        "media.net",
        "popads.net",
        "propellerads.com",
        "exoclick.com",
        "trafficjunky.com",

        // Mobile Telemetry & User Trackers
        "google-analytics.com",
        "analytics.google.com",
        "hotjar.com",
        "segment.io",
        "segment.com",
        "mixpanel.com",
        "amplitude.com",
        "appsflyer.com",
        "adjust.com",
        "branch.io",
        "kochava.com",
        "singular.net",
        "scorecardresearch.com",
        "quantserve.com",
        "chartbeat.com",
        "newrelic.com",
        "flurry.com",
        "leanplum.com",
        "braze.com",
        "appboy.com",
        "onesignal.com",
        "telemetry.mozilla.org",
        "vortex.data.microsoft.com",
        "telemetry.microsoft.com",
        "browser.pipe.aria.microsoft.com",
        "metrics.icloud.com",
        "iadsdk.apple.com",

        // Malware, Cryptominers & Phishing sinks
        "coinhive.com",
        "coin-hive.com",
        "authedmine.com",
        "crypto-loot.com",
        "minr.pw"
    )

    fun isDomainBlocked(rawDomain: String): Boolean {
        if (rawDomain.isBlank()) return false
        val domain = rawDomain.trim().lowercase(Locale.US).removeSuffix(".")

        var current = domain
        while (current.isNotEmpty()) {
            if (blockedDomains.contains(current)) {
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
}
