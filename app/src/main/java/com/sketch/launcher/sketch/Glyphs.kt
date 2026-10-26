package com.sketch.launcher.sketch

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Small doodled icons, so the launcher needs no icon font or image assets. */
enum class Glyph { Gear, Search, ArrowUp, Plus, Dots, Close, Check, Pencil }

@Composable
fun SketchGlyph(
    glyph: Glyph,
    modifier: Modifier = Modifier.size(24.dp),
    color: Color = Color.Unspecified,
    seed: Int = glyph.ordinal,
) {
    val style = LocalSketch.current
    val ink = if (color == Color.Unspecified) style.palette.ink else color
    val roughness = style.roughness * 0.7f
    val frameState = style.frame
    Spacer(
        modifier.drawWithCache {
            val rnd = Random(seed * 131 + frameState.value * 17)
            val px = density
            val s = min(size.width, size.height)
            val c = s / 2f
            val path = Path()
            fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
                Rough.line(path, x1 * s, y1 * s, x2 * s, y2 * s, rnd, roughness, px)
            fun circle(cx: Float, cy: Float, r: Float, passes: Int = 1) =
                Rough.ellipse(path, cx * s, cy * s, r * s, r * s, rnd, roughness, px, passes)

            when (glyph) {
                Glyph.Gear -> {
                    circle(0.5f, 0.5f, 0.24f)
                    circle(0.5f, 0.5f, 0.09f)
                    for (i in 0 until 8) {
                        val a = i * PI.toFloat() / 4f
                        line(0.5f + cos(a) * 0.27f, 0.5f + sin(a) * 0.27f, 0.5f + cos(a) * 0.42f, 0.5f + sin(a) * 0.42f)
                    }
                }
                Glyph.Search -> {
                    circle(0.42f, 0.42f, 0.25f)
                    line(0.6f, 0.6f, 0.86f, 0.86f)
                }
                Glyph.ArrowUp -> {
                    line(0.2f, 0.64f, 0.5f, 0.34f)
                    line(0.5f, 0.34f, 0.8f, 0.64f)
                }
                Glyph.Plus -> {
                    line(0.5f, 0.18f, 0.5f, 0.82f)
                    line(0.18f, 0.5f, 0.82f, 0.5f)
                }
                Glyph.Dots -> {
                    circle(0.2f, 0.5f, 0.05f, 2)
                    circle(0.5f, 0.5f, 0.05f, 2)
                    circle(0.8f, 0.5f, 0.05f, 2)
                }
                Glyph.Close -> {
                    line(0.22f, 0.22f, 0.78f, 0.78f)
                    line(0.78f, 0.22f, 0.22f, 0.78f)
                }
                Glyph.Check -> {
                    line(0.15f, 0.55f, 0.42f, 0.8f)
                    line(0.42f, 0.8f, 0.88f, 0.18f)
                }
                Glyph.Pencil -> {
                    line(0.22f, 0.78f, 0.72f, 0.28f)
                    line(0.32f, 0.88f, 0.82f, 0.38f)
                    line(0.72f, 0.28f, 0.82f, 0.38f)
                    line(0.22f, 0.78f, 0.14f, 0.92f)
                    line(0.14f, 0.92f, 0.32f, 0.88f)
                }
            }
            val stroke = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            onDrawBehind { drawPath(path, ink, style = stroke) }
        },
    )
}
