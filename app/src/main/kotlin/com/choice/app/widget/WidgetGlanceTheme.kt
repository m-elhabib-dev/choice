package com.choice.app.widget

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.glance.unit.ColorProvider
import com.choice.app.ui.theme.ThemeFlavor

/**
 * The six widget-role colors for one flavor — data-model.md §3, contracts/widget-theme-contract.md
 * §1. Glance cannot consume a Compose `MaterialTheme` instance directly, so widget code styles
 * itself off these named [ColorProvider]s instead (research.md §9) rather than inlining literal
 * colors or forking a second palette.
 */
data class WidgetThemeColors(
    val background: ColorProvider,
    val surface: ColorProvider,
    val surfaceVariant: ColorProvider,
    val textPrimary: ColorProvider,
    val textSecondary: ColorProvider,
    val accent: ColorProvider,
)

/**
 * Glance-compatible mirror of the app's Catppuccin palettes
 * ([com.choice.app.ui.theme.CatppuccinPalette]), one [WidgetThemeColors] per [ThemeFlavor].
 */
object WidgetGlanceTheme {

    /**
     * Pure function of [flavor] — no I/O, no `Context` (WT-1). For [ThemeFlavor.MOCHA], reproduces
     * this object's pre-feature six [ColorProvider] values exactly (FR-015).
     */
    fun colorsFor(flavor: ThemeFlavor): WidgetThemeColors {
        val palette = flavor.palette
        return WidgetThemeColors(
            background = ColorProvider(palette.base),
            surface = ColorProvider(palette.surface0),
            surfaceVariant = ColorProvider(palette.surface1),
            textPrimary = ColorProvider(palette.text),
            textSecondary = ColorProvider(palette.subtext0),
            accent = ColorProvider(palette.blue),
        )
    }
}

/**
 * Provided once per `provideGlance` invocation (WP-1, WP-2); every widget composable reads colors
 * through this, never a hardcoded literal and never the app-side `MaterialTheme.colorScheme`
 * (Glance cannot consume it). Default falls back to [ThemeFlavor.DEFAULT]'s colors so a preview or
 * test composition without an explicit provider still renders sensibly.
 */
val LocalWidgetColors = staticCompositionLocalOf<WidgetThemeColors> {
    WidgetGlanceTheme.colorsFor(ThemeFlavor.DEFAULT)
}
