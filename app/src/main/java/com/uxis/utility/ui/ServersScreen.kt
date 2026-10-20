package com.uxis.utility.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.uxis.utility.core.AppStore
import com.uxis.utility.core.LinkParser
import com.uxis.utility.core.Net
import com.uxis.utility.core.ServerItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

@Composable
fun ServersScreen(store: AppStore) {
    val t = LocalUx.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    val inf = rememberInfiniteTransition(label = "r")
    val rot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "rot")

    fun pingAll() = scope.launch {
        store.servers.toList().map { s ->
            async {
                val n = LinkParser.parse(s.link)
                store.setPing(s.id, if (n == null) -1 else Net.tcpPing(n.address, n.port))
            }
        }.awaitAll()
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                UxText("Серверы", 30.sp, FontWeight.Bold)
                UxText("${store.servers.size} конфигураций", 13.sp, color = t.textDim)
            }
            Box(Modifier.rotate(if (refreshing) rot else 0f)) {
                GlassIconButton(UxIcons.Refresh) {
                    if (!refreshing) scope.launch {
                        refreshing = true
                        val n = store.refreshSubscriptions()
                        refreshing = false
                        Toast.makeText(ctx, "Подписки обновлены: $n", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            GlassIconButton(UxIcons.Zap) { pingAll() }
            Spacer(Modifier.width(8.dp))
            GlassIconButton(UxIcons.Plus) { showAdd = true }
        }
        Spacer(Modifier.height(16.dp))

        if (store.servers.isEmpty()) {
            EmptyState { showAdd = true }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(store.servers, key = { it.id }) { s ->
                    ServerRow(
                        s,
                        selected = s.id == store.selectedId,
                        onClick = { store.select(s.id) },
                        onDelete = { store.remove(s.id) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    if (showAdd) {
        AddDialog(onDismiss = { showAdd = false }) { text ->
            scope.launch {
                val n = runCatching { store.importText(text) }.getOrElse { -1 }
                val msg = when {
                    n < 0 -> "Не удалось загрузить подписку"
                    n == 0 -> "Ссылки не найдены"
                    else -> "Добавлено: $n"
                }
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                if (n > 0) showAdd = false
            }
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    val t = LocalUx.current
    Glass(Modifier.fillMaxWidth(), onClick = onAdd) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(Brush.linearGradient(listOf(t.accent, t.accent2))),
                contentAlignment = Alignment.Center,
            ) { SvgIcon(UxIcons.Link, Color.White, 28.dp) }
            Spacer(Modifier.height(14.dp))
            UxText("Пока пусто", 18.sp, FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            UxText(
                "Добавьте ссылку vless, vmess, trojan, ss или URL подписки",
                13.sp, color = t.textDim, align = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ServerRow(s: ServerItem, selected: Boolean, onClick: () -> Unit, onDelete: () -> Unit, modifier: Modifier) {
    val t = LocalUx.current
    val node = remember(s.link) { LinkParser.parse(s.link) }
    Glass(modifier.fillMaxWidth(), highlight = selected, onClick = onClick) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) Brush.linearGradient(listOf(t.accent, t.accent2))
                        else Brush.linearGradient(listOf(t.chip, t.chip))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(selected, label = "sel") { sel ->
                    SvgIcon(if (sel) UxIcons.Check else UxIcons.Globe, if (sel) Color.White else t.textDim, 18.dp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                UxText(s.name, 15.sp, FontWeight.SemiBold, maxLines = 1)
                UxText(
                    node?.let { "${protocolTitle(it.protocol)} · ${it.address}:${it.port}" } ?: "Ошибка ссылки",
                    12.sp, color = t.textDim, maxLines = 1,
                )
            }
            PingBadge(s.ping)
            Spacer(Modifier.width(4.dp))
            Glass(Modifier.size(34.dp), shape = CircleShape, contentAlignment = Alignment.Center, onClick = onDelete) {
                SvgIcon(UxIcons.Trash, t.textDim, 16.dp)
            }
        }
    }
}

@Composable
private fun AddDialog(onDismiss: () -> Unit, onImport: (String) -> Unit) {
    val t = LocalUx.current
    val clipboard = LocalClipboardManager.current
    var text by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Glass(Modifier.fillMaxWidth(), solid = true, shape = RoundedCornerShape(28.dp)) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UxText("Добавить", 22.sp, FontWeight.Bold, modifier = Modifier.weight(1f))
                    GlassIconButton(UxIcons.Close, 36.dp, onDismiss)
                }
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 110.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(t.chip)
                        .padding(14.dp)
                ) {
                    if (text.isEmpty()) {
                        UxText("vless://…  или  https://подписка", 14.sp, color = t.textDim)
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        textStyle = TextStyle(color = t.text, fontSize = 14.sp),
                        cursorBrush = SolidColor(t.accent),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Glass(
                        Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(16.dp),
                        contentAlignment = Alignment.Center,
                        onClick = { clipboard.getText()?.text?.let { onImport(it) } },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SvgIcon(UxIcons.Clipboard, t.text, 18.dp)
                            Spacer(Modifier.width(8.dp))
                            UxText("Из буфера", 14.sp, FontWeight.SemiBold)
                        }
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(t.accent, t.accent2))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Glass(
                            Modifier.fillMaxSize(), shape = RoundedCornerShape(16.dp),
                            contentAlignment = Alignment.Center,
                            onClick = { if (text.isNotBlank()) onImport(text) },
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SvgIcon(UxIcons.Plus, Color.White, 18.dp)
                                Spacer(Modifier.width(8.dp))
                                UxText("Добавить", 14.sp, FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
