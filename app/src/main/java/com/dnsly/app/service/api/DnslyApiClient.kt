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
            val now = System.currentTimeMillis()
            if (force || (now - lastSync) >= COOLDOWN_MS) {
                sendHeartbeat(repository)
            } else {
                val remainingMins = ((COOLDOWN_MS - (now - lastSync)) / 60000)
                Log.d(TAG, "Heartbeat throttled by 1-hour cooldown. Next sync in $remainingMins minutes.")
            }
        }
    }

    suspend fun sendHeartbeat(repository: DnsRepository): Boolean = withContext(Dispatchers.IO) {
        try {
            val blocklistManager = BlocklistManager.getInstance(context)
            val selectedServer = repository.selectedServer.value
            val isShieldActive = blocklistManager.isShieldEnabled.value
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
                put("countryCode", Locale.getDefault().country.ifBlank { "US" })
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
                    Log.d(TAG, "Heartbeat successfully synchronized with DNSly API")
                    prefs.edit().putLong("last_heartbeat_time", System.currentTimeMillis()).apply()
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

    companion object {
        const val BASE_URL = "https://api.dnsly.shovon.bd/"
        const val API_KEY = "cf1a5804f6a44c45da05b04dda52f8bc75242cdf0c827db5fa2aa94dd8bce8a7"
        private const val TAG = "DnslyApiClient"
        private const val COOLDOWN_MS = 60 * 60 * 1000L // 1-Hour Cooldown

        @Volatile
        private var INSTANCE: DnslyApiClient? = null

        fun getInstance(context: Context): DnslyApiClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DnslyApiClient(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
