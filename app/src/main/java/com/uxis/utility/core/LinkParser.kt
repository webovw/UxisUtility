package com.uxis.utility.core

import android.net.Uri
import android.util.Base64
import org.json.JSONObject

data class ProxyNode(
    val protocol: String,
    val name: String,
    val address: String,
    val port: Int,
    val id: String = "",
    val method: String = "",
    val alterId: Int = 0,
    val flow: String = "",
    val encryption: String = "none",
    val network: String = "tcp",
    val security: String = "none",
    val sni: String = "",
    val fingerprint: String = "",
    val alpn: String = "",
    val publicKey: String = "",
    val shortId: String = "",
    val spiderX: String = "",
    val path: String = "",
    val host: String = "",
    val serviceName: String = "",
    val headerType: String = "",
    val mode: String = "",
    val allowInsecure: Boolean = false,
)

object LinkParser {
    private val schemes = listOf("vless://", "vmess://", "trojan://", "ss://")

    fun isLink(s: String): Boolean = schemes.any { s.startsWith(it, ignoreCase = true) }

    fun decodeBase64(input: String): String {
        var s = input.trim().replace("\n", "").replace("\r", "").replace(" ", "")
        while (s.length % 4 != 0) s += "="
        val flags = if (s.contains('-') || s.contains('_')) Base64.URL_SAFE else Base64.DEFAULT
        return String(Base64.decode(s, flags), Charsets.UTF_8)
    }

    fun parse(link: String): ProxyNode? = try {
        val l = link.trim()
        when {
            l.startsWith("vless://", true) -> parseStd(l, "vless")
            l.startsWith("trojan://", true) -> parseStd(l, "trojan")
            l.startsWith("vmess://", true) -> parseVmess(l)
            l.startsWith("ss://", true) -> parseSs(l)
            else -> null
        }
    } catch (e: Exception) {
        null
    }

    /** Extracts proxy links from plain text or a base64 subscription body. */
    fun extractLinks(text: String): List<String> {
        val t = text.trim()
        val plain = if (t.lines().any { isLink(it.trim()) }) t
        else runCatching { decodeBase64(t) }.getOrDefault(t)
        return plain.lines().map { it.trim() }.filter { isLink(it) && parse(it) != null }
    }

    private fun parseStd(link: String, proto: String): ProxyNode {
        val u = Uri.parse(link)
        fun q(k: String) = u.getQueryParameter(k) ?: ""
        val host = (u.host ?: "").trim('[', ']')
        require(host.isNotEmpty() && u.port > 0)
        return ProxyNode(
            protocol = proto,
            name = u.fragment?.takeIf { it.isNotBlank() } ?: host,
            address = host,
            port = u.port,
            id = Uri.decode(u.encodedUserInfo ?: ""),
            flow = q("flow"),
            encryption = q("encryption").ifEmpty { "none" },
            network = q("type").ifEmpty { "tcp" },
            security = q("security").ifEmpty { if (proto == "trojan") "tls" else "none" },
            sni = q("sni").ifEmpty { q("peer") },
            fingerprint = q("fp"),
            alpn = q("alpn"),
            publicKey = q("pbk"),
            shortId = q("sid"),
            spiderX = q("spx"),
            path = q("path"),
            host = q("host"),
            serviceName = q("serviceName"),
            headerType = q("headerType"),
            mode = q("mode"),
            allowInsecure = q("allowInsecure") == "1" || q("insecure") == "1",
        )
    }

    private fun parseVmess(link: String): ProxyNode {
        val j = JSONObject(decodeBase64(link.substring(8).substringBefore('#')))
        val net = j.optString("net").ifEmpty { "tcp" }
        val tls = j.optString("tls")
        return ProxyNode(
            protocol = "vmess",
            name = j.optString("ps").ifEmpty { j.optString("add") },
            address = j.optString("add"),
            port = j.optString("port").trim().toInt(),
            id = j.optString("id"),
            alterId = j.optString("aid").toIntOrNull() ?: 0,
            method = j.optString("scy").ifEmpty { "auto" },
            network = net,
            security = if (tls.isEmpty()) "none" else tls,
            sni = j.optString("sni"),
            fingerprint = j.optString("fp"),
            alpn = j.optString("alpn"),
            path = j.optString("path"),
            host = j.optString("host"),
            serviceName = if (net == "grpc") j.optString("path") else "",
            headerType = j.optString("type"),
        )
    }

    private fun parseSs(link: String): ProxyNode {
        val body = link.substring(5)
        val name = Uri.decode(body.substringAfter('#', ""))
        var main = body.substringBefore('#').substringBefore('?').trimEnd('/')
        if (!main.contains('@')) main = decodeBase64(main)
        val userInfo = Uri.decode(main.substringBeforeLast('@'))
        val hostPort = main.substringAfterLast('@')
        val creds = if (userInfo.contains(':')) userInfo else decodeBase64(userInfo)
        val host = hostPort.substringBeforeLast(':').trim('[', ']')
        return ProxyNode(
            protocol = "shadowsocks",
            name = name.ifBlank { host },
            address = host,
            port = hostPort.substringAfterLast(':').trimEnd('/').toInt(),
            method = creds.substringBefore(':'),
            id = creds.substringAfter(':'),
        )
    }
}
