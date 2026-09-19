package com.dnsly.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.dnsly.app.MainActivity
import com.dnsly.app.R
import com.dnsly.app.data.DnsRepository
import com.dnsly.app.model.DnsServer
import com.dnsly.app.model.QueryLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import com.dnsly.app.service.blocklist.BlocklistEngine
import com.dnsly.app.service.blocklist.BlocklistManager
import java.nio.ByteBuffer

class DnsVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var vpnJob: Job? = null
    private lateinit var repository: DnsRepository
    private lateinit var blocklistManager: BlocklistManager
    private val dohClient by lazy { DohClient { socket -> protect(socket) } }

    companion object {
        const val ACTION_START = "com.dnsly.app.ACTION_START"
        const val ACTION_STOP = "com.dnsly.app.ACTION_STOP"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "dnsly_protection_channel"
        private const val TUN_IP = "10.0.0.2"
    }

    override fun onCreate() {
        super.onCreate()
        repository = DnsRepository.getInstance(applicationContext)
        blocklistManager = BlocklistManager.getInstance(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val server = repository.selectedServer.value
                startVpn(server)
            }
            ACTION_STOP -> {
                stopVpn()
            }
            else -> {
                val server = repository.selectedServer.value
                startVpn(server)
            }
        }
        return START_STICKY
    }

    private fun startVpn(server: DnsServer) {
        stopVpn()
        DnsCache.clear()

        val notification = buildNotification(server)
        startForeground(NOTIFICATION_ID, notification)

        try {
            val builder = Builder()
                .setSession("DNSly - ${server.name}")
                .addAddress(TUN_IP, 32)
                .setMtu(1500)
                .setBlocking(true)

            val primaryIp = server.ipv4Primary.ifBlank { "94.140.14.14" }
            builder.addDnsServer(primaryIp)
            builder.addRoute(primaryIp, 32)

            if (server.ipv4Secondary.isNotBlank()) {
                builder.addDnsServer(server.ipv4Secondary)
                builder.addRoute(server.ipv4Secondary, 32)
            }

            vpnInterface = builder.establish()
            if (vpnInterface == null) {
                stopSelf()
                return
            }

            repository.setVpnConnected(true)
            startPacketLoop(server, primaryIp)

        } catch (e: Exception) {
            e.printStackTrace()
            stopVpn()
        }
    }

    private fun startPacketLoop(server: DnsServer, upstreamIp: String) {
        val pfd = vpnInterface ?: return
        val inputStream = FileInputStream(pfd.fileDescriptor)
        val outputStream = FileOutputStream(pfd.fileDescriptor)

        vpnJob = serviceScope.launch {
            val packet = ByteArray(32767)

            while (isActive) {
                try {
                    val length = inputStream.read(packet)
                    if (length <= 0) continue

                    // Parse IP Header (IPv4)
                    val versionAndIhl = packet[0].toInt() and 0xFF
                    val version = versionAndIhl shr 4
                    if (version != 4) continue // Skip IPv6 on TUN for simplicity

                    val ihl = (versionAndIhl and 0x0F) * 4
                    val protocol = packet[9].toInt() and 0xFF

                    // UDP protocol check
                    if (protocol == 17 && length >= ihl + 8) {
                        val srcPort = ((packet[ihl].toInt() and 0xFF) shl 8) or (packet[ihl + 1].toInt() and 0xFF)
                        val dstPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)

                        // DNS queries sent to port 53
                        if (dstPort == 53) {
                            val udpPayloadOffset = ihl + 8
                            val udpPayloadLength = length - udpPayloadOffset

                            if (udpPayloadLength >= 12) {
                                // Extract query bytes and addresses synchronously to prevent race conditions on the shared buffer
                                val queryBytes = packet.copyOfRange(udpPayloadOffset, udpPayloadOffset + udpPayloadLength)
                                val srcIp = getSrcIp(packet, ihl)
                                val dstIp = getDstIp(packet, ihl)

                                val query = DnsPacketParser.parseQuery(queryBytes, 0, queryBytes.size)
                                if (query != null) {
                                    // 1. Check Local Ad & Tracker Shield
                                    val isShieldOn = blocklistManager.isShieldEnabled.value
                                    val isWhitelisted = blocklistManager.isDomainWhitelisted(query.domain)
                                    val isBlockedLocally = isShieldOn && !isWhitelisted && BlocklistEngine.isBlocked(query.domain)

                                    if (isBlockedLocally) {
                                        val blockedDnsPayload = DnsPacketParser.createBlockedResponse(
                                            dnsQueryPayload = queryBytes,
                                            queryOffset = 0,
                                            queryLength = queryBytes.size,
                                            queryType = query.queryType,
                                            asNxDomain = true,
                                            ttlSeconds = 60
                                        )

                                        val responsePacket = buildUdpIpPacket(
                                            srcIp = dstIp,
                                            dstIp = srcIp,
                                            srcPort = dstPort,
                                            dstPort = srcPort,
                                            payload = blockedDnsPayload
                                        )

                                        synchronized(outputStream) {
                                            outputStream.write(responsePacket)
                                            outputStream.flush()
                                        }

                                        repository.recordQuery(
                                            QueryLog(
                                                domain = query.domain,
                                                isBlocked = true,
                                                queryType = DnsPacketParser.getTypeName(query.queryType),
                                                upstreamServer = "DNSly Local Shield",
                                                latencyMs = 0L,
                                                reason = "Blocked by Local Ad & Tracker Shield",
                                                protocol = "Shield"
                                            )
                                        )
                                        continue
                                    }

                                    // 2. Check ultra-fast in-memory cache for repeat queries (<1ms)
                                    val cachedResponse = DnsCache.get(query.domain, query.queryType)
                                    if (cachedResponse != null && cachedResponse.size >= 12) {
                                        // Adapt transaction ID to match current query
                                        cachedResponse[0] = queryBytes[0]
                                        cachedResponse[1] = queryBytes[1]

                                        val responsePacket = buildUdpIpPacket(
                                            srcIp = dstIp,
                                            dstIp = srcIp,
                                            srcPort = dstPort,
                                            dstPort = srcPort,
                                            payload = cachedResponse
                                        )

                                        synchronized(outputStream) {
                                            outputStream.write(responsePacket)
                                            outputStream.flush()
                                        }

                                        val isBlockedCached = DnsPacketParser.isSinkholedResponse(cachedResponse)

                                        repository.recordQuery(
                                            QueryLog(
                                                domain = query.domain,
                                                isBlocked = isBlockedCached,
                                                queryType = DnsPacketParser.getTypeName(query.queryType),
                                                upstreamServer = "DNSly Fast Cache",
                                                latencyMs = 0L,
                                                reason = if (isBlockedCached) "Blocked by Upstream DNS (Cached)" else "Instant cache lookup (<1ms)",
                                                protocol = "Cache"
                                            )
                                        )
                                        continue
                                    }

                                    // Allowed domain: forward to chosen DNS resolver via DoH or UDP fallback asynchronously
                                    launch(Dispatchers.IO) {
                                        forwardDnsQuery(
                                            queryBytes = queryBytes,
                                            server = server,
                                            domain = query.domain,
                                            qType = query.queryType,
                                            srcPort = srcPort,
                                            dstPort = dstPort,
                                            srcIp = srcIp,
                                            dstIp = dstIp,
                                            outputStream = outputStream
                                        )
                                    }
                                    continue
                                }
                            }
                        }
                    }

                } catch (e: Exception) {
                    if (!isActive) break
                }
            }
        }
    }

    private fun forwardDnsQuery(
        queryBytes: ByteArray,
        server: DnsServer,
        domain: String,
        qType: Int,
        srcPort: Int,
        dstPort: Int,
        srcIp: ByteArray,
        dstIp: ByteArray,
        outputStream: FileOutputStream
    ) {
        val startTime = System.currentTimeMillis()

        // Ultra-Fast Direct Native UDP DNS (Port 53) - 10ms to 30ms latency
        val primaryIp = server.ipv4Primary.ifBlank { "1.1.1.1" }
        var resolved = resolveUdp(
            queryBytes = queryBytes,
            upstreamIp = primaryIp,
            startTime = startTime,
            domain = domain,
            qType = qType,
            srcPort = srcPort,
            dstPort = dstPort,
            srcIp = srcIp,
            dstIp = dstIp,
            outputStream = outputStream,
            serverName = server.name
        )

        // Instant fallback to secondary DNS IP if primary fails
        if (!resolved && server.ipv4Secondary.isNotBlank()) {
            resolved = resolveUdp(
                queryBytes = queryBytes,
                upstreamIp = server.ipv4Secondary,
                startTime = startTime,
                domain = domain,
                qType = qType,
                srcPort = srcPort,
                dstPort = dstPort,
                srcIp = srcIp,
                dstIp = dstIp,
                outputStream = outputStream,
                serverName = "${server.name} (Backup)"
            )
        }

        if (!resolved) {
            val latency = System.currentTimeMillis() - startTime
            repository.recordQuery(
                QueryLog(
                    domain = domain,
                    isBlocked = false,
                    queryType = DnsPacketParser.getTypeName(qType),
                    upstreamServer = "${server.name} (Timeout)",
                    latencyMs = latency,
                    reason = "DNS Request Timeout",
                    protocol = "Failed"
                )
            )
        }
    }

    private fun resolveUdp(
        queryBytes: ByteArray,
        upstreamIp: String,
        startTime: Long,
        domain: String,
        qType: Int,
        srcPort: Int,
        dstPort: Int,
        srcIp: ByteArray,
        dstIp: ByteArray,
        outputStream: FileOutputStream,
        serverName: String
    ): Boolean {
        return try {
            DatagramSocket().use { socket ->
                protect(socket)
                socket.soTimeout = 1500

                val upstreamAddr = InetAddress.getByName(upstreamIp)
                val outPacket = DatagramPacket(queryBytes, queryBytes.size, upstreamAddr, 53)
                socket.send(outPacket)

                val inBuf = ByteArray(2048)
                val inPacket = DatagramPacket(inBuf, inBuf.size)
                socket.receive(inPacket)

                val latency = System.currentTimeMillis() - startTime
                val respPayload = ByteArray(inPacket.length)
                System.arraycopy(inPacket.data, inPacket.offset, respPayload, 0, inPacket.length)

                val fullResponse = buildUdpIpPacket(
                    srcIp = dstIp,
                    dstIp = srcIp,
                    srcPort = dstPort,
                    dstPort = srcPort,
                    payload = respPayload
                )

                synchronized(outputStream) {
                    outputStream.write(fullResponse)
                    outputStream.flush()
                }

                // Check if upstream DNS response returned a sinkholed/blocked IP (0.0.0.0, 127.0.0.1, ::)
                val isBlocked = DnsPacketParser.isSinkholedResponse(respPayload)

                // Cache in memory for instant repeat lookups (<1ms)
                DnsCache.put(domain, qType, respPayload)

                repository.recordQuery(
                    QueryLog(
                        domain = domain,
                        isBlocked = isBlocked,
                        queryType = DnsPacketParser.getTypeName(qType),
                        upstreamServer = serverName,
                        latencyMs = latency,
                        reason = if (isBlocked) "Blocked by Upstream DNS Protection" else "Ultra-fast direct UDP (Port 53)",
                        protocol = "UDP"
                    )
                )
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun getSrcIp(packet: ByteArray, ihl: Int): ByteArray {
        val ip = ByteArray(4)
        System.arraycopy(packet, 12, ip, 0, 4)
        return ip
    }

    private fun getDstIp(packet: ByteArray, ihl: Int): ByteArray {
        val ip = ByteArray(4)
        System.arraycopy(packet, 16, ip, 0, 4)
        return ip
    }

    private fun buildUdpIpPacket(
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int,
        payload: ByteArray
    ): ByteArray {
        val totalLength = 20 + 8 + payload.size
        val buffer = ByteBuffer.allocate(totalLength)

        // IPv4 Header (20 bytes)
        buffer.put(0x45.toByte()) // Version 4, IHL 5
        buffer.put(0x00.toByte()) // TOS
        buffer.putShort(totalLength.toShort()) // Total length
        buffer.putShort((0..65535).random().toShort()) // Identification
        buffer.putShort(0x0000.toShort()) // Flags & Fragment offset
        buffer.put(64.toByte()) // TTL
        buffer.put(17.toByte()) // Protocol: UDP
        buffer.putShort(0.toShort()) // Checksum placeholder
        buffer.put(srcIp)
        buffer.put(dstIp)

        // Compute IP checksum
        val ipHeaderChecksum = computeChecksum(buffer.array(), 0, 20)
        buffer.putShort(10, ipHeaderChecksum.toShort())

        // UDP Header (8 bytes)
        buffer.position(20)
        buffer.putShort(srcPort.toShort())
        buffer.putShort(dstPort.toShort())
        buffer.putShort((8 + payload.size).toShort()) // UDP length
        buffer.putShort(0.toShort()) // UDP checksum optional in IPv4

        // UDP Payload (DNS)
        buffer.put(payload)

        return buffer.array()
    }

    private fun computeChecksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0
        var i = offset
        while (i < offset + length) {
            val b1 = data[i].toInt() and 0xFF
            val b2 = if (i + 1 < offset + length) data[i + 1].toInt() and 0xFF else 0
            sum += (b1 shl 8) or b2
            i += 2
        }
        while (sum shr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return sum.inv() and 0xFFFF
    }

    private fun stopVpn() {
        vpnJob?.cancel()
        vpnJob = null

        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        vpnInterface = null
        repository.setVpnConnected(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(server: DnsServer): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, DnsVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DNSly Active: ${server.name}")
            .setContentText("Shielding queries • ${server.displayIp}")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingOpen)
            .setOngoing(true)
            .addAction(0, "Disconnect", pendingStop)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        stopVpn()
        serviceScope.cancel()
        super.onDestroy()
    }
}
