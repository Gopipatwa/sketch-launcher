package com.sketch.launcher.sketch

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.random.Random

enum class SketchShape { Rect, Ellipse, Underline }

/**
 * Draws a hand-drawn outline (and optional fill / hatching) behind the content.
 * [color] defaults to the theme ink; pass [Color.Transparent] for fill only.
 */
fun Modifier.sketch(
    shape: SketchShape = SketchShape.Rect,
    color: Color = Color.Unspecified,
    fill: Color = Color.Transparent,
    hatch: Color = Color.Transparent,
    seed: Int = 1,
    stroke: Dp = 1.6.dp,
    inset: Dp = 2.dp,
    passes: Int = 2,
): Modifier = composed {
    val style = LocalSketch.current
    val ink = if (color == Color.Unspecified) style.palette.ink else color
    val roughness = style.roughness
    val frameState = style.frame
    drawWithCache {
        val frame = frameState.value
        val rnd = Random(seed * 7919 + frame * 104729)
        val px = density
        val ins = inset.toPx()
        val sw = stroke.toPx()
        val l = ins
        val t = ins
        val r = size.width - ins
        val b = size.height - ins

        val outline = Path()
        val body = Path()
        val hatching = Path()
        when (shape) {
            SketchShape.Rect -> {
                Rough.rect(outline, l, t, r, b, rnd, roughness, px, passes)
                Rough.rectFill(body, l, t, r, b, rnd, roughness, px)
            }
            SketchShape.Ellipse -> {
                val cx = size.width / 2f
                val cy = size.height / 2f
                Rough.ellipse(outline, cx, cy, cx - ins, cy - ins, rnd, roughness, px, passes)
                Rough.ellipse(body, cx, cy, cx - ins, cy - ins, rnd, roughness * 0.5f, px, 1, closed = true)
            }
            SketchShape.Underline -> Rough.line(outline, l, b, r, b, rnd, roughness, px)
        }
        if (hatch.alpha > 0f) Rough.hatch(hatching, size.width, size.height, 7.dp.toPx(), rnd, roughness, px)

        val outlineStroke = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val hatchStroke = Stroke(sw * 0.7f, cap = StrokeCap.Round)
        onDrawBehind {
            if (fill.alpha > 0f) drawPath(body, fill)
            if (hatch.alpha > 0f) clipPath(body) { drawPath(hatching, hatch, style = hatchStroke) }
            if (ink.alpha > 0f && sw > 0f) drawPath(outline, ink, style = outlineStroke)
        }
    }
}

/** Fills the background with the theme paper and its pattern (ruled, grid, dots). */
fun Modifier.paper(): Modifier = composed {
    val p = LocalSketch.current.palette
    drawWithCache {
        val lineStroke = 1.dp.toPx()
        val ruledGap = 34.dp.toPx()
        val gridGap = 22.dp.toPx()
        val width = size.width
        val height = size.height
        val dots = if (p.pattern == PaperPattern.DOTS) buildList {
            var y = gridGap / 2
            while (y < height) {
                var x = gridGap / 2
                while (x < width) {
                    add(Offset(x, y))
                    x += gridGap
                }
                y += gridGap
            }
        } else emptyList()
        onDrawBehind {
            drawRect(p.paper)
            when (p.pattern) {
                PaperPattern.RULED -> {
                    var y = ruledGap * 2
                    while (y < size.height) {
                        drawLine(p.patternColor, Offset(0f, y), Offset(size.width, y), lineStroke)
                        y += ruledGap
                    }
                    val margin = 36.dp.toPx()
                    drawLine(p.accent.copy(alpha = 0.35f), Offset(margin, 0f), Offset(margin, size.height), lineStroke)
                }
                PaperPattern.GRID -> {
                    var x = 0f
                    while (x < size.width) {
                        drawLine(p.patternColor, Offset(x, 0f), Offset(x, size.height), lineStroke)
                        x += gridGap
                    }
                    var y = 0f
                    while (y < size.height) {
                        drawLine(p.patternColor, Offset(0f, y), Offset(size.width, y), lineStroke)
                        y += gridGap
                    }
                }
                PaperPattern.DOTS -> drawPoints(dots, PointMode.Points, p.patternColor, 2.6.dp.toPx(), StrokeCap.Round)
                PaperPattern.NONE -> Unit
            }
        }
    }
}

@Composable
fun SketchText(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 20.sp,
    color: Color = Color.Unspecified,
    font: FontFamily = SketchFonts.hand,
    align: TextAlign = TextAlign.Start,
    maxLines: Int = Int.MAX_VALUE,
) {
    val ink = if (color == Color.Unspecified) LocalSketch.current.palette.ink else color
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(color = ink, fontSize = size, fontFamily = font, textAlign = align),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Clickable without the grey ripple, which looks out of place on paper. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.tap(onLongClick: (() -> Unit)? = null, onClick: () -> Unit): Modifier = composed {
    combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onLongClick = onLongClick,
        onClick = onClick,
    )
}

@Composable
fun SketchButton(
    text: String,
    modifier: Modifier = Modifier,
    seed: Int = text.hashCode(),
    filled: Boolean = false,
    onClick: () -> Unit,
) {
    val p = LocalSketch.current.palette
    Box(
        modifier
            .sketch(seed = seed, fill = if (filled) p.highlight else Color.Transparent)
            .tap(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        SketchText(text, size = 19.sp)
    }
}

@Composable
fun SketchChip(text: String, selected: Boolean, seed: Int = text.hashCode(), onClick: () -> Unit) {
    val p = LocalSketch.current.palette
    Box(
        Modifier
            .sketch(
                seed = seed,
                color = if (selected) p.ink else p.muted,
                hatch = if (selected) p.accent.copy(alpha = 0.45f) else Color.Transparent,
                stroke = if (selected) 2.dp else 1.4.dp,
            )
            .tap(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        SketchText(text, size = 18.sp, color = if (selected) p.ink else p.muted)
    }
}

@Composable
fun SketchToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .tap { onChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SketchText(label, Modifier.weight(1f), size = 20.sp)
        Box(Modifier.size(28.dp).sketch(seed = label.hashCode()), contentAlignment = Alignment.Center) {
            if (checked) SketchGlyph(Glyph.Check, Modifier.size(22.dp), color = LocalSketch.current.palette.accent)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    val p = LocalSketch.current.palette
    Column(modifier.padding(top = 22.dp, bottom = 8.dp)) {
        SketchText(text, size = 30.sp, font = SketchFonts.display)
        Spacer(
            Modifier
                .width(64.dp)
                .height(6.dp)
                .sketch(SketchShape.Underline, color = p.accent, seed = text.hashCode(), stroke = 2.4.dp, inset = 1.dp),
        )
    }
}

@Composable
fun SketchDialog(onDismiss: () -> Unit, seed: Int = 7, content: @Composable ColumnScope.() -> Unit) {
    val style = LocalSketch.current
    Dialog(onDismissRequest = onDismiss) {
        // Dialogs live in their own window, so the theme has to be passed on explicitly.
        androidx.compose.runtime.CompositionLocalProvider(LocalSketch provides style) {
            Column(
                Modifier
                    .widthIn(min = 260.dp, max = 400.dp)
                    .paper()
                    .sketch(seed = seed, stroke = 2.dp)
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                content = content,
            )
        }
    }
}

/** A single row in a menu dialog. */
@Composable
fun MenuItem(text: String, color: Color = Color.Unspecified, onClick: () -> Unit) {
    SketchText(
        text,
        Modifier
            .fillMaxWidth()
            .tap(onClick = onClick)
            .padding(vertical = 10.dp),
        size = 22.sp,
        color = color,
    )
}
