package com.sketch.launcher.sketch

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Hand-drawn geometry, loosely after rough.js: every stroke is a slightly bowed
 * bezier that overshoots its endpoints, usually drawn twice so it looks inked.
 * All randomness comes from the [Random] passed in, so a given seed always
 * produces the same wobble (no flicker on recomposition).
 */
object Rough {

    private fun Random.jitter(amount: Float) = (nextFloat() * 2f - 1f) * amount

    /** One wobbly stroke from (x1, y1) to (x2, y2). [px] is the screen density. */
    fun line(
        path: Path,
        x1: Float, y1: Float, x2: Float, y2: Float,
        rnd: Random, roughness: Float, px: Float,
    ) {
        val dx = x2 - x1
        val dy = y2 - y1
        val len = sqrt(dx * dx + dy * dy)
        if (len < 0.5f || roughness <= 0f) {
            path.moveTo(x1, y1)
            path.lineTo(x2, y2)
            return
        }
        val ux = dx / len
        val uy = dy / len
        val nx = -uy
        val ny = ux

        val wobble = 0.9f * px * roughness
        val overshoot = min(len * 0.04f, 3f * px) * roughness
        val bend = (min(len * 0.03f, 3.2f * px) + 0.4f * px) * roughness

        val so = -overshoot * rnd.nextFloat()
        val eo = overshoot * rnd.nextFloat()
        val sp = rnd.jitter(wobble)
        val ep = rnd.jitter(wobble)
        val sx = x1 + ux * so + nx * sp
        val sy = y1 + uy * so + ny * sp
        val ex = x2 + ux * eo + nx * ep
        val ey = y2 + uy * eo + ny * ep

        val bow = rnd.jitter(bend)
        val t1 = 0.3f + rnd.jitter(0.08f)
        val t2 = 0.7f + rnd.jitter(0.08f)
        val b1 = bow + rnd.jitter(bend * 0.4f)
        val b2 = bow + rnd.jitter(bend * 0.4f)

        path.moveTo(sx, sy)
        path.cubicTo(
            x1 + dx * t1 + nx * b1, y1 + dy * t1 + ny * b1,
            x1 + dx * t2 + nx * b2, y1 + dy * t2 + ny * b2,
            ex, ey,
        )
    }

    fun rect(
        path: Path, l: Float, t: Float, r: Float, b: Float,
        rnd: Random, roughness: Float, px: Float, passes: Int = 2,
    ) {
        repeat(passes) {
            line(path, l, t, r, t, rnd, roughness, px)
            line(path, r, t, r, b, rnd, roughness, px)
            line(path, r, b, l, b, rnd, roughness, px)
            line(path, l, b, l, t, rnd, roughness, px)
        }
    }

    /** A closed, slightly lumpy outline suitable for filling a rectangle. */
    fun rectFill(path: Path, l: Float, t: Float, r: Float, b: Float, rnd: Random, roughness: Float, px: Float) {
        val j = 1.5f * px * roughness
        path.moveTo(l + rnd.jitter(j), t + rnd.jitter(j))
        path.lineTo(r + rnd.jitter(j), t + rnd.jitter(j))
        path.lineTo(r + rnd.jitter(j), b + rnd.jitter(j))
        path.lineTo(l + rnd.jitter(j), b + rnd.jitter(j))
        path.close()
    }

    /**
     * A circle drawn the way people draw circles: start anywhere, go round, and
     * overshoot past the starting point a little.
     */
    fun ellipse(
        path: Path, cx: Float, cy: Float, rx: Float, ry: Float,
        rnd: Random, roughness: Float, px: Float, passes: Int = 2, closed: Boolean = false,
    ) {
        repeat(passes) {
            val count = ((rx + ry) * PI / (14f * px)).toInt().coerceIn(10, 48)
            val start = rnd.nextFloat() * 2f * PI.toFloat()
            val overlap = if (closed) 0f else 0.25f + rnd.nextFloat() * 0.35f * roughness.coerceAtMost(1.5f)
            val total = 2f * PI.toFloat() + overlap
            val radial = 0.045f * roughness
            val points = (0..count).map { i ->
                val a = start + total * i / count
                val k = 1f + rnd.jitter(radial)
                Offset(cx + cos(a) * rx * k, cy + sin(a) * ry * k)
            }
            smooth(path, if (closed) points.dropLast(1) + points.first() else points)
        }
    }

    /** Diagonal hatching that covers a w*h box; clip it to the shape you want filled. */
    fun hatch(path: Path, w: Float, h: Float, gap: Float, rnd: Random, roughness: Float, px: Float) {
        var k = -h
        while (k < w) {
            line(path, k, h, k + h, 0f, rnd, roughness * 0.6f, px)
            k += gap
        }
    }

    /** Catmull-Rom spline through [pts], emitted as cubic beziers. */
    fun smooth(path: Path, pts: List<Offset>) {
        if (pts.isEmpty()) return
        path.moveTo(pts[0].x, pts[0].y)
        for (i in 0 until pts.size - 1) {
            val p0 = pts[max(i - 1, 0)]
            val p1 = pts[i]
            val p2 = pts[i + 1]
            val p3 = pts[min(i + 2, pts.size - 1)]
            val c1 = p1 + (p2 - p0) / 6f
            val c2 = p2 - (p3 - p1) / 6f
            path.cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
        }
    }
}
