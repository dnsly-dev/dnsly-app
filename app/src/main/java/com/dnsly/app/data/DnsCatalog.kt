package com.dnsly.app.data

import com.dnsly.app.model.DnsServer
import com.dnsly.app.model.DnsType

object DnsCatalog {
    val defaultList: List<DnsServer> = listOf(
        DnsServer(
            id = "adguard_default",
            name = "AdGuard DNS Default",
            type = DnsType.FILTERED,
            categories = "Ads; trackers; malware/phishing",
            ipv4Primary = "94.140.14.14",
            ipv4Secondary = "94.140.15.15",
            hostname = "dns.adguard-dns.com",
            dohUrl = "https://dns.adguard-dns.com/dns-query",
            officialLink = "https://adguard-dns.io/en/public-dns.html"
        ),
        DnsServer(
            id = "adguard_family",
            name = "AdGuard DNS Family",
            type = DnsType.FAMILY,
            categories = "Ads; trackers; adult; SafeSearch",
            ipv4Primary = "94.140.14.15",
            ipv4Secondary = "94.140.15.16",
            hostname = "family.adguard-dns.com",
            dohUrl = "https://family.adguard-dns.com/dns-query",
            officialLink = "https://adguard-dns.io/en/public-dns.html"
        ),
        DnsServer(
            id = "quad9_security",
            name = "Quad9",
            type = DnsType.SECURITY,
            categories = "Phishing; malware; spyware",
            ipv4Primary = "9.9.9.9",
            ipv4Secondary = "149.112.112.112",
            hostname = "dns.quad9.net",
            dohUrl = "https://dns.quad9.net/dns-query",
            officialLink = "https://www.quad9.net/service/service-addresses-and-features"
        ),
        DnsServer(
            id = "cloudflare_security",
            name = "Cloudflare Security",
            type = DnsType.SECURITY,
            categories = "Malware",
            ipv4Primary = "1.1.1.2",
            ipv4Secondary = "1.0.0.2",
            hostname = "security.cloudflare-dns.com",
            dohUrl = "https://security.cloudflare-dns.com/dns-query",
            officialLink = "https://developers.cloudflare.com/1.1.1.1/setup/"
        ),
        DnsServer(
            id = "cloudflare_family",
            name = "Cloudflare Family",
            type = DnsType.FAMILY,
            categories = "Malware; adult",
            ipv4Primary = "1.1.1.3",
            ipv4Secondary = "1.0.0.3",
            hostname = "family.cloudflare-dns.com",
            dohUrl = "https://family.cloudflare-dns.com/dns-query",
            officialLink = "https://developers.cloudflare.com/1.1.1.1/setup/"
        ),
        DnsServer(
            id = "cleanbrowsing_family",
            name = "CleanBrowsing Family",
            type = DnsType.FAMILY,
            categories = "Adult; explicit; malicious/phishing; proxy/VPN domains",
            ipv4Primary = "185.228.168.168",
            ipv4Secondary = "185.228.169.168",
            hostname = "family-filter-dns.cleanbrowsing.org",
            dohUrl = "https://doh.cleanbrowsing.org/doh/family-filter/",
            officialLink = "https://cleanbrowsing.org/filters"
        ),
        DnsServer(
            id = "controld_ads",
            name = "Control D Ads & Tracking",
            type = DnsType.PRIVACY,
            categories = "Ads; trackers",
            ipv4Primary = "76.76.2.2",
            ipv4Secondary = "76.76.10.2",
            hostname = "p2.freedns.controld.com",
            dohUrl = "https://freedns.controld.com/p2",
            officialLink = "https://docs.controld.com/docs/free-dns"
        ),
        DnsServer(
            id = "controld_family",
            name = "Control D Family",
            type = DnsType.FAMILY,
            categories = "Ads; adult; drugs; malware",
            ipv4Primary = "76.76.2.4",
            ipv4Secondary = "76.76.10.4",
            hostname = "family.freedns.controld.com",
            dohUrl = "https://freedns.controld.com/family",
            officialLink = "https://docs.controld.com/docs/free-dns"
        ),
        DnsServer(
            id = "rethink_dns",
            name = "RethinkDNS",
            type = DnsType.CONFIGURABLE,
            categories = "Configurable blocklists; privacy",
            ipv4Primary = "104.21.3.155",
            ipv4Secondary = "172.67.147.240",
            hostname = "basic.rethinkdns.com",
            dohUrl = "https://basic.rethinkdns.com/dns-query",
            officialLink = "https://rethinkdns.com/"
        ),
        DnsServer(
            id = "opendns_familyshield",
            name = "OpenDNS FamilyShield",
            type = DnsType.FAMILY,
            categories = "Adult content",
            ipv4Primary = "208.67.222.123",
            ipv4Secondary = "208.67.220.123",
            hostname = "doh.familyshield.opendns.com",
            dohUrl = "https://doh.familyshield.opendns.com/dns-query",
            officialLink = "https://www.opendns.com/setupguide/"
        ),
        DnsServer(
            id = "google_public",
            name = "Google Public DNS",
            type = DnsType.NON_FILTERING,
            categories = "Neutral recursive DNS; DNSSEC",
            ipv4Primary = "8.8.8.8",
            ipv4Secondary = "8.8.4.4",
            hostname = "dns.google",
            dohUrl = "https://dns.google/dns-query",
            officialLink = "https://developers.google.com/speed/public-dns"
        ),
        DnsServer(
            id = "dns_watch",
            name = "DNS.WATCH",
            type = DnsType.PRIVACY,
            categories = "Non-filtering; privacy-focused",
            ipv4Primary = "84.200.69.80",
            ipv4Secondary = "84.200.70.40",
            hostname = "resolver1.dns.watch",
            dohUrl = "",
            officialLink = "https://dns.watch/"
        ),
        DnsServer(
            id = "switch_dns",
            name = "SWITCH DNS",
            type = DnsType.PRIVACY,
            categories = "Non-filtering; DNSSEC",
            ipv4Primary = "130.59.31.248",
            ipv4Secondary = "130.59.31.249",
            hostname = "dns.switch.ch",
            dohUrl = "https://dns.switch.ch/dns-query",
            officialLink = "https://www.switch.ch/security/"
        ),
        DnsServer(
            id = "hurricane_electric",
            name = "Hurricane Electric",
            type = DnsType.NON_FILTERING,
            categories = "Neutral recursive DNS; anycast",
            ipv4Primary = "74.82.42.42",
            ipv4Secondary = "",
            hostname = "ordns.he.net",
            dohUrl = "",
            officialLink = "https://dns.he.net/"
        ),
        DnsServer(
            id = "quad101",
            name = "Quad101",
            type = DnsType.PRIVACY,
            categories = "Regional/Privacy; non-filtering; no logging",
            ipv4Primary = "101.101.101.101",
            ipv4Secondary = "101.102.103.104",
            hostname = "101.101.101.101",
            dohUrl = "https://dns.twnic.tw/dns-query",
            officialLink = "https://quad101.twnic.tw/"
        ),
        DnsServer(
            id = "safesurfer",
            name = "Safe Surfer",
            type = DnsType.FAMILY,
            categories = "Porn; ads; malware; social categories",
            ipv4Primary = "104.155.237.225",
            ipv4Secondary = "104.197.28.121",
            hostname = "",
            dohUrl = "",
            officialLink = "https://www.safesurfer.co.nz/"
        ),
        DnsServer(
            id = "nawala_child",
            name = "Nawala Child Protection",
            type = DnsType.FAMILY,
            categories = "Adult; inappropriate/abusive content",
            ipv4Primary = "180.131.144.144",
            ipv4Secondary = "180.131.145.145",
            hostname = "",
            dohUrl = "",
            officialLink = "https://nawala.id/"
        ),
        DnsServer(
            id = "safedns",
            name = "SafeDNS",
            type = DnsType.SECURITY,
            categories = "Content filtering; security",
            ipv4Primary = "195.46.39.39",
            ipv4Secondary = "195.46.39.40",
            hostname = "",
            dohUrl = "",
            officialLink = "https://www.safedns.com/"
        ),
        DnsServer(
            id = "cloudflare_default",
            name = "Cloudflare Standard",
            type = DnsType.PRIVACY,
            categories = "High speed; privacy-first; no logging",
            ipv4Primary = "1.1.1.1",
            ipv4Secondary = "1.0.0.1",
            hostname = "one.one.one.one",
            dohUrl = "https://cloudflare-dns.com/dns-query",
            officialLink = "https://1.1.1.1/"
        ),
        DnsServer(
            id = "mullvad_adblock",
            name = "Mullvad DNS Ad-Block",
            type = DnsType.FILTERED,
            categories = "Ads; tracking; malware; privacy",
            ipv4Primary = "194.242.2.3",
            ipv4Secondary = "194.242.2.4",
            hostname = "adblock.dns.mullvad.net",
            dohUrl = "https://adblock.dns.mullvad.net/dns-query",
            officialLink = "https://mullvad.net/help/dns-over-https-and-dns-over-tls"
        )
    )
}
