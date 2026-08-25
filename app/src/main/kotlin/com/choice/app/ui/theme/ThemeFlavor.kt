package com.choice.app.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.choice.app.R

/**
 * The four selectable Catppuccin flavors — data-model.md §2, contracts/theme-contract.md §1.
 *
 * FR-017 extension point: adding a fifth flavor means appending one entry here (with its own
 * [CatppuccinPalette] instance and two new string resources) — no screen, dialog, widget
 * composable, or navigation code should need to change.
 */
enum class ThemeFlavor(
    // Persisted verbatim in SharedPreferences — stable, never localized, never reused.
    val id: String,
    val isLight: Boolean,
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int,
    internal val palette: CatppuccinPalette,
) {
    LATTE("latte", true, R.string.theme_latte_name, R.string.theme_latte_description, LattePalette),
    FRAPPE("frappe", false, R.string.theme_frappe_name, R.string.theme_frappe_description, FrappePalette),
    MACCHIATO(
        "macchiato",
        false,
        R.string.theme_macchiato_name,
        R.string.theme_macchiato_description,
        MacchiatoPalette,
    ),
    MOCHA("mocha", false, R.string.theme_mocha_name, R.string.theme_mocha_description, MochaPalette),
    ;

    val colorScheme: ColorScheme by lazy { buildColorScheme(palette) }
    val extendedColors: ChoiceExtendedColors by lazy { buildExtendedColors(palette) }

    /** Three representative colors for the FR-002 preview swatch — computed, not stored. */
    val swatchColors: List<Color> get() = listOf(palette.base, palette.mauve, palette.blue)

    companion object {
        val DEFAULT: ThemeFlavor = MOCHA

        /** Returns `null` for anything not matching one of the four `id`s (FR-006). */
        fun fromId(id: String?): ThemeFlavor? = entries.find { it.id == id }
    }
}
