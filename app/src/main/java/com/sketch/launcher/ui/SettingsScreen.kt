package com.sketch.launcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.sketch.launcher.BuildConfig
import com.sketch.launcher.LauncherViewModel
import com.sketch.launcher.Screen
import com.sketch.launcher.data.ClockStyle
import com.sketch.launcher.data.IconStyle
import com.sketch.launcher.data.Wobble
import com.sketch.launcher.sketch.Glyph
import com.sketch.launcher.sketch.LocalSketch
import com.sketch.launcher.sketch.SectionTitle
import com.sketch.launcher.sketch.SketchButton
import com.sketch.launcher.sketch.SketchChip
import com.sketch.launcher.sketch.SketchFonts
import com.sketch.launcher.sketch.SketchGlyph
import com.sketch.launcher.sketch.SketchPalette
import com.sketch.launcher.sketch.SketchShape
import com.sketch.launcher.sketch.SketchStyle
import com.sketch.launcher.sketch.SketchText
import com.sketch.launcher.sketch.SketchThemes
import com.sketch.launcher.sketch.SketchToggle
import com.sketch.launcher.sketch.paper
import com.sketch.launcher.sketch.sketch
import com.sketch.launcher.sketch.tap
import com.sketch.launcher.widgets.WidgetController

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: LauncherViewModel, widgets: WidgetController) {
    val s = vm.settings
    val p = LocalSketch.current.palette
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    var isDefault by remember { mutableStateOf(true) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            isDefault = SystemActions.isDefaultLauncher(context)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SketchText("Settings", Modifier.weight(1f), size = 48.sp, font = SketchFonts.display)
            Box(Modifier.size(46.dp).tap { vm.go(Screen.HOME) }, contentAlignment = Alignment.Center) {
                SketchGlyph(Glyph.Close, Modifier.size(26.dp))
            }
        }

        if (!isDefault) {
            Spacer(Modifier.height(12.dp))
            SketchButton("Make Sketch Launcher my home screen", Modifier.fillMaxWidth(), filled = true) {
                SystemActions.start(context, SystemActions.defaultLauncherIntent(context))
            }
        }

        SectionTitle("Theme")
        SketchThemes.all.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { theme ->
                    ThemeCard(theme, selected = theme.id == s.themeId, modifier = Modifier.weight(1f)) {
                        vm.update { it.copy(themeId = theme.id) }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        SectionTitle("Sketchiness")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Wobble.entries.forEach { w ->
                SketchChip(w.title, s.wobble == w) { vm.update { it.copy(wobble = w) } }
            }
        }
        SketchToggle("Boiling lines (animated wobble)", s.boilingLines) { v -> vm.update { it.copy(boilingLines = v) } }

        SectionTitle("Clock")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ClockStyle.entries.forEach { c ->
                SketchChip(c.title, s.clockStyle == c) { vm.update { it.copy(clockStyle = c) } }
            }
        }
        SketchToggle("24-hour time", s.use24h) { v -> vm.update { it.copy(use24h = v) } }

        SectionTitle("Home widgets")
        SketchToggle("Date", s.showDate) { v -> vm.update { it.copy(showDate = v) } }
        SketchToggle("Battery", s.showBattery) { v -> vm.update { it.copy(showBattery = v) } }
        SketchToggle("Calendar", s.showCalendar) { v -> vm.update { it.copy(showCalendar = v) } }
        SketchToggle("Sticky note", s.showNote) { v -> vm.update { it.copy(showNote = v) } }

        SectionTitle("App widgets")
        if (s.widgetIds.isEmpty()) {
            SketchText("Add widgets from any installed app.", size = 19.sp, color = p.muted)
        }
        s.widgetIds.forEach { id ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                SketchText(widgets.label(id), Modifier.weight(1f), size = 20.sp, maxLines = 1)
                Box(Modifier.size(40.dp).tap { widgets.remove(id) }, contentAlignment = Alignment.Center) {
                    SketchGlyph(Glyph.Close, Modifier.size(20.dp), color = p.accent, seed = id)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        SketchButton("+ Add a widget") { vm.go(Screen.WIDGET_PICKER) }

        SectionTitle("Icons")
        SketchText("On the home screen", size = 19.sp, color = p.muted)
        IconStylePicker(s.homeIcons) { v -> vm.update { it.copy(homeIcons = v) } }
        Spacer(Modifier.height(10.dp))
        SketchText("In the app drawer", size = 19.sp, color = p.muted)
        IconStylePicker(s.drawerIcons) { v -> vm.update { it.copy(drawerIcons = v) } }

        SectionTitle("App drawer")
        SketchToggle("Open keyboard for search", s.autoKeyboard) { v -> vm.update { it.copy(autoKeyboard = v) } }

        if (s.hidden.isNotEmpty()) {
            SectionTitle("Hidden apps")
            s.hidden.sorted().forEach { key ->
                val app = vm.appsByKey[key]
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    SketchText(app?.label ?: key.substringBefore('/'), Modifier.weight(1f), size = 20.sp, maxLines = 1)
                    SketchChip("show", false, seed = key.hashCode()) { vm.unhide(key) }
                }
            }
        }

        SectionTitle("Gestures")
        SketchText(
            "Swipe up — all apps\nSwipe down — notifications\nLong-press home — menu\nLong-press an app — pin, hide, uninstall",
            size = 19.sp,
            color = p.muted,
        )

        SectionTitle("About")
        SketchText(
            "Sketch Launcher ${BuildConfig.VERSION_NAME}\nHandwriting: Patrick Hand & Caveat (SIL Open Font License)",
            size = 17.sp,
            color = p.muted,
        )
        Spacer(Modifier.height(40.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconStylePicker(selected: IconStyle, onPick: (IconStyle) -> Unit) {
    FlowRow(
        Modifier.padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IconStyle.entries.forEach { style ->
            SketchChip(style.title, selected == style, seed = style.ordinal * 13 + selected.ordinal) { onPick(style) }
        }
    }
}

/** A tiny page of the theme's paper, drawn with the theme's own palette. */
@Composable
private fun ThemeCard(theme: SketchPalette, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val current = LocalSketch.current
    val preview = remember(theme, current.roughness) { SketchStyle(theme, current.roughness, current.frame) }
    CompositionLocalProvider(LocalSketch provides preview) {
        Box(
            modifier
                .aspectRatio(0.95f)
                .paper()
                .sketch(seed = theme.id.hashCode(), stroke = if (selected) 3.dp else 1.6.dp)
                .tap(onClick = onClick)
                .padding(12.dp),
        ) {
            Column {
                SketchText("10:24", size = 38.sp, font = SketchFonts.display)
                SketchText(theme.name, size = 18.sp)
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Swatch(theme.accent, 1)
                    Swatch(theme.highlight, 2)
                    Swatch(theme.muted, 3)
                }
            }
            if (selected) {
                SketchGlyph(Glyph.Check, Modifier.align(Alignment.TopEnd).size(28.dp), color = theme.accent)
            }
        }
    }
}

@Composable
private fun Swatch(color: Color, seed: Int) {
    Box(Modifier.size(18.dp).sketch(SketchShape.Ellipse, fill = color, stroke = 1.2.dp, seed = seed, inset = 1.dp))
}
