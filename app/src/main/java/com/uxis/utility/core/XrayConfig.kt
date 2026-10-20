package com.uxis.utility.core

import org.json.JSONArray
import org.json.JSONObject

object XrayConfig {

    fun build(n: ProxyNode, routing: String, blockAds: Boolean, ipv6: Boolean): String {
        val root = JSONObject()
        root.put("log", JSONObject().put("loglevel", "warning"))
        root.put(
            "dns", JSONObject()
                .put("servers", JSONArray().put("1.1.1.1").put("8.8.8.8"))
                .put("queryStrategy", if (ipv6) "UseIP" else "UseIPv4")
        )

        val tun = JSONObject()
            .put("tag", "tun-in")
            .put("port", 0)
            .put("protocol", "tun")
            .put("settings", JSONObject().put("name", "xray0").put("MTU", 1500))
            .put(
                "sniffing", JSONObject()
                    .put("enabled", true)
                    .put("destOverride", JSONArray().put("http").put("tls").put("quic"))
                    .put("routeOnly", true)
            )
        root.put("inbounds", JSONArray().put(tun))

        val outbounds = JSONArray()
            .put(proxyOutbound(n))
            .put(JSONObject().put("tag", "direct").put("protocol", "freedom"))
            .put(JSONObject().put("tag", "block").put("protocol", "blackhole"))
            .put(JSONObject().put("tag", "dns-out").put("protocol", "dns"))
        root.put("outbounds", outbounds)

        val rules = JSONArray()
        rules.put(rule().put("network", "udp").put("port", "53").put("outboundTag", "dns-out"))
        if (blockAds) {
            rules.put(rule().put("domain", JSONArray().put("geosite:category-ads-all")).put("outboundTag", "block"))
        }
        if (routing == "bypass_lan" || routing == "bypass_ru") {
            rules.put(rule().put("ip", JSONArray().put("geoip:private")).put("outboundTag", "direct"))
        }
        if (routing == "bypass_ru") {
            rules.put(
                rule().put(
                    "domain", JSONArray()
                        .put("geosite:category-ru")
                        .put("domain:ru").put("domain:su").put("domain:xn--p1ai")
                ).put("outboundTag", "direct")
            )
            rules.put(rule().put("ip", JSONArray().put("geoip:ru")).put("outboundTag", "direct"))
        }
        root.put(
            "routing", JSONObject()
                .put("domainStrategy", if (routing == "global") "AsIs" else "IPIfNonMatch")
                .put("rules", rules)
        )
        return root.toString()
    }

    private fun rule() = JSONObject().put("type", "field")

    private fun proxyOutbound(n: ProxyNode): JSONObject {
        val o = JSONObject().put("tag", "proxy")
        when (n.protocol) {
            "vless" -> {
                val user = JSONObject().put("id", n.id).put("encryption", n.encryption)
                if (n.flow.isNotEmpty()) user.put("flow", n.flow)
                o.put("protocol", "vless").put("settings", JSONObject().put("vnext", vnext(n, user)))
            }
            "vmess" -> {
                val user = JSONObject().put("id", n.id).put("alterId", n.alterId).put("security", n.method)
                o.put("protocol", "vmess").put("settings", JSONObject().put("vnext", vnext(n, user)))
            }
            "trojan" -> {
                val server = JSONObject().put("address", n.address).put("port", n.port).put("password", n.id)
                o.put("protocol", "trojan").put("settings", JSONObject().put("servers", JSONArray().put(server)))
            }
            "shadowsocks" -> {
                val server = JSONObject().put("address", n.address).put("port", n.port)
                    .put("method", n.method).put("password", n.id)
                o.put("protocol", "shadowsocks").put("settings", JSONObject().put("servers", JSONArray().put(server)))
            }
        }
        if (n.protocol != "shadowsocks") o.put("streamSettings", stream(n))
        return o
    }

    private fun vnext(n: ProxyNode, user: JSONObject) = JSONArray().put(
        JSONObject().put("address", n.address).put("port", n.port).put("users", JSONArray().put(user))
    )

    private fun stream(n: ProxyNode): JSONObject {
        val net = when (n.network) {
            "", "raw", "tcp" -> "tcp"
            "splithttp" -> "xhttp"
            else -> n.network
        }
        val s = JSONObject().put("network", net).put("security", n.security)
        when (n.security) {
            "tls" -> {
                val tls = JSONObject().put("allowInsecure", n.allowInsecure)
                if (n.sni.isNotEmpty()) tls.put("serverName", n.sni)
                if (n.fingerprint.isNotEmpty()) tls.put("fingerprint", n.fingerprint)
                if (n.alpn.isNotEmpty()) tls.put("alpn", JSONArray(n.alpn.split(',').map { it.trim() }))
                s.put("tlsSettings", tls)
            }
            "reality" -> {
                s.put(
                    "realitySettings", JSONObject()
                        .put("serverName", n.sni)
                        .put("fingerprint", n.fingerprint.ifEmpty { "chrome" })
                        .put("publicKey", n.publicKey)
                        .put("shortId", n.shortId)
                        .put("spiderX", n.spiderX)
                )
            }
        }
        when (net) {
            "ws" -> s.put("wsSettings", JSONObject().put("path", n.path.ifEmpty { "/" }).put("host", n.host))
            "httpupgrade" -> s.put("httpupgradeSettings", JSONObject().put("path", n.path.ifEmpty { "/" }).put("host", n.host))
            "xhttp" -> s.put(
                "xhttpSettings", JSONObject().put("path", n.path.ifEmpty { "/" }).put("host", n.host)
                    .put("mode", n.mode.ifEmpty { "auto" })
            )
            "grpc" -> s.put(
                "grpcSettings", JSONObject().put("serviceName", n.serviceName).put("multiMode", n.mode == "multi")
            )
            "tcp" -> if (n.headerType == "http") {
                val request = JSONObject()
                    .put("path", JSONArray().put(n.path.ifEmpty { "/" }))
                    .put("headers", JSONObject().put("Host", JSONArray().put(n.host.ifEmpty { n.address })))
                s.put("tcpSettings", JSONObject().put("header", JSONObject().put("type", "http").put("request", request)))
            }
        }
        return s
    }
}
