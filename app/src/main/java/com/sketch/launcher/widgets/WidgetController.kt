package com.sketch.launcher.widgets

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.sketch.launcher.LauncherViewModel
import com.sketch.launcher.Screen

/**
 * Hosts real Android app widgets: allocation, the bind-permission prompt,
 * the provider's configuration screen, and view creation.
 */
class WidgetController(
    private val activity: ComponentActivity,
    private val vm: LauncherViewModel,
    savedState: Bundle?,
) {
    val manager: AppWidgetManager = AppWidgetManager.getInstance(activity)
    val host = AppWidgetHost(activity.applicationContext, HOST_ID)

    private var pendingId: Int = savedState?.getInt(KEY_PENDING, -1) ?: -1

    private val bindLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val id = pendingId
        if (id == -1) return@registerForActivityResult
        val info = manager.getAppWidgetInfo(id)
        if (result.resultCode == Activity.RESULT_OK && info != null) configureOrAdd(id, info) else discard(id)
    }

    fun providers(): List<AppWidgetProviderInfo> = manager.installedProviders

    fun startListening() = runCatching { host.startListening() }
    fun stopListening() = runCatching { host.stopListening() }

    fun pick(info: AppWidgetProviderInfo) {
        val id = host.allocateAppWidgetId()
        pendingId = id
        val bound = runCatching {
            manager.bindAppWidgetIdIfAllowed(id, info.profile, info.provider, null)
        }.getOrDefault(false)
        if (bound) {
            configureOrAdd(id, info)
        } else {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile)
            runCatching { bindLauncher.launch(intent) }.onFailure { discard(id) }
        }
    }

    private fun configureOrAdd(id: Int, info: AppWidgetProviderInfo) {
        val optional = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            info.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL != 0
        if (info.configure != null && !optional) {
            val started = runCatching {
                host.startAppWidgetConfigureActivityForResult(activity, id, 0, REQUEST_CONFIGURE, null)
            }.isSuccess
            if (started) return
        }
        finish(id)
    }

    /** Called from the activity's onActivityResult (the configure API predates result contracts). */
    fun onActivityResult(requestCode: Int, resultCode: Int): Boolean {
        if (requestCode != REQUEST_CONFIGURE) return false
        val id = pendingId
        if (id != -1) {
            if (resultCode == Activity.RESULT_OK) finish(id) else discard(id)
        }
        return true
    }

    private fun finish(id: Int) {
        pendingId = -1
        vm.addWidget(id)
        vm.go(Screen.HOME)
    }

    private fun discard(id: Int) {
        pendingId = -1
        runCatching { host.deleteAppWidgetId(id) }
    }

    fun remove(id: Int) {
        runCatching { host.deleteAppWidgetId(id) }
        vm.removeWidget(id)
    }

    fun createView(id: Int): AppWidgetHostView? {
        val info = manager.getAppWidgetInfo(id) ?: return null
        return host.createView(activity, id, info)
    }

    fun defaultHeightDp(id: Int): Int {
        val info = manager.getAppWidgetInfo(id) ?: return 120
        val density = activity.resources.displayMetrics.density
        return (info.minHeight / density).toInt().coerceIn(80, 400)
    }

    fun label(id: Int): String {
        val info = manager.getAppWidgetInfo(id) ?: return "missing widget"
        return info.loadLabel(activity.packageManager)
    }

    fun save(out: Bundle) = out.putInt(KEY_PENDING, pendingId)

    companion object {
        private const val HOST_ID = 0x51E7
        private const val REQUEST_CONFIGURE = 0x51E8
        private const val KEY_PENDING = "pending_widget_id"

        /** Tell the widget how much room it actually has, so it can pick its layout. */
        @Suppress("DEPRECATION")
        fun reportSize(view: AppWidgetHostView, widthDp: Int, heightDp: Int) {
            if (widthDp <= 0 || heightDp <= 0) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                view.updateAppWidgetSize(Bundle(), listOf(SizeF(widthDp.toFloat(), heightDp.toFloat())))
            } else {
                view.updateAppWidgetSize(null, widthDp, heightDp, widthDp, heightDp)
            }
        }
    }
}
