package com.uxis.utility.ui

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Slowly drifting colour blobs that give the glass something to refract. */
@Composable
fun AuroraBackground() {
    val t = LocalUx.current
    val inf = rememberInfiniteTransition(label = "bg")
    val p1 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p1")
    val p2 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(18000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p2")
    val p3 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(22000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p3")
    val strength = if (t.dark) 0.55f else 0.7f
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(t.bgTop, t.bgBottom)))
        fun blob(c: Color, x: Float, y: Float, r: Float) {
            val center = Offset(x, y)
            drawCircle(
                Brush.radialGradient(listOf(c.copy(alpha = strength), c.copy(alpha = 0f)), center = center, radius = r),
                radius = r, center = center,
            )
        }
        val w = size.width
        val h = size.height
        blob(t.blob1, w * (0.1f + 0.6f * p1), h * (0.15f + 0.15f * p2), w * 0.8f)
        blob(t.blob2, w * (0.95f - 0.6f * p2), h * (0.5f + 0.15f * p3), w * 0.7f)
        blob(t.blob3, w * (0.25f + 0.5f * p3), h * (0.95f - 0.2f * p1), w * 0.7f)
    }
}

/** Frosted glass surface: translucent gradient fill + light-catching gradient border + press spring. */
@Composable
fun Glass(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    highlight: Boolean = false,
    solid: Boolean = false,
    contentAlignment: Alignment = Alignment.TopStart,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val t = LocalUx.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(dampingRatio = 0.55f, stiffness = 500f), label = "press")
    val borderStart by animateColorAsState(if (highlight) t.accent else t.borderHi, tween(350), label = "b1")
    val borderEnd by animateColorAsState(if (highlight) t.accent2.copy(alpha = 0.6f) else t.borderLo, tween(350), label = "b2")
    val fill = if (solid) Brush.linearGradient(listOf(t.dialogBg, t.dialogBg))
    else Brush.linearGradient(listOf(t.glassHi, t.glassLo))
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(fill)
            .border(if (highlight) 1.5.dp else 1.dp, Brush.linearGradient(listOf(borderStart, borderEnd)), shape)
            .then(
                if (onClick != null) Modifier.clickable(interaction, indication = null, onClick = onClick)
                else Modifier
            ),
        contentAlignment = contentAlignment,
        content = content,
    )
}

@Composable
fun GlassIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, size: Dp = 44.dp, onClick: () -> Unit) {
    val t = LocalUx.current
    Glass(Modifier.size(size), shape = CircleShape, contentAlignment = Alignment.Center, onClick = onClick) {
        SvgIcon(icon, t.text, 20.dp)
    }
}

@Composable
fun UxText(
    text: String,
    size: TextUnit = 15.sp,
    weight: FontWeight = FontWeight.Normal,
    color: Color = LocalUx.current.text,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign? = null,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text, fontSize = size, fontWeight = weight, color = color,
        maxLines = maxLines, overflow = TextOverflow.Ellipsis, textAlign = align, modifier = modifier,
    )
}

/** The app mark: gradient squircle with a bold rounded "U". */
@Composable
fun LogoMark(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val r = w * 0.26f
        drawRoundRect(
            Brush.linearGradient(
                listOf(Color(0xFF7B5CFF), Color(0xFF4E7BFF), Color(0xFF22C1EE)),
                start = Offset.Zero, end = Offset(w, w),
            ),
            cornerRadius = CornerRadius(r, r),
        )
        drawRoundRect(
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.3f), Color.Transparent), endY = w * 0.6f),
            cornerRadius = CornerRadius(r, r),
        )
        val path = Path().apply {
            moveTo(w * 0.35f, w * 0.29f)
            lineTo(w * 0.35f, w * 0.52f)
            arcTo(Rect(w * 0.35f, w * 0.37f, w * 0.65f, w * 0.67f), 180f, -180f, false)
            lineTo(w * 0.65f, w * 0.29f)
        }
        drawPath(path, Color.White, style = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Animated glass toggle. */
@Composable
fun UxSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val t = LocalUx.current
    val x by animateDpAsState(if (checked) 22.dp else 2.dp, spring(dampingRatio = 0.6f, stiffness = 500f), label = "sw")
    val track by animateColorAsState(if (checked) t.accent else t.chip, tween(250), label = "track")
    Box(
        Modifier
            .size(width = 50.dp, height = 30.dp)
            .clip(CircleShape)
            .background(track)
            .border(1.dp, t.borderHi.copy(alpha = 0.4f), CircleShape)
            .clickable(remember { MutableInteractionSource() }, indication = null) { onChange(!checked) }
    ) {
        Box(
            Modifier
                .offset(x = x, y = 2.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
