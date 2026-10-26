package com.sketch.launcher.data

import android.content.Context
import androidx.compose.runtime.Immutable

enum class ClockStyle(val title: String) { DIGITAL("Digital"), ANALOG("Analog"), WORDS("Words") }
enum class IconStyle(val title: String) { NONE("None"), ORIGINAL("Colour"), INK("Ink"), LETTER("Letter") }
enum class Wobble(val title: String, val roughness: Float) { CALM("Calm", 0.45f), NORMAL("Normal", 1f), WILD("Wild", 1.8f) }

@Immutable
data class Settings(
    val themeId: String = "notebook",
    val clockStyle: ClockStyle = ClockStyle.DIGITAL,
    val use24h: Boolean = false,
    val showDate: Boolean = true,
    val showBattery: Boolean = true,
    val showCalendar: Boolean = false,
    val showNote: Boolean = true,
    val homeIcons: IconStyle = IconStyle.NONE,
    val drawerIcons: IconStyle = IconStyle.ORIGINAL,
    val wobble: Wobble = Wobble.NORMAL,
    val boilingLines: Boolean = false,
    val autoKeyboard: Boolean = true,
    val favorites: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val widgetIds: List<Int> = emptyList(),
    val widgetHeights: Map<Int, Int> = emptyMap(),
    val note: String = "",
    val favoritesSeeded: Boolean = false,
)

/** Plain SharedPreferences: the data is tiny and this keeps the launcher dependency-free. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("sketch_launcher", Context.MODE_PRIVATE)

    fun load(): Settings {
        val d = Settings()
        return Settings(
            themeId = sp.getString("theme", d.themeId)!!,
            clockStyle = enumOr(sp.getString("clock", null), d.clockStyle),
            use24h = sp.getBoolean("use24h", d.use24h),
            showDate = sp.getBoolean("showDate", d.showDate),
            showBattery = sp.getBoolean("showBattery", d.showBattery),
            showCalendar = sp.getBoolean("showCalendar", d.showCalendar),
            showNote = sp.getBoolean("showNote", d.showNote),
            homeIcons = enumOr(sp.getString("homeIcons", null), d.homeIcons),
            drawerIcons = enumOr(sp.getString("drawerIcons", null), d.drawerIcons),
            wobble = enumOr(sp.getString("wobble", null), d.wobble),
            boilingLines = sp.getBoolean("boiling", d.boilingLines),
            autoKeyboard = sp.getBoolean("autoKeyboard", d.autoKeyboard),
            favorites = sp.getString("favorites", "")!!.split('|').filter { it.isNotBlank() },
            hidden = sp.getString("hidden", "")!!.split('|').filter { it.isNotBlank() }.toSet(),
            widgetIds = sp.getString("widgets", "")!!.split(',').mapNotNull { it.toIntOrNull() },
            widgetHeights = sp.getString("widgetHeights", "")!!.split(',').mapNotNull { entry ->
                val (id, h) = entry.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
                (id.toIntOrNull() ?: return@mapNotNull null) to (h.toIntOrNull() ?: return@mapNotNull null)
            }.toMap(),
            note = sp.getString("note", d.note)!!,
            favoritesSeeded = sp.getBoolean("seeded", false),
        )
    }

    fun save(s: Settings) {
        sp.edit()
            .putString("theme", s.themeId)
            .putString("clock", s.clockStyle.name)
            .putBoolean("use24h", s.use24h)
            .putBoolean("showDate", s.showDate)
            .putBoolean("showBattery", s.showBattery)
            .putBoolean("showCalendar", s.showCalendar)
            .putBoolean("showNote", s.showNote)
            .putString("homeIcons", s.homeIcons.name)
            .putString("drawerIcons", s.drawerIcons.name)
            .putString("wobble", s.wobble.name)
            .putBoolean("boiling", s.boilingLines)
            .putBoolean("autoKeyboard", s.autoKeyboard)
            .putString("favorites", s.favorites.joinToString("|"))
            .putString("hidden", s.hidden.joinToString("|"))
            .putString("widgets", s.widgetIds.joinToString(","))
            .putString("widgetHeights", s.widgetHeights.entries.joinToString(",") { "${it.key}:${it.value}" })
            .putString("note", s.note)
            .putBoolean("seeded", s.favoritesSeeded)
            .apply()
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, fallback: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: fallback
}
