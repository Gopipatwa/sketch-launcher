package com.sketch.launcher.sketch

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.sketch.launcher.R

enum class PaperPattern { NONE, RULED, GRID, DOTS }

@Immutable
data class SketchPalette(
    val id: String,
    val name: String,
    val paper: Color,
    val ink: Color,
    val muted: Color,
    val accent: Color,
    val highlight: Color,
    val pattern: PaperPattern,
    val patternColor: Color,
    val isDark: Boolean,
)

object SketchThemes {
    val Notebook = SketchPalette(
        id = "notebook", name = "Notebook",
        paper = Color(0xFFF7F3E8), ink = Color(0xFF2B2B2B), muted = Color(0xFF857E70),
        accent = Color(0xFFD9534F), highlight = Color(0xFFFFE27A),
        pattern = PaperPattern.RULED, patternColor = Color(0x5584A9D1), isDark = false,
    )
    val Blueprint = SketchPalette(
        id = "blueprint", name = "Blueprint",
        paper = Color(0xFF1D4A86), ink = Color(0xFFF2F6FF), muted = Color(0xFFA9C1E8),
        accent = Color(0xFFFFD166), highlight = Color(0xFF2E62A8),
        pattern = PaperPattern.GRID, patternColor = Color(0x22FFFFFF), isDark = true,
    )
    val Chalkboard = SketchPalette(
        id = "chalkboard", name = "Chalkboard",
        paper = Color(0xFF2E3B33), ink = Color(0xFFEDEDE4), muted = Color(0xFFA4B0A7),
        accent = Color(0xFFF4A6A6), highlight = Color(0xFF475A4E),
        pattern = PaperPattern.NONE, patternColor = Color.Transparent, isDark = true,
    )
    val Midnight = SketchPalette(
        id = "midnight", name = "Midnight Ink",
        paper = Color(0xFF0E0E0E), ink = Color(0xFFEAEAEA), muted = Color(0xFF707070),
        accent = Color(0xFFFF6B6B), highlight = Color(0xFF262626),
        pattern = PaperPattern.NONE, patternColor = Color.Transparent, isDark = true,
    )
    val Kraft = SketchPalette(
        id = "kraft", name = "Kraft Paper",
        paper = Color(0xFFD8C29D), ink = Color(0xFF3B2A1A), muted = Color(0xFF7A6248),
        accent = Color(0xFF9C3418), highlight = Color(0xFFEBDDBF),
        pattern = PaperPattern.DOTS, patternColor = Color(0x553B2A1A), isDark = false,
    )
    val Pastel = SketchPalette(
        id = "pastel", name = "Pastel Doodle",
        paper = Color(0xFFFFF1F4), ink = Color(0xFF3D3550), muted = Color(0xFF8E86A3),
        accent = Color(0xFF7C6CF0), highlight = Color(0xFFC9F2E1),
        pattern = PaperPattern.DOTS, patternColor = Color(0x553D3550), isDark = false,
    )
    val Graph = SketchPalette(
        id = "graph", name = "Graph Paper",
        paper = Color(0xFFFDFDFB), ink = Color(0xFF1A1A1A), muted = Color(0xFF7B7B7B),
        accent = Color(0xFF2E86DE), highlight = Color(0xFFB8F0C2),
        pattern = PaperPattern.GRID, patternColor = Color(0x4466B08A), isDark = false,
    )

    val all = listOf(Notebook, Blueprint, Chalkboard, Midnight, Kraft, Pastel, Graph)

    fun byId(id: String) = all.firstOrNull { it.id == id } ?: Notebook
}

object SketchFonts {
    val hand = FontFamily(Font(R.font.patrick_hand))
    val display = FontFamily(Font(R.font.caveat_bold, FontWeight.Bold))
}

/** Everything a sketch component needs to draw itself. */
@Immutable
class SketchStyle(
    val palette: SketchPalette,
    val roughness: Float,
    /** Bumps periodically when "boiling lines" animation is on; folded into every seed. */
    val frame: State<Int>,
)

val LocalSketch = staticCompositionLocalOf<SketchStyle> { error("SketchStyle not provided") }
