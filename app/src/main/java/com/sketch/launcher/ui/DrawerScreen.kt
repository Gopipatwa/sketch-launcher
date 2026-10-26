package com.sketch.launcher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sketch.launcher.LauncherViewModel
import com.sketch.launcher.Screen
import com.sketch.launcher.data.AppInfo
import com.sketch.launcher.data.IconStyle
import com.sketch.launcher.sketch.Glyph
import com.sketch.launcher.sketch.LocalSketch
import com.sketch.launcher.sketch.SketchFonts
import com.sketch.launcher.sketch.SketchGlyph
import com.sketch.launcher.sketch.SketchShape
import com.sketch.launcher.sketch.SketchText
import com.sketch.launcher.sketch.sketch
import com.sketch.launcher.sketch.tap
import kotlinx.coroutines.delay

private sealed interface DrawerRow {
    data class Header(val letter: String) : DrawerRow
    data class App(val app: AppInfo) : DrawerRow
}

/** Best matches first: name starts with the query, then a word starts with it, then contains it. */
private fun search(apps: List<AppInfo>, query: String): List<AppInfo> {
    val q = query.trim()
    if (q.isEmpty()) return apps
    fun rank(app: AppInfo): Int {
        val label = app.label
        return when {
            label.startsWith(q, ignoreCase = true) -> 0
            label.split(' ', '-', '.').any { it.startsWith(q, ignoreCase = true) } -> 1
            label.contains(q, ignoreCase = true) -> 2
            app.packageName.contains(q, ignoreCase = true) -> 3
            else -> -1
        }
    }
    return apps.map { it to rank(it) }.filter { it.second >= 0 }.sortedBy { it.second }.map { it.first }
}

@Composable
fun DrawerScreen(vm: LauncherViewModel) {
    val s = vm.settings
    val p = LocalSketch.current.palette
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    var appMenu by remember { mutableStateOf<AppInfo?>(null) }
    val pullToClose = with(LocalDensity.current) { 90.dp.toPx() }

    val results = remember(vm.visibleApps, vm.query) { search(vm.visibleApps, vm.query) }
    val rows = remember(results, vm.query) {
        if (vm.query.isNotBlank()) results.map<AppInfo, DrawerRow> { DrawerRow.App(it) }
        else buildList<DrawerRow> {
            var last = ""
            results.forEach { app ->
                val first = app.label.firstOrNull()?.uppercaseChar()
                val letter = if (first != null && first.isLetter()) first.toString() else "#"
                if (letter != last) {
                    add(DrawerRow.Header(letter))
                    last = letter
                }
                add(DrawerRow.App(app))
            }
        }
    }

    LaunchedEffect(Unit) {
        if (s.autoKeyboard) {
            delay(180)
            runCatching { focus.requestFocus() }
        }
    }

    // Pull down at the top of the list to close; any scroll hides the keyboard.
    val connection = remember {
        object : NestedScrollConnection {
            var pulled = 0f
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                keyboard?.hide()
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                @Suppress("DEPRECATION")
                if (source == NestedScrollSource.Drag && available.y > 0f) {
                    pulled += available.y
                    if (pulled > pullToClose) {
                        pulled = 0f
                        vm.go(Screen.HOME)
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                pulled = 0f
                return Velocity.Zero
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding()
            .padding(horizontal = 24.dp),
    ) {
        Row(Modifier.padding(top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier
                    .weight(1f)
                    .sketch(seed = 5)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SketchGlyph(Glyph.Search, Modifier.size(22.dp), color = p.muted)
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = vm.query,
                    onValueChange = { vm.query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = p.ink, fontSize = 22.sp, fontFamily = SketchFonts.hand),
                    cursorBrush = SolidColor(p.accent),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(onGo = { results.firstOrNull()?.let { vm.launch(it) } }),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focus),
                    decorationBox = { inner ->
                        Box {
                            if (vm.query.isEmpty()) SketchText("search apps", size = 22.sp, color = p.muted)
                            inner()
                        }
                    },
                )
                if (vm.query.isNotEmpty()) {
                    SketchGlyph(Glyph.Close, Modifier.size(20.dp).tap { vm.query = "" }, color = p.muted)
                }
            }
            Box(
                Modifier
                    .padding(start = 10.dp)
                    .size(46.dp)
                    .tap { vm.go(Screen.SETTINGS) },
                contentAlignment = Alignment.Center,
            ) {
                SketchGlyph(Glyph.Gear, Modifier.size(30.dp))
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .nestedScroll(connection),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(rows, key = { row ->
                when (row) {
                    is DrawerRow.Header -> "h:${row.letter}"
                    is DrawerRow.App -> row.app.key
                }
            }) { row ->
                when (row) {
                    is DrawerRow.Header -> SketchText(
                        row.letter,
                        Modifier.padding(top = 14.dp, bottom = 2.dp),
                        size = 26.sp,
                        font = SketchFonts.display,
                        color = p.accent,
                    )
                    is DrawerRow.App -> DrawerApp(
                        app = row.app,
                        icons = s.drawerIcons,
                        onClick = { vm.launch(row.app) },
                        onLongClick = { appMenu = row.app },
                    )
                }
            }
            if (rows.isEmpty() && vm.query.isNotBlank()) {
                item {
                    SketchText("nothing called \"${vm.query}\"…", Modifier.padding(top = 24.dp), size = 22.sp, color = p.muted)
                }
            }
        }
    }

    appMenu?.let { app -> AppMenuDialog(app, vm) { appMenu = null } }
}

@Composable
private fun DrawerApp(app: AppInfo, icons: IconStyle, onClick: () -> Unit, onLongClick: () -> Unit) {
    val p = LocalSketch.current.palette
    Row(
        Modifier
            .fillMaxWidth()
            .tap(onLongClick = onLongClick, onClick = onClick)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icons != IconStyle.NONE) {
            AppIcon(app, icons, 36.dp)
            Spacer(Modifier.width(16.dp))
        }
        SketchText(app.label, Modifier.weight(1f, fill = false), size = 23.sp, maxLines = 1)
        if (app.isWork) {
            SketchText(
                "work",
                Modifier
                    .padding(start = 10.dp)
                    .sketch(SketchShape.Underline, color = p.accent, seed = 3),
                size = 15.sp,
                color = p.accent,
            )
        }
    }
}
