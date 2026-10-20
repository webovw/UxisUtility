package com.uxis.utility.ui

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Line icons defined as SVG path data (24x24 viewport, Lucide-like style). */
object UxIcons {
    private fun svg(vararg paths: String): ImageVector {
        val b = ImageVector.Builder(
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        )
        paths.forEach {
            b.addPath(
                pathData = addPathNodes(it),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }

    private const val CIRCLE = "M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0"

    val Power by lazy { svg("M12 3v8", "M17.7 6.6a8 8 0 1 1 -11.4 0") }
    val Home by lazy { svg("M3 10.5L12 3l9 7.5", "M5.5 9v11h13V9", "M10 20v-5h4v5") }
    val Servers by lazy { svg("M4 4h16v6H4z", "M4 14h16v6H4z", "M8 7h.01", "M8 17h.01", "M12 7h4", "M12 17h4") }
    val Sliders by lazy { svg("M4 7h9", "M17 7h3", "M15 5v4", "M4 17h3", "M11 17h9", "M9 15v4") }
    val Plus by lazy { svg("M12 5v14", "M5 12h14") }
    val Clipboard by lazy { svg("M9 3h6v4H9z", "M9 5H6.5A1.5 1.5 0 0 0 5 6.5v13A1.5 1.5 0 0 0 6.5 21h11a1.5 1.5 0 0 0 1.5 -1.5v-13A1.5 1.5 0 0 0 17.5 5H15") }
    val Trash by lazy { svg("M4 7h16", "M10 11v6", "M14 11v6", "M6 7l1 13h10l1 -13", "M9 7V4h6v3") }
    val Zap by lazy { svg("M13 2L4 14h7l-1 8 9 -12h-7z") }
    val ArrowDown by lazy { svg("M12 5v14", "M6 13l6 6 6 -6") }
    val ArrowUp by lazy { svg("M12 19V5", "M6 11l6 -6 6 6") }
    val Check by lazy { svg("M5 12.5l4.5 4.5L19 7.5") }
    val Globe by lazy { svg(CIRCLE, "M3 12h18", "M12 3c3.5 3.5 3.5 14.5 0 18", "M12 3c-3.5 3.5 -3.5 14.5 0 18") }
    val Shield by lazy { svg("M12 3l8 3v6c0 5 -3.5 8 -8 9c-4.5 -1 -8 -4 -8 -9V6z", "M9 12l2 2 4 -4") }
    val Palette by lazy {
        svg(
            "M12 3a9 9 0 1 0 0 18c1 0 1.6 -.8 1.6 -1.6c0 -1.1 -1 -1.4 -1 -2.4c0 -.9 .7 -1.5 1.6 -1.5H16a5 5 0 0 0 5 -5c0 -4.1 -4 -7.5 -9 -7.5z",
            "M7.5 11h.01", "M10 7.5h.01", "M15 7.5h.01",
        )
    }
    val Chevron by lazy { svg("M9 6l6 6 -6 6") }
    val Close by lazy { svg("M6 6l12 12", "M18 6L6 18") }
    val Refresh by lazy { svg("M20 12a8 8 0 1 1 -2.3 -5.7", "M20 4v5h-5") }
    val Link by lazy {
        svg(
            "M10 14a4 4 0 0 0 5.7 0l3 -3a4 4 0 0 0 -5.7 -5.7l-1 1",
            "M14 10a4 4 0 0 0 -5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1 -1",
        )
    }
    val Info by lazy { svg(CIRCLE, "M12 11v5", "M12 8h.01") }
    val Block by lazy { svg(CIRCLE, "M5.6 5.6l12.8 12.8") }
    val Network by lazy { svg("M12 3v6", "M5 15v-3h14v3", "M12 9v3", "M3 15h4v5H3z", "M10 15h4v5h-4z", "M17 15h4v5h-4z") }
}

@Composable
fun SvgIcon(icon: ImageVector, tint: Color, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Icon(imageVector = icon, contentDescription = null, modifier = modifier.size(size), tint = tint)
}
