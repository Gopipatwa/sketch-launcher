package com.sketch.launcher

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import com.sketch.launcher.sketch.LocalSketch
import com.sketch.launcher.sketch.SketchStyle
import com.sketch.launcher.sketch.SketchThemes
import com.sketch.launcher.sketch.paper
import com.sketch.launcher.ui.DrawerScreen
import com.sketch.launcher.ui.HomeScreen
import com.sketch.launcher.ui.SettingsScreen
import com.sketch.launcher.ui.WidgetPickerScreen
import com.sketch.launcher.widgets.WidgetController
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val vm: LauncherViewModel by viewModels()
    private lateinit var widgets: WidgetController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgets = WidgetController(this, vm, savedInstanceState)
        setContent { LauncherApp(this, vm, widgets) }
    }

    override fun onStart() {
        super.onStart()
        widgets.startListening()
    }

    override fun onStop() {
        super.onStop()
        widgets.stopListening()
    }

    /** Pressing Home while we're already in front returns to the home page. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN) vm.go(Screen.HOME)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        widgets.save(outState)
    }

    @Deprecated("AppWidgetHost's configure flow still reports here")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!widgets.onActivityResult(requestCode, resultCode)) {
            @Suppress("DEPRECATION")
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}

@Composable
private fun LauncherApp(activity: ComponentActivity, vm: LauncherViewModel, widgets: WidgetController) {
    val settings = vm.settings
    val palette = SketchThemes.byId(settings.themeId)
    val frame = if (settings.boilingLines) {
        produceState(0) {
            while (true) {
                delay(260)
                value = (value + 1) % 3
            }
        }
    } else {
        remember { mutableIntStateOf(0) }
    }
    val style = remember(palette, settings.wobble, frame) { SketchStyle(palette, settings.wobble.roughness, frame) }

    LaunchedEffect(palette.isDark) {
        val transparent = AndroidColor.TRANSPARENT
        val bars = if (palette.isDark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
        activity.enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
    }

    val focus = LocalFocusManager.current
    LaunchedEffect(vm.screen) { if (vm.screen == Screen.HOME) focus.clearFocus() }

    // The home screen is the root: back never leaves the launcher.
    BackHandler { vm.back() }

    CompositionLocalProvider(LocalSketch provides style) {
        Box(Modifier.fillMaxSize().paper()) {
            AnimatedContent(
                targetState = vm.screen,
                transitionSpec = {
                    val toDrawer = targetState == Screen.DRAWER
                    val fromDrawer = initialState == Screen.DRAWER
                    when {
                        toDrawer -> (slideInVertically(tween(220)) { it / 6 } + fadeIn(tween(220))) togetherWith fadeOut(tween(120))
                        fromDrawer -> fadeIn(tween(200)) togetherWith (slideOutVertically(tween(200)) { it / 6 } + fadeOut(tween(160)))
                        else -> fadeIn(tween(200)) togetherWith fadeOut(tween(140))
                    }
                },
                label = "screen",
            ) { screen ->
                when (screen) {
                    Screen.HOME -> HomeScreen(vm, widgets)
                    Screen.DRAWER -> DrawerScreen(vm)
                    Screen.SETTINGS -> SettingsScreen(vm, widgets)
                    Screen.WIDGET_PICKER -> WidgetPickerScreen(vm, widgets)
                }
            }
        }
    }
}
