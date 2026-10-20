package com.uxis.utility.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

data class UxTheme(
    val id: String,
    val title: String,
    val dark: Boolean,
    val bgTop: Color,
    val bgBottom: Color,
    val blob1: Color,
    val blob2: Color,
    val blob3: Color,
    val accent: Color,
    val accent2: Color,
) {
    val text: Color get() = if (dark) Color.White else Color(0xFF15162E)
    val textDim: Color get() = text.copy(alpha = 0.6f)
    val glassHi: Color get() = if (dark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.72f)
    val glassLo: Color get() = if (dark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.36f)
    val borderHi: Color get() = if (dark) Color.White.copy(alpha = 0.34f) else Color.White.copy(alpha = 0.95f)
    val borderLo: Color get() = if (dark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.3f)
    val chip: Color get() = if (dark) Color.White.copy(alpha = 0.08f) else Color(0xFF15162E).copy(alpha = 0.06f)
    val dialogBg: Color get() = if (dark) bgBottom.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.9f)
}

val UxThemes = listOf(
    UxTheme("aurora", "Aurora", true, Color(0xFF0B0B1F), Color(0xFF16103A),
        Color(0xFF7B5CFF), Color(0xFF2EC5FF), Color(0xFFFF5CAA), Color(0xFF8C7BFF), Color(0xFF2EC5FF)),
    UxTheme("ocean", "Ocean", true, Color(0xFF03111D), Color(0xFF052B3B),
        Color(0xFF00C2FF), Color(0xFF00E0B0), Color(0xFF3A6BFF), Color(0xFF00B4F0), Color(0xFF00E0B0)),
    UxTheme("sunset", "Sunset", true, Color(0xFF190913), Color(0xFF2C0E1F),
        Color(0xFFFF7A59), Color(0xFFFF3D7F), Color(0xFFFFC14D), Color(0xFFFF5F7E), Color(0xFFFFAA4D)),
    UxTheme("emerald", "Emerald", true, Color(0xFF03130D), Color(0xFF09281C),
        Color(0xFF1DE9B6), Color(0xFF00C853), Color(0xFF00B8D4), Color(0xFF14D9A5), Color(0xFF64FFDA)),
    UxTheme("midnight", "Midnight", true, Color(0xFF050507), Color(0xFF111116),
        Color(0xFF4A4A62), Color(0xFF2E2E40), Color(0xFF5C5C7A), Color(0xFFE8E8F0), Color(0xFF9A9AB0)),
    UxTheme("frost", "Frost", false, Color(0xFFF1F4FF), Color(0xFFE2E8FF),
        Color(0xFF9DB4FF), Color(0xFFFFB3D9), Color(0xFF9DF0FF), Color(0xFF4E5BFF), Color(0xFF8A5CFF)),
)

val LocalUx = compositionLocalOf { UxThemes.first() }

@Composable
private fun anim(c: Color): Color = animateColorAsState(c, tween(700), label = "theme").value

/** Smoothly cross-fades every color when the theme changes. */
@Composable
fun animatedTheme(t: UxTheme): UxTheme = t.copy(
    bgTop = anim(t.bgTop),
    bgBottom = anim(t.bgBottom),
    blob1 = anim(t.blob1),
    blob2 = anim(t.blob2),
    blob3 = anim(t.blob3),
    accent = anim(t.accent),
    accent2 = anim(t.accent2),
)
