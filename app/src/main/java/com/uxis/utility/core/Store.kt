package com.uxis.utility.core

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ServerItem(
    val id: String,
    val name: String,
    val link: String,
    val sub: String = "",
    val ping: Int = PING_UNKNOWN,
)

const val PING_UNKNOWN = -2
const val PING_FAIL = -1

object Prefs {
    fun sp(c: Context): SharedPreferences = c.getSharedPreferences("uxis", Context.MODE_PRIVATE)

    fun loadServers(c: Context): List<ServerItem> {
        val arr = runCatching { JSONArray(sp(c).getString("servers", "[]")) }.getOrDefault(JSONArray())
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            ServerItem(o.getString("id"), o.getString("name"), o.getString("link"), o.optString("sub"), o.optInt("ping", PING_UNKNOWN))
        }
    }

    fun saveServers(c: Context, list: List<ServerItem>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("id", it.id).put("name", it.name).put("link", it.link).put("sub", it.sub).put("ping", it.ping))
        }
        sp(c).edit().putString("servers", arr.toString()).apply()
    }

    fun selectedLink(c: Context): String? {
        val id = sp(c).getString("selected", null) ?: return null
        return loadServers(c).firstOrNull { it.id == id }?.link
    }
}

class AppStore(private val ctx: Context) {
    private val sp = Prefs.sp(ctx)

    val servers = mutableStateListOf<ServerItem>().apply { addAll(Prefs.loadServers(ctx)) }

    private val _selected = mutableStateOf(sp.getString("selected", null))
    var selectedId: String?
        get() = _selected.value
        set(v) { _selected.value = v; sp.edit().putString("selected", v).apply() }

    private val _theme = mutableStateOf(sp.getString("theme", "aurora") ?: "aurora")
    var themeId: String
        get() = _theme.value
        set(v) { _theme.value = v; sp.edit().putString("theme", v).apply() }

    private val _routing = mutableStateOf(sp.getString("routing", "bypass_lan") ?: "bypass_lan")
    var routing: String
        get() = _routing.value
        set(v) { _routing.value = v; sp.edit().putString("routing", v).apply() }

    private val _blockAds = mutableStateOf(sp.getBoolean("blockAds", false))
    var blockAds: Boolean
        get() = _blockAds.value
        set(v) { _blockAds.value = v; sp.edit().putBoolean("blockAds", v).apply() }

    private val _ipv6 = mutableStateOf(sp.getBoolean("ipv6", false))
    var ipv6: Boolean
        get() = _ipv6.value
        set(v) { _ipv6.value = v; sp.edit().putBoolean("ipv6", v).apply() }

    fun selectedServer(): ServerItem? = servers.firstOrNull { it.id == selectedId }

    private fun persist() = Prefs.saveServers(ctx, servers.toList())

    fun select(id: String) { selectedId = id }

    fun remove(id: String) {
        servers.removeAll { it.id == id }
        if (selectedId == id) selectedId = servers.firstOrNull()?.id
        persist()
    }

    fun setPing(id: String, ms: Int) {
        val i = servers.indexOfFirst { it.id == id }
        if (i >= 0) servers[i] = servers[i].copy(ping = ms)
        persist()
    }

    private fun addLinks(links: List<String>, sub: String) {
        links.forEach { link ->
            val node = LinkParser.parse(link) ?: return@forEach
            servers.add(ServerItem(UUID.randomUUID().toString(), node.name, link, sub))
        }
        if (selectedServer() == null) selectedId = servers.firstOrNull()?.id
        persist()
    }

    /** Import a link list, a base64 blob or a subscription URL. Returns imported count. */
    suspend fun importText(text: String): Int {
        val t = text.trim()
        if (t.startsWith("http://") || t.startsWith("https://")) {
            val links = withContext(Dispatchers.IO) { LinkParser.extractLinks(Net.fetch(t)) }
            servers.removeAll { it.sub == t }
            addLinks(links, t)
            return links.size
        }
        val links = LinkParser.extractLinks(t)
        addLinks(links, "")
        return links.size
    }

    /** Re-download every saved subscription. */
    suspend fun refreshSubscriptions(): Int {
        var total = 0
        servers.map { it.sub }.filter { it.isNotEmpty() }.distinct().forEach { url ->
            val links = runCatching { withContext(Dispatchers.IO) { LinkParser.extractLinks(Net.fetch(url)) } }.getOrNull()
            if (links != null) {
                servers.removeAll { it.sub == url }
                addLinks(links, url)
                total += links.size
            }
        }
        return total
    }
}
