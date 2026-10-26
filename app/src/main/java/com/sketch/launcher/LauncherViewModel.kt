package com.sketch.launcher

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sketch.launcher.data.AppInfo
import com.sketch.launcher.data.AppRepository
import com.sketch.launcher.data.Prefs
import com.sketch.launcher.data.Settings
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class Screen { HOME, DRAWER, SETTINGS, WIDGET_PICKER }

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    val repo = AppRepository(app)

    var settings by mutableStateOf(prefs.load())
        private set
    var apps by mutableStateOf<List<AppInfo>>(emptyList())
        private set
    var screen by mutableStateOf(Screen.HOME)
        private set
    var query by mutableStateOf("")

    val visibleApps by derivedStateOf { apps.filter { it.key !in settings.hidden } }
    val appsByKey by derivedStateOf { apps.associateBy { it.key } }

    private var loadJob: Job? = null

    init {
        repo.watch { reload() }
        reload()
    }

    fun reload() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            apps = repo.loadApps()
            seedFavorites()
        }
    }

    fun update(change: (Settings) -> Settings) {
        settings = change(settings)
        prefs.save(settings)
    }

    fun go(target: Screen) {
        if (target == Screen.DRAWER) query = ""
        screen = target
    }

    /** Back press: everything except home returns home; home swallows it. */
    fun back(): Boolean {
        if (screen == Screen.HOME) return false
        screen = if (screen == Screen.WIDGET_PICKER) Screen.SETTINGS else Screen.HOME
        return true
    }

    fun launch(app: AppInfo) {
        repo.launch(app)
        // Come back to a clean home screen next time.
        viewModelScope.launch {
            kotlinx.coroutines.delay(400)
            screen = Screen.HOME
        }
    }

    fun isFavorite(app: AppInfo) = app.key in settings.favorites

    fun toggleFavorite(app: AppInfo) = update {
        if (app.key in it.favorites) it.copy(favorites = it.favorites - app.key)
        else it.copy(favorites = it.favorites + app.key)
    }

    fun moveFavorite(app: AppInfo, delta: Int) = update {
        val list = it.favorites.toMutableList()
        val i = list.indexOf(app.key)
        val j = i + delta
        if (i < 0 || j !in list.indices) return@update it
        list[i] = list[j].also { list[j] = list[i] }
        it.copy(favorites = list)
    }

    fun hide(app: AppInfo) = update { it.copy(hidden = it.hidden + app.key, favorites = it.favorites - app.key) }
    fun unhide(key: String) = update { it.copy(hidden = it.hidden - key) }

    fun addWidget(id: Int) = update { it.copy(widgetIds = it.widgetIds + id) }

    fun removeWidget(id: Int) = update { it.copy(widgetIds = it.widgetIds - id, widgetHeights = it.widgetHeights - id) }

    fun resizeWidget(id: Int, heightDp: Int) = update {
        it.copy(widgetHeights = it.widgetHeights + (id to heightDp.coerceIn(60, 600)))
    }

    /**
     * First run: put the phone, messages, camera and browser on the home screen,
     * so it isn't empty before the user has pinned anything.
     */
    private fun seedFavorites() {
        if (settings.favoritesSeeded || apps.isEmpty()) return
        val pm = getApplication<Application>().packageManager
        val candidates = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING),
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER),
        )
        val packages = candidates.mapNotNull {
            runCatching { pm.resolveActivity(it, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName }.getOrNull()
        }.filter { it != "android" }.distinct()
        val keys = packages.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg && !it.isWork }?.key }
        update { it.copy(favorites = (it.favorites + keys).distinct(), favoritesSeeded = true) }
    }

    override fun onCleared() {
        repo.stopWatching()
    }
}
