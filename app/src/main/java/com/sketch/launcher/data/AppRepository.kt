package com.sketch.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Immutable
data class AppInfo(
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
    val userSerial: Long,
    val icon: ImageBitmap,
    val isWork: Boolean,
) {
    /** Stable id used to remember favorites / hidden apps across restarts. */
    val key: String get() = "${component.flattenToString()}#$userSerial"
    val packageName: String get() = component.packageName
}

class AppRepository(private val context: Context) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private var callback: LauncherApps.Callback? = null

    suspend fun loadApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val iconPx = (48 * context.resources.displayMetrics.density).toInt()
        val me = android.os.Process.myUserHandle()
        userManager.userProfiles.flatMap { user ->
            val serial = userManager.getSerialNumberForUser(user)
            launcherApps.getActivityList(null, user)
                .filter { it.applicationInfo.packageName != context.packageName }
                .map { info ->
                    AppInfo(
                        label = info.label.toString().trim(),
                        component = info.componentName,
                        user = user,
                        userSerial = serial,
                        icon = info.getBadgedIcon(0).toBitmap(iconPx, iconPx).asImageBitmap(),
                        isWork = user != me,
                    )
                }
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    fun launch(app: AppInfo, bounds: Rect? = null) {
        runCatching { launcherApps.startMainActivity(app.component, app.user, bounds, null) }
    }

    fun openAppInfo(app: AppInfo) {
        runCatching { launcherApps.startAppDetailsActivity(app.component, app.user, null, null) }
    }

    fun uninstall(app: AppInfo) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(Intent.EXTRA_USER, app.user)
        runCatching { context.startActivity(intent) }
    }

    /** Calls [onChange] on the main thread whenever apps are installed, removed or updated. */
    fun watch(onChange: () -> Unit) {
        val cb = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) = onChange()
            override fun onPackageAdded(packageName: String?, user: UserHandle?) = onChange()
            override fun onPackageChanged(packageName: String?, user: UserHandle?) = onChange()
            override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = onChange()
            override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = onChange()
        }
        callback = cb
        launcherApps.registerCallback(cb, Handler(Looper.getMainLooper()))
    }

    fun stopWatching() {
        callback?.let { launcherApps.unregisterCallback(it) }
        callback = null
    }
}
