package com.sketch.launcher.ui

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.sketch.launcher.LauncherViewModel
import com.sketch.launcher.Screen
import com.sketch.launcher.sketch.Glyph
import com.sketch.launcher.sketch.LocalSketch
import com.sketch.launcher.sketch.SketchFonts
import com.sketch.launcher.sketch.SketchGlyph
import com.sketch.launcher.sketch.SketchText
import com.sketch.launcher.sketch.sketch
import com.sketch.launcher.sketch.tap
import com.sketch.launcher.widgets.WidgetController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private class ProviderGroup(val appLabel: String, val icon: ImageBitmap?, val widgets: List<ProviderItem>)
private class ProviderItem(val info: AppWidgetProviderInfo, val label: String, val cells: String)

@Composable
fun WidgetPickerScreen(vm: LauncherViewModel, widgets: WidgetController) {
    val context = LocalContext.current
    val p = LocalSketch.current.palette

    val groups by produceState<List<ProviderGroup>?>(null) {
        value = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val density = context.resources.displayMetrics.density
            val iconPx = (32 * density).toInt()
            widgets.providers()
                .groupBy { it.provider.packageName }
                .map { (pkg, infos) ->
                    val appInfo = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull()
                    ProviderGroup(
                        appLabel = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: pkg,
                        icon = appInfo?.let { runCatching { pm.getApplicationIcon(it).toBitmap(iconPx, iconPx).asImageBitmap() }.getOrNull() },
                        widgets = infos.map { info ->
                            // Rough home-screen cell count (Android's guideline: n cells ≈ 70n − 30 dp).
                            val w = ((info.minWidth / density + 30) / 70).toInt().coerceAtLeast(1)
                            val h = ((info.minHeight / density + 30) / 70).toInt().coerceAtLeast(1)
                            ProviderItem(info, info.loadLabel(pm), "$w × $h")
                        },
                    )
                }
                .sortedBy { it.appLabel.lowercase() }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 24.dp),
    ) {
        Row(Modifier.padding(top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            SketchText("Widgets", Modifier.weight(1f), size = 48.sp, font = SketchFonts.display)
            Box(Modifier.size(46.dp).tap { vm.go(Screen.HOME) }, contentAlignment = Alignment.Center) {
                SketchGlyph(Glyph.Close, Modifier.size(26.dp))
            }
        }
        val list = groups
        when {
            list == null -> SketchText("looking for widgets…", size = 22.sp, color = p.muted)
            list.isEmpty() -> SketchText("No app on this phone offers widgets.", size = 22.sp, color = p.muted)
            else -> LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
                list.forEach { group ->
                    item(key = "g:" + group.appLabel + group.hashCode()) {
                        Row(Modifier.padding(top = 18.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            group.icon?.let {
                                Image(it, contentDescription = null, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(10.dp))
                            }
                            SketchText(group.appLabel, size = 28.sp, font = SketchFonts.display, color = p.accent)
                        }
                    }
                    items(group.widgets, key = { it.info.provider.flattenToString() + it.info.profile.hashCode() }) { item ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .sketch(seed = item.label.hashCode())
                                .tap { widgets.pick(item.info) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SketchText(item.label, Modifier.weight(1f), size = 21.sp, maxLines = 2)
                            SketchText(item.cells, size = 18.sp, color = p.muted)
                        }
                    }
                }
            }
        }
    }
}
