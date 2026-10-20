package com.uxis.utility.ui

import android.net.TrafficStats
import android.os.Process
import android.os.SystemClock
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uxis.utility.core.AppStore
import com.uxis.utility.core.LinkParser
import com.uxis.utility.core.VpnBus
import com.uxis.utility.core.VpnState
import kotlinx.coroutines.delay

fun statusTitle(s: VpnState) = when (s) {
    VpnState.Idle -> "Не подключено"
    VpnState.Connecting -> "Подключение…"
    VpnState.Connected -> "Защищено"
    VpnState.Stopping -> "Отключение…"
    VpnState.Error -> "Ошибка"
}

fun routingTitle(r: String) = when (r) {
    "global" -> "Весь трафик через прокси"
    "bypass_ru" -> "RU и локальная сеть напрямую"
    else -> "Локальная сеть напрямую"
}

fun protocolTitle(p: String) = when (p) {
    "shadowsocks" -> "SS"
    else -> p.uppercase()
}

private fun formatSpeed(b: Long): String = when {
    b < 1024 -> "$b B/s"
    b < 1024 * 1024 -> String.format("%.1f KB/s", b / 1024f)
    else -> String.format("%.2f MB/s", b / 1024f / 1024f)
}

private fun formatTime(sec: Long): String =
    String.format("%02d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60)

@Composable
fun HomeScreen(store: AppStore, onToggle: () -> Unit, onOpenServers: () -> Unit) {
    val t = LocalUx.current
    val state by VpnBus.state.collectAsState()
    val message by VpnBus.message.collectAsState()
    val selected = store.selectedServer()
    val node = remember(selected?.link) { selected?.let { LinkParser.parse(it.link) } }

    var elapsed by remember { mutableLongStateOf(0L) }
    var down by remember { mutableLongStateOf(0L) }
    var up by remember { mutableLongStateOf(0L) }
    LaunchedEffect(state) {
        if (state == VpnState.Connected) {
            val uid = Process.myUid()
            var lastRx = TrafficStats.getUidRxBytes(uid)
            var lastTx = TrafficStats.getUidTxBytes(uid)
            while (true) {
                elapsed = (SystemClock.elapsedRealtime() - VpnBus.connectedAt) / 1000
                delay(1000)
                val rx = TrafficStats.getUidRxBytes(uid)
                val tx = TrafficStats.getUidTxBytes(uid)
                down = (rx - lastRx).coerceAtLeast(0)
                up = (tx - lastTx).coerceAtLeast(0)
                lastRx = rx
                lastTx = tx
            }
        } else {
            elapsed = 0; down = 0; up = 0
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LogoMark(42.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                UxText("Uxis utility", 20.sp, FontWeight.Bold)
                UxText(routingTitle(store.routing), 12.sp, color = t.textDim, maxLines = 1)
            }
            StatusPill(state)
        }

        Spacer(Modifier.weight(1f))
        ConnectButton(state, onToggle)
        Spacer(Modifier.height(4.dp))

        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 }) togetherWith
                    (fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 2 })
            },
            label = "status",
        ) { s ->
            UxText(statusTitle(s), 28.sp, FontWeight.SemiBold, align = TextAlign.Center)
        }
        Spacer(Modifier.height(4.dp))
        val sub = when (state) {
            VpnState.Connected -> formatTime(elapsed)
            VpnState.Error -> message
            VpnState.Idle -> "Нажмите, чтобы подключиться"
            else -> " "
        }
        UxText(sub, 14.sp, color = if (state == VpnState.Error) Color(0xFFFF6B7A) else t.textDim, maxLines = 3, align = TextAlign.Center)

        Spacer(Modifier.weight(1f))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(Modifier.weight(1f), UxIcons.ArrowDown, "Загрузка", formatSpeed(down))
            StatCard(Modifier.weight(1f), UxIcons.ArrowUp, "Отдача", formatSpeed(up))
        }
        Spacer(Modifier.height(12.dp))

        Glass(Modifier.fillMaxWidth(), onClick = onOpenServers) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(t.accent, t.accent2))),
                    contentAlignment = Alignment.Center,
                ) { SvgIcon(UxIcons.Globe, Color.White, 22.dp) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    UxText(selected?.name ?: "Сервер не выбран", 16.sp, FontWeight.SemiBold, maxLines = 1)
                    UxText(
                        if (node != null) "${protocolTitle(node.protocol)} · ${node.network} · ${node.security}"
                        else "Добавьте конфигурацию или подписку",
                        12.sp, color = t.textDim, maxLines = 1,
                    )
                }
                if (selected != null) PingBadge(selected.ping)
                Spacer(Modifier.width(6.dp))
                SvgIcon(UxIcons.Chevron, t.textDim, 18.dp)
            }
        }
    }
}

@Composable
private fun StatusPill(state: VpnState) {
    val t = LocalUx.current
    val color by animateColorAsState(
        when (state) {
            VpnState.Connected -> Color(0xFF3DDC97)
            VpnState.Error -> Color(0xFFFF5C6C)
            VpnState.Idle -> t.textDim
            else -> Color(0xFFFFC14D)
        }, tween(400), label = "pill",
    )
    val inf = rememberInfiniteTransition(label = "dot")
    val a by inf.animateFloat(0.35f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
    Glass(shape = CircleShape) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .alpha(if (state == VpnState.Connecting || state == VpnState.Stopping) a else 1f)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(8.dp))
            UxText(if (state == VpnState.Connected) "ON" else "OFF", 12.sp, FontWeight.Bold)
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, icon: ImageVector, label: String, value: String) {
    val t = LocalUx.current
    Glass(modifier) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(t.chip), contentAlignment = Alignment.Center) {
                SvgIcon(icon, t.accent, 18.dp)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                UxText(label, 11.sp, color = t.textDim)
                UxText(value, 15.sp, FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

@Composable
fun PingBadge(ping: Int) {
    val t = LocalUx.current
    val (text, color) = when {
        ping == -2 -> "—" to t.textDim
        ping < 0 -> "timeout" to Color(0xFFFF5C6C)
        ping < 150 -> "$ping ms" to Color(0xFF3DDC97)
        ping < 400 -> "$ping ms" to Color(0xFFFFC14D)
        else -> "$ping ms" to Color(0xFFFF8A4D)
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) { UxText(text, 11.sp, FontWeight.SemiBold, color = color) }
}

@Composable
fun ConnectButton(state: VpnState, onClick: () -> Unit) {
    val t = LocalUx.current
    val connected = state == VpnState.Connected
    val busy = state == VpnState.Connecting || state == VpnState.Stopping
    val inf = rememberInfiniteTransition(label = "btn")
    val rot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "rot")
    val pulse by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearOutSlowInEasing)), label = "pulse")
    val breathe by inf.animateFloat(0.96f, 1.04f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "br")
    val glow by animateFloatAsState(if (connected) 1f else 0f, tween(700), label = "glow")
    val busyAlpha by animateFloatAsState(if (busy) 1f else 0f, tween(300), label = "busy")
    val iconColor by animateColorAsState(if (connected) Color.White else t.text, tween(500), label = "icon")
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, spring(dampingRatio = 0.45f, stiffness = 400f), label = "scale")

    Box(Modifier.size(270.dp), contentAlignment = Alignment.Center) {
        // Soft glow
        Canvas(Modifier.fillMaxSize().graphicsLayer { scaleX = breathe; scaleY = breathe }) {
            drawCircle(
                Brush.radialGradient(listOf(t.accent.copy(alpha = 0.10f + 0.40f * glow), Color.Transparent)),
                radius = size.minDimension / 2,
            )
        }
        // Ripples while connected
        if (glow > 0.01f) {
            Canvas(Modifier.fillMaxSize()) {
                val base = size.minDimension * 0.33f
                for (i in 0..1) {
                    val p = (pulse + i * 0.5f) % 1f
                    drawCircle(
                        t.accent2.copy(alpha = (1f - p) * 0.45f * glow),
                        radius = base + p * size.minDimension * 0.17f,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }
        }
        // Spinner
        Canvas(Modifier.size(212.dp).alpha(busyAlpha).rotate(rot)) {
            drawArc(
                Brush.sweepGradient(listOf(Color.Transparent, t.accent, t.accent2)),
                startAngle = 0f, sweepAngle = 300f, useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        // Glass disc
        Box(
            Modifier
                .size(180.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(t.glassHi, t.glassLo)))
                .border(1.5.dp, Brush.linearGradient(listOf(t.borderHi, t.borderLo)), CircleShape)
                .clickable(interaction, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .alpha(glow)
                    .background(Brush.linearGradient(listOf(t.accent, t.accent2)))
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)))
            )
            SvgIcon(UxIcons.Power, iconColor, 66.dp)
        }
    }
}
