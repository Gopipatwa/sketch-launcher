package com.sketch.launcher.ui

import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sketch.launcher.LauncherViewModel
import com.sketch.launcher.data.AppInfo
import com.sketch.launcher.data.IconStyle
import com.sketch.launcher.sketch.LocalSketch
import com.sketch.launcher.sketch.MenuItem
import com.sketch.launcher.sketch.SketchDialog
import com.sketch.launcher.sketch.SketchFonts
import com.sketch.launcher.sketch.SketchShape
import com.sketch.launcher.sketch.SketchText
import com.sketch.launcher.sketch.sketch

@Composable
fun AppIcon(app: AppInfo, style: IconStyle, size: Dp) {
    val p = LocalSketch.current.palette
    when (style) {
        IconStyle.NONE -> Unit
        IconStyle.ORIGINAL -> Image(app.icon, contentDescription = null, modifier = Modifier.size(size))
        IconStyle.INK -> {
            // Desaturate, then let the paper show through a little: reads like a pencil sketch.
            val filter = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
            Image(app.icon, contentDescription = null, modifier = Modifier.size(size), colorFilter = filter, alpha = 0.85f)
        }
        IconStyle.LETTER -> Box(
            Modifier
                .size(size)
                .sketch(SketchShape.Ellipse, seed = app.label.hashCode(), stroke = 1.6.dp),
            contentAlignment = Alignment.Center,
        ) {
            SketchText(
                app.label.firstOrNull()?.uppercase() ?: "?",
                size = (size.value * 0.5f).sp,
                font = SketchFonts.display,
                color = if (app.isWork) p.accent else p.ink,
            )
        }
    }
}

@Composable
fun AppMenuDialog(app: AppInfo, vm: LauncherViewModel, onDismiss: () -> Unit) {
    val p = LocalSketch.current.palette
    val favorite = vm.isFavorite(app)
    SketchDialog(onDismiss = onDismiss, seed = app.label.hashCode()) {
        SketchText(app.label + if (app.isWork) " (work)" else "", size = 32.sp, font = SketchFonts.display, maxLines = 2)
        if (favorite) {
            MenuItem("Remove from home") { vm.toggleFavorite(app); onDismiss() }
            MenuItem("Move up") { vm.moveFavorite(app, -1); onDismiss() }
            MenuItem("Move down") { vm.moveFavorite(app, 1); onDismiss() }
        } else {
            MenuItem("Pin to home") { vm.toggleFavorite(app); onDismiss() }
        }
        MenuItem("App info") { vm.repo.openAppInfo(app); onDismiss() }
        MenuItem("Hide from drawer") { vm.hide(app); onDismiss() }
        MenuItem("Uninstall", color = p.accent) { vm.repo.uninstall(app); onDismiss() }
    }
}

object SystemActions {
    /** Pull down the notification shade. Uses a hidden (but long-stable) API, so failures are ignored. */
    @SuppressLint("WrongConstant")
    fun expandNotifications(context: Context) {
        runCatching {
            val service = context.getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(service)
        }
    }

    fun openAlarms(context: Context) = start(context, Intent(AlarmClock.ACTION_SHOW_ALARMS))

    fun openCalendar(context: Context) =
        start(context, Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR))

    fun isDefaultLauncher(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val info = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return info?.activityInfo?.packageName == context.packageName
    }

    /** Intent that asks the user to make us the home app. */
    fun defaultLauncherIntent(context: Context): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roles = context.getSystemService(RoleManager::class.java)
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME) && !roles.isRoleHeld(RoleManager.ROLE_HOME)) {
                return roles.createRequestRoleIntent(RoleManager.ROLE_HOME)
            }
        }
        return Intent(Settings.ACTION_HOME_SETTINGS)
    }

    fun start(context: Context, intent: Intent) {
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
