package com.dnsly.app.data

import android.content.Context
import com.dnsly.app.model.DnsServer
import com.dnsly.app.model.DnsType
import com.dnsly.app.model.QueryLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.Collections
import java.util.UUID

import com.dnsly.app.data.local.DnsDatabase

class DnsRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("dnsly_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database = DnsDatabase.getInstance(context)

    private val _servers = MutableStateFlow<List<DnsServer>>(loadServers())
    val servers: StateFlow<List<DnsServer>> = _servers.asStateFlow()

    private val _selectedServer = MutableStateFlow<DnsServer>(loadSelectedServer())
    val selectedServer: StateFlow<DnsServer> = _selectedServer.asStateFlow()

    private val _isVpnConnected = MutableStateFlow(false)
    val isVpnConnected: StateFlow<Boolean> = _isVpnConnected.asStateFlow()

    private val _queryLogs = MutableStateFlow<List<QueryLog>>(emptyList())
    val queryLogs: StateFlow<List<QueryLog>> = _queryLogs.asStateFlow()

    private val _totalQueries = MutableStateFlow(prefs.getLong("stat_total_queries", 0L))
    val totalQueries: StateFlow<Long> = _totalQueries.asStateFlow()

    private val _blockedQueries = MutableStateFlow(prefs.getLong("stat_blocked_queries", 0L))
    val blockedQueries: StateFlow<Long> = _blockedQueries.asStateFlow()

    private val logList = Collections.synchronizedList(mutableListOf<QueryLog>())

    init {
        scope.launch {
            val stored = database.getRecentLogs(200)
            synchronized(logList) {
                logList.clear()
                logList.addAll(stored)
                _queryLogs.value = logList.toList()
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: DnsRepository? = null

        fun getInstance(context: Context): DnsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DnsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private fun loadServers(): List<DnsServer> {
        val baseList = DnsCatalog.defaultList.toMutableList()
        val customJson = prefs.getString("custom_dns_list", null)
        if (!customJson.isNullOrBlank()) {
            customJson.split(";;;").forEach { item ->
                val parts = item.split("|||")
                if (parts.size >= 5) {
                    baseList.add(
                        DnsServer(
                            id = parts[0],
                            name = parts[1],
                            type = DnsType.CUSTOM,
                            categories = parts.getOrNull(2) ?: "Custom manual DNS",
                            ipv4Primary = parts.getOrNull(3) ?: "",
                            ipv4Secondary = parts.getOrNull(4) ?: "",
                            hostname = parts.getOrNull(5) ?: "",
                            dohUrl = parts.getOrNull(6) ?: "",
                            officialLink = "",
                            isCustom = true
                        )
                    )
                }
            }
        }
        return baseList
    }

    private fun loadSelectedServer(): DnsServer {
        val savedId = prefs.getString("selected_server_id", "adguard_default")
        return _servers.value.find { it.id == savedId } ?: DnsCatalog.defaultList.first()
    }

    fun selectServer(server: DnsServer) {
        _selectedServer.value = server
        prefs.edit().putString("selected_server_id", server.id).apply()
    }

    fun setVpnConnected(connected: Boolean) {
        _isVpnConnected.value = connected
        com.dnsly.app.service.api.DnslyApiClient.getInstance(context).scheduleHeartbeat(this, force = true)
    }

    fun addCustomServer(
        name: String,
        ipv4Primary: String,
        ipv4Secondary: String = "",
        hostname: String = "",
        dohUrl: String = "",
        categories: String = "Custom DNS"
    ): DnsServer {
        val newServer = DnsServer(
            id = "custom_" + UUID.randomUUID().toString().take(8),
            name = name.trim(),
            type = DnsType.CUSTOM,
            categories = categories.ifBlank { "Custom manual DNS" },
            ipv4Primary = ipv4Primary.trim(),
            ipv4Secondary = ipv4Secondary.trim(),
            hostname = hostname.trim(),
            dohUrl = dohUrl.trim(),
            officialLink = "",
            isCustom = true
        )

        val updated = _servers.value.toMutableList().apply { add(newServer) }
        _servers.value = updated
        saveCustomServers(updated)
        return newServer
    }

    fun removeCustomServer(serverId: String) {
        val updated = _servers.value.filter { it.id != serverId || !it.isCustom }
        _servers.value = updated
        saveCustomServers(updated)
        if (_selectedServer.value.id == serverId) {
            selectServer(DnsCatalog.defaultList.first())
        }
    }

    private fun saveCustomServers(allList: List<DnsServer>) {
        val customOnly = allList.filter { it.isCustom }
        val serialized = customOnly.joinToString(";;;") {
            "${it.id}|||${it.name}|||${it.categories}|||${it.ipv4Primary}|||${it.ipv4Secondary}|||${it.hostname}|||${it.dohUrl}"
        }
        prefs.edit().putString("custom_dns_list", serialized).apply()
    }

    fun recordQuery(log: QueryLog) {
        val updatedList = synchronized(logList) {
            logList.add(0, log)
            while (logList.size > 200) {
                logList.removeAt(logList.size - 1)
            }
            logList.toList()
        }
        _queryLogs.value = updatedList

        val newTotal = _totalQueries.value + 1
        _totalQueries.value = newTotal

        val newBlocked = if (log.isBlocked) {
            val count = _blockedQueries.value + 1
            _blockedQueries.value = count
            count
        } else {
            _blockedQueries.value
        }

        // Asynchronously persist to SQLite WAL database and stats
        scope.launch {
            database.insertLog(log)
            prefs.edit()
                .putLong("stat_total_queries", newTotal)
                .putLong("stat_blocked_queries", newBlocked)
                .apply()

            // Trigger telemetry sync if query/block thresholds are met
            com.dnsly.app.service.api.DnslyApiClient.getInstance(context).onQueryRecorded(this@DnsRepository)
        }
    }

    fun clearLogs() {
        logList.clear()
        _queryLogs.value = emptyList()
        scope.launch {
            database.clearLogs()
        }
    }

    fun resetAllStats() {
        logList.clear()
        _queryLogs.value = emptyList()
        _totalQueries.value = 0L
        _blockedQueries.value = 0L
        prefs.edit().putLong("stat_total_queries", 0L).putLong("stat_blocked_queries", 0L).apply()
        scope.launch {
            database.clearLogs()
        }
    }

    fun runBenchmark(onComplete: (() -> Unit)? = null) {
        scope.launch {
            val currentServers = _servers.value
            val updated = currentServers.map { server ->
                val ping = measureDnsLatency(server)
                server.copy(latencyMs = ping)
            }.sortedBy { it.latencyMs ?: 9999 }

            _servers.value = updated
            val currSelected = _selectedServer.value
            _servers.value.find { it.id == currSelected.id }?.let {
                _selectedServer.value = it
            }
            onComplete?.invoke()
        }
    }

    private suspend fun measureDnsLatency(server: DnsServer): Int? = withContext(Dispatchers.IO) {
        val targetIp = server.ipv4Primary.ifBlank {
            try {
                if (server.hostname.isNotBlank()) {
                    InetAddress.getByName(server.hostname).hostAddress
                } else null
            } catch (e: Exception) {
                null
            }
        } ?: return@withContext null

        try {
            val address = InetAddress.getByName(targetIp)
            DatagramSocket().use { socket ->
                socket.soTimeout = 2000
                // Minimal standard DNS query for google.com (Type A)
                val queryBytes = byteArrayOf(
                    0x12, 0x34, // ID
                    0x01, 0x00, // RD=1
                    0x00, 0x01, // QDCOUNT=1
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x06, 0x67, 0x6f, 0x6f, 0x67, 0x6c, 0x65, // google
                    0x03, 0x63, 0x6f, 0x6d, // com
                    0x00, // root
                    0x00, 0x01, // Type A
                    0x00, 0x01  // Class IN
                )

                val packet = DatagramPacket(queryBytes, queryBytes.size, address, 53)
                val startTime = System.currentTimeMillis()
                socket.send(packet)

                val receiveBuf = ByteArray(512)
                val receivePacket = DatagramPacket(receiveBuf, receiveBuf.size)
                socket.receive(receivePacket)
                val elapsed = (System.currentTimeMillis() - startTime).toInt()
                return@withContext elapsed
            }
        } catch (e: Exception) {
            return@withContext null
        }
    }
}
