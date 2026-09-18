package com.dnsly.app.service

import okhttp3.ConnectionPool
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.InetAddress
import java.net.Socket
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.net.SocketFactory

/**
 * Native DNS-over-HTTPS (DoH, RFC 8484) Client.
 * Features:
 * 1. Automatic VpnService socket protection so HTTPS traffic bypasses the TUN interface.
 * 2. Bootstrap DNS resolution pinning DoH hostnames directly to known primary IPv4 addresses (no circular DNS deadlock).
 * 3. HTTP/2 connection pooling with low-latency keep-alive.
 */
class DohClient(
    private val socketProtector: (Socket) -> Boolean
) {

    private val dnsMediaType = "application/dns-message".toMediaType()
    private val bootstrapIpMap = ConcurrentHashMap<String, List<InetAddress>>()

    private val protectedSocketFactory = object : SocketFactory() {
        private val defaultFactory = SocketFactory.getDefault()

        override fun createSocket(): Socket {
            val socket = defaultFactory.createSocket()
            socketProtector(socket)
            return socket
        }

        override fun createSocket(host: String?, port: Int): Socket {
            val socket = defaultFactory.createSocket(host, port)
            socketProtector(socket)
            return socket
        }

        override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket {
            val socket = defaultFactory.createSocket(host, port, localHost, localPort)
            socketProtector(socket)
            return socket
        }

        override fun createSocket(host: InetAddress?, port: Int): Socket {
            val socket = defaultFactory.createSocket(host, port)
            socketProtector(socket)
            return socket
        }

        override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket {
            val socket = defaultFactory.createSocket(address, port, localAddress, localPort)
            socketProtector(socket)
            return socket
        }
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .socketFactory(protectedSocketFactory)
        .connectionPool(ConnectionPool(32, 5, TimeUnit.MINUTES))
        .dispatcher(okhttp3.Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 32
        })
        .connectTimeout(2000, TimeUnit.MILLISECONDS)
        .readTimeout(2000, TimeUnit.MILLISECONDS)
        .writeTimeout(2000, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                val pinned = bootstrapIpMap[hostname]
                if (!pinned.isNullOrEmpty()) {
                    return pinned
                }
                return try {
                    Dns.SYSTEM.lookup(hostname)
                } catch (e: Exception) {
                    emptyList()
                }
            }
        })
        .build()

    /**
     * Registers known primary IPv4 address for a DoH server's hostname to prevent circular DNS resolution.
     */
    fun registerBootstrap(dohUrl: String, ipv4Primary: String) {
        if (ipv4Primary.isBlank()) return
        try {
            val uri = URI(dohUrl)
            val host = uri.host ?: return
            val addr = InetAddress.getByName(ipv4Primary)
            bootstrapIpMap[host] = listOf(addr)
        } catch (_: Exception) {
        }
    }

    /**
     * Executes RFC 8484 DNS query over HTTPS.
     * Returns the raw DNS response binary payload, or null if query failed.
     */
    fun query(dohUrl: String, queryPayload: ByteArray): ByteArray? {
        return try {
            val requestBody = queryPayload.toRequestBody(dnsMediaType)
            val request = Request.Builder()
                .url(dohUrl)
                .post(requestBody)
                .header("Accept", "application/dns-message")
                .header("User-Agent", "DNSly-DoH/1.0")
                .build()

            val response = client.newCall(request).execute()
            response.use { resp ->
                if (resp.isSuccessful) {
                    resp.body?.bytes()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
