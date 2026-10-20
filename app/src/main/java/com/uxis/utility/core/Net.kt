package com.uxis.utility.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

object Net {
    fun fetch(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 15_000
        c.setRequestProperty("User-Agent", "v2rayNG/1.10.5 UxisUtility/1.0")
        try {
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    suspend fun tcpPing(host: String, port: Int): Int = withContext(Dispatchers.IO) {
        try {
            Socket().use { s ->
                val start = System.nanoTime()
                s.connect(InetSocketAddress(host, port), 3000)
                ((System.nanoTime() - start) / 1_000_000).toInt()
            }
        } catch (e: Exception) {
            PING_FAIL
        }
    }
}
