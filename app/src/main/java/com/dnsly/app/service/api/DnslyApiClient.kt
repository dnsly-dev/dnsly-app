package com.dnsly.app.service.api

import android.content.Context
import android.os.Build
import android.util.Log
import com.dnsly.app.data.DnsRepository
import com.dnsly.app.service.blocklist.BlocklistManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class DnslyApiClient private constructor(private val context: Context) {

    private val prefs = context.getSharedPreferences("dnsly_api_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    val deviceId: String
        get() {
            var id = prefs.getString("device_uuid", null)
            if (id == null) {
                id = UUID.randomUUID().toString()
                prefs.edit().putString("device_uuid", id).apply()
            }
            return id
        }

    fun scheduleHeartbeat(repository: DnsRepository, force: Boolean = false) {
        scope.launch {
            val lastSync = prefs.getLong("last_heartbeat_time", 0L)
            val lastTotal = prefs.getLong("last_heartbeat_total", -1L)
            val lastBlocked = prefs.getLong("last_heartbeat_blocked", -1L)
            val currentTotal = repository.totalQueries.value
            val currentBlocked = repository.blockedQueries.value
            val now = System.currentTimeMillis()

            val hasChanged = (currentTotal != lastTotal || currentBlocked != lastBlocked)
            val thresholdMet = (currentTotal - lastTotal >= 10 || currentBlocked - lastBlocked >= 3)
            val minIntervalElapsed = (now - lastSync) >= MIN_SYNC_INTERVAL_MS
            val cooldownElapsed = (now - lastSync) >= COOLDOWN_MS

            if (force || (minIntervalElapsed && thresholdMet) || (minIntervalElapsed && hasChanged && cooldownElapsed) || lastSync == 0L) {
                sendHeartbeat(repository)
            } else {
                Log.d(TAG, "Heartbeat sync skipped (cooldown/threshold active).")
            }
        }
    }

    fun onQueryRecorded(repository: DnsRepository) {
        val currentTotal = repository.totalQueries.value
        val currentBlocked = repository.blockedQueries.value
        val lastTotal = prefs.getLong("last_heartbeat_total", 0L)
        val lastBlocked = prefs.getLong("last_heartbeat_blocked", 0L)
        val lastSync = prefs.getLong("last_heartbeat_time", 0L)
        val now = System.currentTimeMillis()

        if ((currentTotal - lastTotal >= 10 || currentBlocked - lastBlocked >= 3) && (now - lastSync) >= MIN_SYNC_INTERVAL_MS) {
            scheduleHeartbeat(repository, force = true)
        }
    }

    suspend fun sendHeartbeat(repository: DnsRepository): Boolean = withContext(Dispatchers.IO) {
        try {
            val blocklistManager = BlocklistManager.getInstance(context)
            val selectedServer = repository.selectedServer.value
            val isShieldActive = repository.isVpnConnected.value && blocklistManager.isShieldEnabled.value
            val totalCount = repository.totalQueries.value
            val blockedCount = repository.blockedQueries.value

            val payload = JSONObject().apply {
                put("deviceId", deviceId)
                put("appVersion", "1.0.0")
                put("selectedProvider", selectedServer.name)
                put("shieldEnabled", isShieldActive)
                put("totalQueries", totalCount)
                put("blockedQueries", blockedCount)
                put("deviceModel", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
                put("osVersion", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                put("countryCode", detectCountryCode(context))
                put("cpuArch", Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a")
                put("timestamp", System.currentTimeMillis())
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = payload.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(BASE_URL + "api/v1/telemetry/heartbeat")
                .header("x-api-key", API_KEY)
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Heartbeat successfully synchronized with DNSly API (Total: $totalCount, Blocked: $blockedCount)")
                    prefs.edit()
                        .putLong("last_heartbeat_time", System.currentTimeMillis())
                        .putLong("last_heartbeat_total", totalCount)
                        .putLong("last_heartbeat_blocked", blockedCount)
                        .apply()
                    true
                } else {
                    Log.w(TAG, "Heartbeat sync failed with HTTP ${response.code}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send telemetry heartbeat: ${e.message}")
            false
        }
    }

    private fun detectCountryCode(context: Context): String {
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
            if (tm != null) {
                val networkCountry = tm.networkCountryIso
                if (!networkCountry.isNullOrBlank()) {
                    return networkCountry.trim().uppercase(Locale.US)
                }
                val simCountry = tm.simCountryIso
                if (!simCountry.isNullOrBlank()) {
                    return simCountry.trim().uppercase(Locale.US)
                }
            }
        } catch (_: Exception) {
            // Telephony unavailable
        }

        try {
            val localeCountry = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.resources.configuration.locales.get(0)?.country
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale?.country
            }
            if (!localeCountry.isNullOrBlank()) {
                return localeCountry.trim().uppercase(Locale.US)
            }
        } catch (_: Exception) {
            // Fallback
        }

        val defaultCountry = Locale.getDefault().country
        return if (defaultCountry.isNotBlank()) defaultCountry.uppercase(Locale.US) else "US"
    }

    companion object {
        const val BASE_URL = "https://api.dnsly.shovon.bd/"
        const val API_KEY = "cf1a5804f6a44c45da05b04dda52f8bc75242cdf0c827db5fa2aa94dd8bce8a7"
        private const val TAG = "DnslyApiClient"
        private const val MIN_SYNC_INTERVAL_MS = 15 * 1000L // Min 15s between syncs to debounce
        private const val COOLDOWN_MS = 2 * 60 * 1000L // 2-Minute periodic cooldown

        @Volatile
        private var INSTANCE: DnslyApiClient? = null

        fun getInstance(context: Context): DnslyApiClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DnslyApiClient(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
