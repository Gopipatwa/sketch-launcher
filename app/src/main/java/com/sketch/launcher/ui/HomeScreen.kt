package com.sketch.launcher.ui

import android.appwidget.AppWidgetHostView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.sketch.launcher.LauncherViewModel
import com.sketch.launcher.Screen
import com.sketch.launcher.data.AppInfo
import com.sketch.launcher.data.IconStyle
import com.sketch.launcher.sketch.Glyph
import com.sketch.launcher.sketch.LocalSketch
import com.sketch.launcher.sketch.MenuItem
import com.sketch.launcher.sketch.SketchDialog
import com.sketch.launcher.sketch.SketchFonts
import com.sketch.launcher.sketch.SketchGlyph
import com.sketch.launcher.sketch.SketchText
import com.sketch.launcher.sketch.sketch
import com.sketch.launcher.sketch.tap
import com.sketch.launcher.widgets.WidgetController

@Composable
fun HomeScreen(vm: LauncherViewModel, widgets: WidgetController) {
    val s = vm.settings
    val p = LocalSketch.current.palette
    val context = LocalContext.current
    val swipe = with(LocalDensity.current) { 80.dp.toPx() }

    var homeMenu by remember { mutableStateOf(false) }
    var appMenu by remember { mutableStateOf<AppInfo?>(null) }
    var widgetMenu by remember { mutableStateOf<Int?>(null) }

    val openDrawer = { vm.go(Screen.DRAWER) }
    val openShade = { SystemActions.expandNotifications(context) }

    // Leftover scroll from the widget list (already at its end) still counts as a swipe.
    val overscroll = remember {
        object : NestedScrollConnection {
            var total = 0f
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                @Suppress("DEPRECATION")
                if (source == NestedScrollSource.Drag) {
                    total += available.y
                    if (total < -swipe) { total = 0f; openDrawer() }
                    if (total > swipe) { total = 0f; openShade() }
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                total = 0f
                return Velocity.Zero
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .nestedScroll(overscroll)
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        if (total < -swipe) openDrawer() else if (total > swipe) openShade()
                    },
                ) { _, amount -> total += amount }
            }
            .pointerInput(Unit) { detectTapGestures(onLongPress = { homeMenu = true }) }
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding()
            .padding(horizontal = 28.dp),
    ) {
        Spacer(Modifier.height(36.dp))
        ClockWidget(s.clockStyle, s.use24h, Modifier.tap { SystemActions.openAlarms(context) })
        if (s.showDate) DateLine(Modifier.tap { SystemActions.openCalendar(context) })
        if (s.showBattery) BatteryWidget(Modifier.padding(top = 6.dp))

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (s.showCalendar) CalendarWidget()
            if (s.showNote) NoteWidget(s.note, onChange = { text -> vm.update { it.copy(note = text) } })
            s.widgetIds.forEach { id ->
                HostedWidget(
                    id = id,
                    heightDp = s.widgetHeights[id] ?: widgets.defaultHeightDp(id),
                    widgets = widgets,
                    onMenu = { widgetMenu = id },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            s.favorites.mapNotNull { vm.appsByKey[it] }.forEach { app ->
                FavoriteRow(app, s.homeIcons, onClick = { vm.launch(app) }, onLongClick = { appMenu = app })
            }
            if (s.favorites.isEmpty() && vm.apps.isNotEmpty()) {
                SketchText("long-press an app in the drawer to pin it here", size = 18.sp, color = p.muted)
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp)
                .tap(onClick = openDrawer),
            contentAlignment = Alignment.Center,
        ) {
            SketchGlyph(Glyph.ArrowUp, Modifier.size(width = 36.dp, height = 22.dp), color = p.muted)
        }
    }

    if (homeMenu) {
        SketchDialog(onDismiss = { homeMenu = false }) {
            SketchText("Home", size = 34.sp, font = SketchFonts.display)
            MenuItem("Add a widget") { homeMenu = false; vm.go(Screen.WIDGET_PICKER) }
            MenuItem("Themes & settings") { homeMenu = false; vm.go(Screen.SETTINGS) }
            MenuItem("All apps") { homeMenu = false; openDrawer() }
        }
    }
    appMenu?.let { app -> AppMenuDialog(app, vm) { appMenu = null } }
    widgetMenu?.let { id ->
        val current = s.widgetHeights[id] ?: widgets.defaultHeightDp(id)
        SketchDialog(onDismiss = { widgetMenu = null }) {
            SketchText(widgets.label(id), size = 30.sp, font = SketchFonts.display, maxLines = 2)
            MenuItem("Taller") { vm.resizeWidget(id, current + 40) }
            MenuItem("Shorter") { vm.resizeWidget(id, current - 40) }
            MenuItem("Remove widget", color = p.accent) { widgets.remove(id); widgetMenu = null }
        }
    }
}

@Composable
private fun FavoriteRow(app: AppInfo, icons: IconStyle, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .tap(onLongClick = onLongClick, onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icons != IconStyle.NONE) {
            AppIcon(app, icons, 30.dp)
            Spacer(Modifier.width(14.dp))
        }
        SketchText(app.label, size = 28.sp, maxLines = 1)
    }
}

@Composable
private fun HostedWidget(id: Int, heightDp: Int, widgets: WidgetController, onMenu: () -> Unit) {
    val p = LocalSketch.current.palette
    val label = remember(id) { widgets.label(id) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SketchText(label, Modifier.weight(1f), size = 16.sp, color = p.muted, maxLines = 1)
            Box(Modifier.size(36.dp).tap(onClick = onMenu), contentAlignment = Alignment.Center) {
                SketchGlyph(Glyph.Dots, Modifier.size(22.dp), color = p.muted, seed = id)
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(heightDp.dp)
                .sketch(seed = id)
                .padding(6.dp),
        ) {
            val view = remember(id) { widgets.createView(id) }
            if (view == null) {
                SketchText("This widget's app is gone. Use the dots above to remove it.", Modifier.padding(8.dp), size = 18.sp, color = p.muted)
            } else {
                AndroidView(
                    factory = {
                        (view.parent as? android.view.ViewGroup)?.removeView(view)
                        view.apply { addOnLayoutChangeListener(SizeReporter) }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/** Reports the hosted widget's real size in dp whenever its layout changes. */
private object SizeReporter : android.view.View.OnLayoutChangeListener {
    override fun onLayoutChange(
        v: android.view.View, left: Int, top: Int, right: Int, bottom: Int,
        oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int,
    ) {
        val w = right - left
        val h = bottom - top
        if (w == oldRight - oldLeft && h == oldBottom - oldTop) return
        val density = v.resources.displayMetrics.density
        WidgetController.reportSize(v as AppWidgetHostView, (w / density).toInt(), (h / density).toInt())
    }
}
