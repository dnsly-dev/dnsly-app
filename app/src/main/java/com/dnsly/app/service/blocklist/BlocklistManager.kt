package com.dnsly.app.service.blocklist

import android.content.Context
import com.dnsly.app.model.BlocklistCategory
import com.dnsly.app.model.BlocklistPreset
import com.dnsly.app.model.CustomBlocklistUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class BlocklistManager private constructor(private val context: Context) {

    private val prefs = context.getSharedPreferences("dnsly_blocklist_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _isShieldEnabled = MutableStateFlow(prefs.getBoolean("shield_enabled", true))
    val isShieldEnabled: StateFlow<Boolean> = _isShieldEnabled.asStateFlow()

    private val _presets = MutableStateFlow<List<BlocklistPreset>>(loadPresets())
    val presets: StateFlow<List<BlocklistPreset>> = _presets.asStateFlow()

    private val _customUrls = MutableStateFlow<List<CustomBlocklistUrl>>(loadCustomUrls())
    val customUrls: StateFlow<List<CustomBlocklistUrl>> = _customUrls.asStateFlow()

    private val _whitelist = MutableStateFlow<Set<String>>(loadWhitelist())
    val whitelist: StateFlow<Set<String>> = _whitelist.asStateFlow()

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    private val _totalBlockedCount = MutableStateFlow(0)
    val totalBlockedCount: StateFlow<Int> = _totalBlockedCount.asStateFlow()

    init {
        scope.launch {
            initStarterCacheIfNeeded()
            recompileBlocklist()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: BlocklistManager? = null

        fun getInstance(context: Context): BlocklistManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BlocklistManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        val DEFAULT_PRESETS = listOf(
            // ─── 1. Advertising ───
            BlocklistPreset(
                id = "adaway",
                name = "AdAway Official",
                description = "Gold standard mobile & in-app ad blocking",
                url = "https://adaway.org/hosts.txt",
                category = BlocklistCategory.ADS,
                isEnabled = true
            ),
            BlocklistPreset(
                id = "adguard_dns",
                name = "AdGuard DNS Filter",
                description = "Curated DNS-level ad blocking rules",
                url = "https://v.firebog.net/hosts/AdguardDNS.txt",
                category = BlocklistCategory.ADS,
                isEnabled = false
            ),
            BlocklistPreset(
                id = "peter_lowe",
                name = "Peter Lowe's List (Yoyo)",
                description = "Zero false positive curated ad servers",
                url = "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=hosts&showintro=0&mimetype=plaintext",
                category = BlocklistCategory.ADS,
                isEnabled = false
            ),
            BlocklistPreset(
                id = "easylist",
                name = "EasyList DNS",
                description = "Standard web advertising blocklist",
                url = "https://v.firebog.net/hosts/Easylist.txt",
                category = BlocklistCategory.ADS,
                isEnabled = false
            ),
            BlocklistPreset(
                id = "hagezi_popups",
                name = "HaGeZi Pop-Up Ads",
                description = "Intrusive pop-ups, redirects & push spam",
                url = "https://cdn.jsdelivr.net/gh/hagezi/dns-blocklists@latest/wildcard/popupads-onlydomains.txt",
                category = BlocklistCategory.ADS,
                isEnabled = false
            ),

            // ─── 2. Tracking & Telemetry ───
            BlocklistPreset(
                id = "easyprivacy",
                name = "EasyPrivacy DNS",
                description = "Global standard user tracking & analytics shield",
                url = "https://v.firebog.net/hosts/Easyprivacy.txt",
                category = BlocklistCategory.TRACKING,
                isEnabled = false
            ),
            BlocklistPreset(
                id = "windows_spy",
                name = "Windows SpyBlocker",
                description = "Microsoft telemetry & diagnostic data",
                url = "https://raw.githubusercontent.com/crazy-max/WindowsSpyBlocker/master/data/hosts/spy.txt",
                category = BlocklistCategory.TRACKING,
                isEnabled = false
            ),
            BlocklistPreset(
                id = "frogeye_trackers",
                name = "FrogEye First-Party Trackers",
                description = "First-party & CNAME cloaked user trackers",
                url = "https://hostfiles.frogeye.fr/firstparty-trackers-hosts.txt",
                category = BlocklistCategory.TRACKING,
                isEnabled = false
            ),
            BlocklistPreset(
                id = "prigent_ads",
                name = "Prigent Analytics",
                description = "Mobile analytics & SDK tracking servers",
                url = "https://v.firebog.net/hosts/Prigent-Ads.txt",
                category = BlocklistCategory.TRACKING,
                isEnabled = false
            ),

            // ─── 3. Security & Threats ───
            BlocklistPreset(
                id = "urlhaus",
                name = "URLhaus by abuse.ch",
                description = "Live malware payloads, botnets & C2 servers",
                url = "https://urlhaus.abuse.ch/downloads/hostfile/",
                category = BlocklistCategory.SECURITY,
                isEnabled = false
            ),
            BlocklistPreset(
                id = "phishing_army",
                name = "Phishing Army Extended",
                description = "Active verified phishing & credential theft domains",
                url = "https://phishing.army/download/phishing_army_blocklist_extended.txt",
                category = BlocklistCategory.SECURITY,
                isEnabled = false
            ),
            BlocklistPreset(
                id = "dandelion_malware",
                name = "DandelionSprout Anti-Malware",
                description = "High accuracy ransomware & Trojan domain sinkhole",
                url = "https://raw.githubusercontent.com/DandelionSprout/adfilt/master/Alternate%20versions%20Anti-Malware%20List/AntiMalwareHosts.txt",
                category = BlocklistCategory.SECURITY,
                isEnabled = false
            ),
            BlocklistPreset(
                id = "prigent_crypto",
                name = "Prigent Cryptominers",
                description = "In-browser cryptocurrency miners & drainers",
                url = "https://v.firebog.net/hosts/Prigent-Crypto.txt",
                category = BlocklistCategory.SECURITY,
                isEnabled = false
            ),
            BlocklistPreset(
                id = "hagezi_tif_mini",
                name = "HaGeZi Threat Feeds (TIF Mini)",
                description = "Multi-source consolidated threat intelligence",
                url = "https://cdn.jsdelivr.net/gh/hagezi/dns-blocklists@latest/wildcard/tif.mini-onlydomains.txt",
                category = BlocklistCategory.SECURITY,
                isEnabled = false
            ),

            // ─── 4. Scams & Suspicious ───
            BlocklistPreset(
                id = "kadhosts",
                name = "KADhosts (PolishFiltersTeam)",
                description = "Scam portals, fake lotteries & fraud sinks",
                url = "https://raw.githubusercontent.com/PolishFiltersTeam/KADhosts/master/KADhosts.txt",
                category = BlocklistCategory.SCAMS,
                isEnabled = false
            ),
            BlocklistPreset(
                id = "someonewhocares",
                name = "Dan Pollock Zero Hosts",
                description = "Longstanding community spam & scam list",
                url = "https://someonewhocares.org/hosts/zero/hosts",
                category = BlocklistCategory.SCAMS,
                isEnabled = false
            ),
            BlocklistPreset(
                id = "fademind_spam",
                name = "FadeMind Spam Hosts",
                description = "Referrer spam & marketing trap domains",
                url = "https://raw.githubusercontent.com/FadeMind/hosts.extras/master/add.Spam/hosts",
                category = BlocklistCategory.SCAMS,
                isEnabled = false
            ),
            BlocklistPreset(
                id = "hagezi_fake",
                name = "HaGeZi Fake Stores & Scams",
                description = "Fake online shops, subscription traps & scams",
                url = "https://cdn.jsdelivr.net/gh/hagezi/dns-blocklists@latest/wildcard/fake-onlydomains.txt",
                category = BlocklistCategory.SCAMS,
                isEnabled = false
            ),

            // ─── 5. Content & Safety ───
            BlocklistPreset(
                id = "chadmayfield_porn",
                name = "Chad Mayfield Adult / NSFW",
                description = "Comprehensive adult & explicit content blocker",
                url = "https://raw.githubusercontent.com/chadmayfield/pihole-blocklists/master/lists/pi_blocklist_porn_all.list",
                category = BlocklistCategory.CONTENT,
                isEnabled = false
            ),
            BlocklistPreset(
                id = "hagezi_gambling",
                name = "HaGeZi Gambling (Mini)",
                description = "Online casinos, betting & gambling websites",
                url = "https://cdn.jsdelivr.net/gh/hagezi/dns-blocklists@latest/wildcard/gambling.mini-onlydomains.txt",
                category = BlocklistCategory.CONTENT,
                isEnabled = false
            ),
            BlocklistPreset(
                id = "hagezi_piracy",
                name = "HaGeZi Anti-Piracy",
                description = "Warez, illegal torrent portals & streaming sites",
                url = "https://cdn.jsdelivr.net/gh/hagezi/dns-blocklists@latest/wildcard/anti.piracy-onlydomains.txt",
                category = BlocklistCategory.CONTENT,
                isEnabled = false
            ),
            BlocklistPreset(
                id = "hagezi_bypass",
                name = "HaGeZi VPN / Proxy Bypass",
                description = "Open proxies, anonymous VPNs & Tor exit nodes",
                url = "https://cdn.jsdelivr.net/gh/hagezi/dns-blocklists@latest/wildcard/doh-vpn-proxy-bypass-onlydomains.txt",
                category = BlocklistCategory.CONTENT,
                isEnabled = false
            )
        )
    }

    private fun loadPresets(): List<BlocklistPreset> {
        return DEFAULT_PRESETS.map { defaultPreset ->
            val isEnabled = prefs.getBoolean("preset_${defaultPreset.id}_enabled", defaultPreset.isEnabled)
            val count = prefs.getInt("preset_${defaultPreset.id}_count", 0)
            val updated = prefs.getLong("preset_${defaultPreset.id}_updated", 0L)
            defaultPreset.copy(isEnabled = isEnabled, domainCount = count, lastUpdated = updated)
        }
    }

    private fun loadCustomUrls(): List<CustomBlocklistUrl> {
        val serialized = prefs.getString("custom_blocklist_urls", null) ?: return emptyList()
        val list = mutableListOf<CustomBlocklistUrl>()
        serialized.split(";;;").forEach { item ->
            val parts = item.split("|||")
            if (parts.size >= 4) {
                list.add(
                    CustomBlocklistUrl(
                        id = parts[0],
                        name = parts[1],
                        url = parts[2],
                        isEnabled = parts[3].toBooleanStrictOrNull() ?: true,
                        domainCount = parts.getOrNull(4)?.toIntOrNull() ?: 0
                    )
                )
            }
        }
        return list
    }

    private fun saveCustomUrls(list: List<CustomBlocklistUrl>) {
        val serialized = list.joinToString(";;;") { "${it.id}|||${it.name}|||${it.url}|||${it.isEnabled}|||${it.domainCount}" }
        prefs.edit().putString("custom_blocklist_urls", serialized).apply()
    }

    private fun loadWhitelist(): Set<String> {
        return prefs.getStringSet("domain_whitelist", emptySet()) ?: emptySet()
    }

    fun setShieldEnabled(enabled: Boolean) {
        _isShieldEnabled.value = enabled
        prefs.edit().putBoolean("shield_enabled", enabled).apply()
        scope.launch {
            recompileBlocklist()
        }
    }

    fun togglePreset(presetId: String, enabled: Boolean) {
        val updated = _presets.value.map {
            if (it.id == presetId) it.copy(isEnabled = enabled) else it
        }
        _presets.value = updated
        prefs.edit().putBoolean("preset_${presetId}_enabled", enabled).apply()

        scope.launch {
            val file = getPresetFile(presetId)
            if (enabled && (!file.exists() || file.length() == 0L)) {
                // Fetch this specific list in background
                val preset = updated.firstOrNull { it.id == presetId }
                if (preset != null) {
                    val downloaded = downloadAndCacheList(preset.url, file)
                    if (downloaded != null) {
                        prefs.edit()
                            .putInt("preset_${presetId}_count", downloaded.size)
                            .putLong("preset_${presetId}_updated", System.currentTimeMillis())
                            .apply()
                        _presets.value = _presets.value.map {
                            if (it.id == presetId) it.copy(domainCount = downloaded.size, lastUpdated = System.currentTimeMillis()) else it
                        }
                    }
                }
            }
            recompileBlocklist()
        }
    }

    fun addCustomUrl(name: String, url: String) {
        val newUrl = CustomBlocklistUrl(
            id = "custom_" + UUID.randomUUID().toString().take(8),
            name = name.trim(),
            url = url.trim(),
            isEnabled = true
        )
        val updated = _customUrls.value + newUrl
        _customUrls.value = updated
        saveCustomUrls(updated)

        scope.launch {
            val file = getCustomFile(newUrl.id)
            val downloaded = downloadAndCacheList(newUrl.url, file)
            if (downloaded != null) {
                _customUrls.value = _customUrls.value.map {
                    if (it.id == newUrl.id) it.copy(domainCount = downloaded.size) else it
                }
                saveCustomUrls(_customUrls.value)
            }
            recompileBlocklist()
        }
    }

    fun removeCustomUrl(id: String) {
        val updated = _customUrls.value.filter { it.id != id }
        _customUrls.value = updated
        saveCustomUrls(updated)
        getCustomFile(id).delete()

        scope.launch {
            recompileBlocklist()
        }
    }

    fun toggleCustomUrl(id: String, enabled: Boolean) {
        val updated = _customUrls.value.map {
            if (it.id == id) it.copy(isEnabled = enabled) else it
        }
        _customUrls.value = updated
        saveCustomUrls(updated)

        scope.launch {
            val file = getCustomFile(id)
            if (enabled && (!file.exists() || file.length() == 0L)) {
                val custom = updated.firstOrNull { it.id == id }
                if (custom != null) {
                    val downloaded = downloadAndCacheList(custom.url, file)
                    if (downloaded != null) {
                        _customUrls.value = _customUrls.value.map {
                            if (it.id == id) it.copy(domainCount = downloaded.size) else it
                        }
                        saveCustomUrls(_customUrls.value)
                    }
                }
            }
            recompileBlocklist()
        }
    }

    fun addWhitelistDomain(rawDomain: String) {
        val domain = rawDomain.trim().lowercase(Locale.US).removePrefix("*.").removePrefix(".").removeSuffix(".")
        if (domain.isBlank()) return
        val updated = _whitelist.value + domain
        _whitelist.value = updated
        prefs.edit().putStringSet("domain_whitelist", updated).apply()
    }

    fun removeWhitelistDomain(domain: String) {
        val updated = _whitelist.value - domain.lowercase(Locale.US)
        _whitelist.value = updated
        prefs.edit().putStringSet("domain_whitelist", updated).apply()
    }

    fun isDomainWhitelisted(rawDomain: String): Boolean {
        if (rawDomain.isBlank()) return false
        val domain = rawDomain.trim().lowercase(Locale.US).removeSuffix(".")
        val set = _whitelist.value
        if (set.isEmpty()) return false

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

    private fun getPresetFile(presetId: String): File = File(context.filesDir, "preset_${presetId}.txt")
    private fun getCustomFile(customId: String): File = File(context.filesDir, "custom_${customId}.txt")

    /**
     * Seeds initial default blocklist file if not present.
     */
    private suspend fun initStarterCacheIfNeeded() = withContext(Dispatchers.IO) {
        val adawayFile = getPresetFile("adaway")
        if (!adawayFile.exists() || adawayFile.length() == 0L) {
            try {
                context.assets.open("default_blocklist.txt").use { input ->
                    FileOutputStream(adawayFile).use { output ->
                        input.copyTo(output)
                    }
                }
                val count = countDomainsInFile(adawayFile)
                prefs.edit()
                    .putInt("preset_adaway_count", count)
                    .putLong("preset_adaway_updated", System.currentTimeMillis())
                    .apply()
                _presets.value = _presets.value.map {
                    if (it.id == "adaway") it.copy(domainCount = count, lastUpdated = System.currentTimeMillis()) else it
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Recompiles the active in-memory blocklist strictly from enabled feeds.
     */
    private suspend fun recompileBlocklist() = withContext(Dispatchers.IO) {
        if (!_isShieldEnabled.value) {
            BlocklistEngine.updateDomains(emptySet())
            _totalBlockedCount.value = 0
            return@withContext
        }

        val activePresets = _presets.value.filter { it.isEnabled }
        val activeCustom = _customUrls.value.filter { it.isEnabled }

        val combinedDomains = HashSet<String>(100000)

        for (preset in activePresets) {
            val file = getPresetFile(preset.id)
            if (file.exists() && file.length() > 0L) {
                try {
                    FileInputStream(file).use { input ->
                        BlocklistParser.parseStream(input, combinedDomains)
                    }
                } catch (_: Exception) {}
            }
        }

        for (custom in activeCustom) {
            val file = getCustomFile(custom.id)
            if (file.exists() && file.length() > 0L) {
                try {
                    FileInputStream(file).use { input ->
                        BlocklistParser.parseStream(input, combinedDomains)
                    }
                } catch (_: Exception) {}
            }
        }

        BlocklistEngine.updateDomains(combinedDomains)
        _totalBlockedCount.value = combinedDomains.size
    }

    /**
     * Downloads and refreshes all active blocklists in the background when the user taps update.
     */
    fun triggerUpdate(onComplete: ((Boolean) -> Unit)? = null) {
        if (_isUpdating.value) return
        scope.launch {
            _isUpdating.value = true
            val success = updateAllActiveBlocklists()
            _isUpdating.value = false
            onComplete?.invoke(success)
        }
    }

    private suspend fun updateAllActiveBlocklists(): Boolean = withContext(Dispatchers.IO) {
        var anySuccess = false
        val currentPresets = _presets.value
        val currentCustom = _customUrls.value

        for (preset in currentPresets) {
            if (preset.isEnabled) {
                val file = getPresetFile(preset.id)
                val downloaded = downloadAndCacheList(preset.url, file)
                if (downloaded != null) {
                    prefs.edit()
                        .putInt("preset_${preset.id}_count", downloaded.size)
                        .putLong("preset_${preset.id}_updated", System.currentTimeMillis())
                        .apply()
                    _presets.value = _presets.value.map {
                        if (it.id == preset.id) it.copy(domainCount = downloaded.size, lastUpdated = System.currentTimeMillis()) else it
                    }
                    anySuccess = true
                }
            }
        }

        for (custom in currentCustom) {
            if (custom.isEnabled) {
                val file = getCustomFile(custom.id)
                val downloaded = downloadAndCacheList(custom.url, file)
                if (downloaded != null) {
                    _customUrls.value = _customUrls.value.map {
                        if (it.id == custom.id) it.copy(domainCount = downloaded.size) else it
                    }
                    saveCustomUrls(_customUrls.value)
                    anySuccess = true
                }
            }
        }

        recompileBlocklist()
        anySuccess
    }

    private fun downloadAndCacheList(url: String, targetFile: File): Set<String>? {
        return try {
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body ?: return null

            val tempFile = File(context.cacheDir, "temp_${UUID.randomUUID()}.txt")
            FileOutputStream(tempFile).use { output ->
                body.byteStream().copyTo(output)
            }

            val set = HashSet<String>()
            FileInputStream(tempFile).use { input ->
                BlocklistParser.parseStream(input, set)
            }

            if (set.isNotEmpty()) {
                tempFile.copyTo(targetFile, overwrite = true)
            }
            tempFile.delete()
            set
        } catch (_: Exception) {
            null
        }
    }

    private fun countDomainsInFile(file: File): Int {
        val set = HashSet<String>()
        try {
            FileInputStream(file).use { input ->
                BlocklistParser.parseStream(input, set)
            }
        } catch (_: Exception) {}
        return set.size
    }
}
