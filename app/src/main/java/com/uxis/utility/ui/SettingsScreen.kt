package com.uxis.utility.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uxis.utility.core.AppStore
import libv2ray.Libv2ray

@Composable
fun SettingsScreen(store: AppStore) {
    val t = LocalUx.current
    val coreVersion = remember { runCatching { Libv2ray.checkVersionX() }.getOrDefault("—") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        UxText("Настройки", 30.sp, FontWeight.Bold)

        Section(UxIcons.Palette, "Тема") {
            UxThemes.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { theme ->
                        ThemeSwatch(theme, theme.id == store.themeId, Modifier.weight(1f)) { store.themeId = theme.id }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }

        Section(UxIcons.Network, "Маршрутизация") {
            listOf(
                "global" to "Весь трафик идёт через прокси",
                "bypass_lan" to "Локальные адреса открываются напрямую",
                "bypass_ru" to "Российские сайты и LAN напрямую",
            ).forEach { (id, desc) ->
                OptionRow(routingTitle(id), desc, id == store.routing) { store.routing = id }
                Spacer(Modifier.height(8.dp))
            }
        }

        Section(UxIcons.Shield, "Параметры") {
            ToggleRow(UxIcons.Block, "Блокировать рекламу", "Правила geosite:category-ads-all", store.blockAds) { store.blockAds = it }
            Spacer(Modifier.height(10.dp))
            ToggleRow(UxIcons.Globe, "IPv6", "Туннелировать IPv6 трафик", store.ipv6) { store.ipv6 = it }
            Spacer(Modifier.height(10.dp))
            UxText("Изменения применяются при следующем подключении", 12.sp, color = t.textDim)
        }

        Glass(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                LogoMark(56.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    UxText("Uxis utility", 18.sp, FontWeight.Bold)
                    UxText("Версия 1.0.0", 13.sp, color = t.textDim)
                    UxText(coreVersion, 12.sp, color = t.textDim, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun Section(icon: ImageVector, title: String, content: @Composable ColumnScope.() -> Unit) {
    val t = LocalUx.current
    Glass(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SvgIcon(icon, t.accent, 20.dp)
                Spacer(Modifier.width(10.dp))
                UxText(title, 17.sp, FontWeight.SemiBold)
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun ThemeSwatch(theme: UxTheme, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val t = LocalUx.current
    Glass(modifier.height(92.dp), highlight = selected, shape = RoundedCornerShape(20.dp), onClick = onClick) {
        Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(theme.blob1, theme.accent2, theme.bgBottom)))
                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                val k by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.5f), label = "chk")
                SvgIcon(UxIcons.Check, Color.White, 18.dp, Modifier.graphicsLayer { scaleX = k; scaleY = k; alpha = k })
            }
            Spacer(Modifier.height(8.dp))
            UxText(theme.title, 12.sp, FontWeight.Medium, color = if (selected) t.text else t.textDim)
        }
    }
}

@Composable
private fun OptionRow(title: String, desc: String, selected: Boolean, onClick: () -> Unit) {
    val t = LocalUx.current
    val dot by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.5f), label = "dot")
    Glass(Modifier.fillMaxWidth(), highlight = selected, shape = RoundedCornerShape(18.dp), onClick = onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(22.dp).clip(CircleShape).border(2.dp, if (selected) t.accent else t.textDim, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size((10 * dot).dp).clip(CircleShape).background(t.accent))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                UxText(title, 14.sp, FontWeight.SemiBold)
                UxText(desc, 12.sp, color = t.textDim)
            }
        }
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val t = LocalUx.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(t.chip), contentAlignment = Alignment.Center) {
            SvgIcon(icon, t.text, 18.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            UxText(title, 14.sp, FontWeight.SemiBold)
            UxText(desc, 12.sp, color = t.textDim)
        }
        UxSwitch(checked, onChange)
    }
}
