package com.sketch.launcher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sketch.launcher.data.ClockStyle
import com.sketch.launcher.sketch.LocalSketch
import com.sketch.launcher.sketch.Rough
import com.sketch.launcher.sketch.SketchFonts
import com.sketch.launcher.sketch.SketchShape
import com.sketch.launcher.sketch.SketchText
import com.sketch.launcher.sketch.sketch
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DayTextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** The current time, refreshed on each minute boundary. */
@Composable
fun rememberNow(): State<LocalDateTime> = produceState(LocalDateTime.now()) {
    while (true) {
        value = LocalDateTime.now()
        delay(60_000L - System.currentTimeMillis() % 60_000L + 50L)
    }
}

@Composable
fun ClockWidget(style: ClockStyle, use24h: Boolean, modifier: Modifier = Modifier) {
    val now by rememberNow()
    val p = LocalSketch.current.palette
    when (style) {
        ClockStyle.DIGITAL -> Row(modifier, verticalAlignment = Alignment.Bottom) {
            val pattern = if (use24h) "HH:mm" else "h:mm"
            SketchText(now.format(DateTimeFormatter.ofPattern(pattern)), size = 92.sp, font = SketchFonts.display)
            if (!use24h) {
                SketchText(
                    now.format(DateTimeFormatter.ofPattern("a", Locale.ENGLISH)).lowercase(),
                    Modifier.padding(start = 8.dp, bottom = 22.dp),
                    size = 24.sp,
                    color = p.muted,
                )
            }
        }
        ClockStyle.WORDS -> Column(modifier) {
            SketchText("it's", size = 22.sp, color = p.muted)
            SketchText(timeInWords(now.hour, now.minute), size = 52.sp, font = SketchFonts.display)
        }
        ClockStyle.ANALOG -> AnalogClock(now, modifier.size(170.dp))
    }
}

@Composable
private fun AnalogClock(now: LocalDateTime, modifier: Modifier) {
    val style = LocalSketch.current
    val p = style.palette
    val frameState = style.frame
    Box(
        modifier.drawWithCache {
            val rnd = Random(9 + frameState.value)
            val px = density
            val c = size.width / 2f
            val r = c - 6.dp.toPx()
            val face = Path()
            Rough.ellipse(face, c, c, r, r, rnd, style.roughness, px, 2)
            for (i in 0 until 12) {
                val a = i * PI.toFloat() / 6f
                val inner = if (i % 3 == 0) r * 0.78f else r * 0.86f
                Rough.line(face, c + cos(a) * inner, c + sin(a) * inner, c + cos(a) * r * 0.93f, c + sin(a) * r * 0.93f, rnd, style.roughness * 0.5f, px)
            }
            val hands = Path()
            val minuteA = (now.minute / 60f) * 2f * PI.toFloat() - PI.toFloat() / 2f
            val hourA = ((now.hour % 12 + now.minute / 60f) / 12f) * 2f * PI.toFloat() - PI.toFloat() / 2f
            Rough.line(hands, c, c, c + cos(hourA) * r * 0.5f, c + sin(hourA) * r * 0.5f, rnd, style.roughness * 0.6f, px)
            Rough.line(hands, c, c, c + cos(minuteA) * r * 0.78f, c + sin(minuteA) * r * 0.78f, rnd, style.roughness * 0.6f, px)
            val dot = Path()
            Rough.ellipse(dot, c, c, 4.dp.toPx(), 4.dp.toPx(), rnd, 0.4f, px, 2)
            val thin = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            val thick = Stroke(3.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            onDrawBehind {
                drawPath(face, p.ink, style = thin)
                drawPath(hands, p.ink, style = thick)
                drawPath(dot, p.accent, style = thick)
            }
        },
    )
}

fun timeInWords(hour24: Int, minute: Int): String {
    val names = listOf("twelve", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven")
    var rounded = (minute + 2) / 5 * 5
    var hour = hour24 % 12
    if (rounded == 60) {
        rounded = 0
        hour = (hour + 1) % 12
    }
    val current = names[hour]
    val next = names[(hour + 1) % 12]
    fun words(m: Int) = when (m) {
        5 -> "five"; 10 -> "ten"; 20 -> "twenty"; 25 -> "twenty-five"; else -> ""
    }
    return when (rounded) {
        0 -> "$current o'clock"
        15 -> "quarter past $current"
        30 -> "half past $current"
        45 -> "quarter to $next"
        in 5..25 -> "${words(rounded)} past $current"
        else -> "${words(60 - rounded)} to $next"
    }
}

@Composable
fun DateLine(modifier: Modifier = Modifier) {
    val now by rememberNow()
    SketchText(now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")), modifier, size = 24.sp)
}

private data class BatteryState(val percent: Int, val charging: Boolean)

@Composable
private fun rememberBattery(): State<BatteryState> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(BatteryState(100, false)) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                if (level >= 0 && scale > 0) {
                    state.value = BatteryState(
                        percent = level * 100 / scale,
                        charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
                    )
                }
            }
        }
        // ACTION_BATTERY_CHANGED is sticky, so registering also delivers the current value.
        ContextCompat.registerReceiver(context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return state
}

@Composable
fun BatteryWidget(modifier: Modifier = Modifier) {
    val battery by rememberBattery()
    val style = LocalSketch.current
    val p = style.palette
    val frameState = style.frame
    val level = battery.percent / 100f
    val fillColor = if (battery.percent <= 15 && !battery.charging) p.accent else p.ink
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(42.dp)
                .height(20.dp)
                .drawWithCache {
                    val rnd = Random(3 + frameState.value)
                    val px = density
                    val nub = 4.dp.toPx()
                    val body = size.width - nub
                    val outline = Path()
                    Rough.rect(outline, 1.dp.toPx(), 1.dp.toPx(), body, size.height - 1.dp.toPx(), rnd, style.roughness * 0.6f, px)
                    Rough.line(outline, body + nub * 0.6f, size.height * 0.32f, body + nub * 0.6f, size.height * 0.68f, rnd, 0.3f, px)
                    val hatch = Path()
                    val inner = 4.dp.toPx()
                    val filled = (body - inner * 2) * level
                    var x = inner
                    while (x < inner + filled) {
                        Rough.line(hatch, x, size.height - inner, x + 3.dp.toPx(), inner, rnd, 0.3f, px)
                        x += 3.dp.toPx()
                    }
                    val stroke = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round)
                    onDrawBehind {
                        drawPath(outline, p.ink, style = stroke)
                        drawPath(hatch, fillColor, style = stroke)
                    }
                },
        )
        Spacer(Modifier.width(10.dp))
        SketchText("${battery.percent}%" + if (battery.charging) "  · charging" else "", size = 20.sp, color = p.muted)
    }
}

@Composable
fun CalendarWidget(modifier: Modifier = Modifier) {
    val now by rememberNow()
    val p = LocalSketch.current.palette
    val today = now.toLocalDate()
    val firstDay = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val monthStart = LocalDate.of(today.year, today.month, 1)
    val lead = (monthStart.dayOfWeek.value - firstDay.value + 7) % 7
    val days = today.lengthOfMonth()
    val cells = List(lead) { 0 } + (1..days).toList()

    Column(
        modifier
            .fillMaxWidth()
            .sketch(seed = 41)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        SketchText(
            today.month.getDisplayName(DayTextStyle.FULL, Locale.getDefault()) + " " + today.year,
            size = 28.sp,
            font = SketchFonts.display,
        )
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            for (i in 0 until 7) {
                val dow = firstDay.plus(i.toLong())
                SketchText(
                    dow.getDisplayName(DayTextStyle.NARROW, Locale.getDefault()),
                    Modifier.weight(1f),
                    size = 16.sp,
                    color = p.muted,
                    align = TextAlign.Center,
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                for (i in 0 until 7) {
                    val day = week.getOrNull(i) ?: 0
                    Box(Modifier.weight(1f).aspectRatio(1.25f), contentAlignment = Alignment.Center) {
                        if (day == today.dayOfMonth) {
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .sketch(SketchShape.Ellipse, color = p.accent, stroke = 2.dp, seed = day),
                            )
                        }
                        if (day > 0) {
                            SketchText(day.toString(), size = 17.sp, color = if (day < today.dayOfMonth) p.muted else p.ink)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoteWidget(text: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val p = LocalSketch.current.palette
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .rotate(-1.2f)
            .sketch(seed = 77, fill = p.highlight, color = p.ink.copy(alpha = 0.7f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        val textStyle = TextStyle(color = p.ink, fontSize = 21.sp, fontFamily = SketchFonts.hand)
        BasicTextField(
            value = text,
            onValueChange = onChange,
            textStyle = textStyle,
            cursorBrush = SolidColor(p.accent),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box {
                        if (text.isEmpty()) SketchText("jot something down…", size = 21.sp, color = p.ink.copy(alpha = 0.45f))
                        inner()
                    }
                }
            },
        )
    }
}
