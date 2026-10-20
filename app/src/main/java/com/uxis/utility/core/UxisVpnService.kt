package com.uxis.utility.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.uxis.utility.MainActivity
import com.uxis.utility.R
import kotlinx.coroutines.flow.MutableStateFlow
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray
import java.io.File
import java.util.concurrent.Executors

enum class VpnState { Idle, Connecting, Connected, Stopping, Error }

object VpnBus {
    val state = MutableStateFlow(VpnState.Idle)
    val message = MutableStateFlow("")
    @Volatile var connectedAt = 0L
}

class UxisVpnService : VpnService(), CoreCallbackHandler {

    companion object {
        const val ACTION_START = "com.uxis.utility.START"
        const val ACTION_STOP = "com.uxis.utility.STOP"
        private const val CHANNEL = "uxis_vpn"
        private const val NOTIFY_ID = 7
    }

    private val worker = Executors.newSingleThreadExecutor()
    private var tun: ParcelFileDescriptor? = null
    private var core: CoreController? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            worker.execute { stopAll() }
        } else {
            showForeground("Подключение…")
            worker.execute { startAll() }
        }
        return START_NOT_STICKY
    }

    private fun startAll() {
        if (core?.isRunning == true) return
        VpnBus.state.value = VpnState.Connecting
        try {
            val link = Prefs.selectedLink(this) ?: error("Сервер не выбран")
            val node = LinkParser.parse(link) ?: error("Не удалось разобрать ссылку сервера")
            val sp = Prefs.sp(this)
            val ipv6 = sp.getBoolean("ipv6", false)

            val b = Builder()
                .setSession("Uxis utility")
                .setMtu(1500)
                .addAddress("10.88.0.1", 30)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
            if (ipv6) b.addAddress("fd88:7578:6973::1", 126).addRoute("::", 0)
            runCatching { b.addDisallowedApplication(packageName) }
            if (Build.VERSION.SDK_INT >= 29) b.setMetered(false)
            b.setConfigureIntent(openAppIntent())
            val pfd = b.establish() ?: error("Нет разрешения на VPN")
            tun = pfd

            val geoDir = prepareGeo()
            Libv2ray.initCoreEnv(geoDir.absolutePath, "")
            val controller = core ?: Libv2ray.newCoreController(this).also { core = it }
            val config = XrayConfig.build(
                node,
                sp.getString("routing", "bypass_lan") ?: "bypass_lan",
                sp.getBoolean("blockAds", false),
                ipv6,
            )
            controller.startLoop(config, pfd.fd)

            VpnBus.connectedAt = SystemClock.elapsedRealtime()
            VpnBus.message.value = ""
            VpnBus.state.value = VpnState.Connected
            showForeground("Подключено · ${node.name}")
        } catch (e: Throwable) {
            VpnBus.message.value = e.message ?: e.javaClass.simpleName
            releaseTunnel()
            VpnBus.state.value = VpnState.Error
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun stopAll() {
        VpnBus.state.value = VpnState.Stopping
        releaseTunnel()
        VpnBus.state.value = VpnState.Idle
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releaseTunnel() {
        runCatching { core?.stopLoop() }
        runCatching { tun?.close() }
        tun = null
    }

    /** Copies geoip/geosite from APK assets into internal storage (needed for routing rules). */
    private fun prepareGeo(): File {
        val dir = File(filesDir, "geo").apply { mkdirs() }
        val version = packageManager.getPackageInfo(packageName, 0).lastUpdateTime
        val sp = Prefs.sp(this)
        if (sp.getLong("geoVersion", 0L) != version) {
            listOf("geoip.dat", "geosite.dat").forEach { name ->
                runCatching {
                    assets.open(name).use { input -> File(dir, name).outputStream().use { input.copyTo(it) } }
                }
            }
            sp.edit().putLong("geoVersion", version).apply()
        }
        return dir
    }

    override fun onRevoke() {
        worker.execute { stopAll() }
    }

    override fun onDestroy() {
        releaseTunnel()
        if (VpnBus.state.value != VpnState.Error) VpnBus.state.value = VpnState.Idle
        super.onDestroy()
    }

    // ---- Xray callbacks ----
    override fun startup(): Long = 0
    override fun shutdown(): Long = 0
    override fun onEmitStatus(p0: Long, p1: String?): Long = 0

    // ---- Notification ----
    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this, 0, Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun showForeground(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "VPN", NotificationManager.IMPORTANCE_LOW))
        val stop = PendingIntent.getService(
            this, 1, Intent(this, UxisVpnService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_u)
            .setContentTitle("Uxis utility")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(openAppIntent())
            .addAction(0, "Отключить", stop)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFY_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFY_ID, n)
        }
    }
}
