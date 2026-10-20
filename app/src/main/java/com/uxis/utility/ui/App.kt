package com.uxis.utility.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uxis.utility.core.AppStore
import kotlinx.coroutines.delay

@Composable
fun UxisApp(store: AppStore, onToggle: () -> Unit) {
    val target = UxThemes.firstOrNull { it.id == store.themeId } ?: UxThemes.first()
    val theme = animatedTheme(target)
    CompositionLocalProvider(LocalUx provides theme) {
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var splash by rememberSaveable { mutableStateOf(true) }
        LaunchedEffect(Unit) { delay(1200); splash = false }

        Box(Modifier.fillMaxSize()) {
            AuroraBackground()
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (fadeIn(tween(380)) + slideInHorizontally(tween(380, easing = FastOutSlowInEasing)) { it / 5 * dir })
                        .togetherWith(fadeOut(tween(220)) + slideOutHorizontally(tween(220)) { -it / 5 * dir })
                },
                modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
                label = "tabs",
            ) { page ->
                when (page) {
                    0 -> HomeScreen(store, onToggle) { tab = 1 }
                    1 -> ServersScreen(store)
                    else -> SettingsScreen(store)
                }
            }
            BottomBar(
                tab, { tab = it },
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 14.dp),
            )
            AnimatedVisibility(
                visible = splash,
                enter = fadeIn(),
                exit = fadeOut(tween(500)) + scaleOut(tween(500), targetScale = 1.1f),
            ) { Splash() }
        }
    }
}

@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier) {
    val t = LocalUx.current
    val items = listOf(UxIcons.Home to "Главная", UxIcons.Servers to "Серверы", UxIcons.Sliders to "Настройки")
    val itemW = 98.dp
    val offset by animateDpAsState(itemW * selected, spring(dampingRatio = 0.68f, stiffness = 380f), label = "ind")
    Glass(modifier.height(66.dp).width(itemW * 3 + 12.dp), shape = RoundedCornerShape(33.dp)) {
        Box(Modifier.fillMaxSize().padding(6.dp)) {
            Box(
                Modifier
                    .offset(x = offset)
                    .width(itemW)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(27.dp))
                    .background(Brush.linearGradient(listOf(t.accent, t.accent2)))
            )
            Row(Modifier.fillMaxSize()) {
                items.forEachIndexed { i, (icon, label) ->
                    val sel = i == selected
                    val color by animateColorAsState(if (sel) Color.White else t.textDim, tween(300), label = "c$i")
                    Column(
                        Modifier
                            .width(itemW)
                            .fillMaxHeight()
                            .clickable(remember { MutableInteractionSource() }, indication = null) { onSelect(i) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        SvgIcon(icon, color, 22.dp)
                        AnimatedVisibility(sel, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                            UxText(label, 11.sp, FontWeight.SemiBold, color = color, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Splash() {
    val t = LocalUx.current
    val s = remember { Animatable(0.5f) }
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        a.animateTo(1f, tween(400))
    }
    LaunchedEffect(Unit) {
        s.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 180f))
    }
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(t.bgTop, t.bgBottom))),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer { scaleX = s.value; scaleY = s.value; alpha = a.value },
        ) {
            LogoMark(112.dp)
            Spacer(Modifier.height(20.dp))
            UxText("Uxis utility", 28.sp, FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            UxText("secure proxy client", 13.sp, color = t.textDim)
        }
    }
}
