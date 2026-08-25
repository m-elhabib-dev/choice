package com.choice.app.widget

import androidx.glance.unit.ColorProvider
import com.choice.app.ui.theme.CatppuccinPalette
import com.choice.app.ui.theme.FrappePalette
import com.choice.app.ui.theme.LattePalette
import com.choice.app.ui.theme.MacchiatoPalette
import com.choice.app.ui.theme.MochaPalette
import com.choice.app.ui.theme.ThemeFlavor
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * `colorsFor`'s six-role mapping table for all four flavors, plus the FR-015 guarantee that
 * `colorsFor(MOCHA)` reproduces the pre-feature [WidgetGlanceTheme] constants exactly —
 * data-model.md §3, contracts/widget-theme-contract.md §1.
 */
class WidgetGlanceThemeTest {

    private fun expected(palette: CatppuccinPalette) = WidgetThemeColors(
        background = ColorProvider(palette.base),
        surface = ColorProvider(palette.surface0),
        surfaceVariant = ColorProvider(palette.surface1),
        textPrimary = ColorProvider(palette.text),
        textSecondary = ColorProvider(palette.subtext0),
        accent = ColorProvider(palette.blue),
    )

    @Test
    fun `colorsFor LATTE maps the six roles from the Latte palette`() {
        assertEquals(expected(LattePalette), WidgetGlanceTheme.colorsFor(ThemeFlavor.LATTE))
    }

    @Test
    fun `colorsFor FRAPPE maps the six roles from the Frappe palette`() {
        assertEquals(expected(FrappePalette), WidgetGlanceTheme.colorsFor(ThemeFlavor.FRAPPE))
    }

    @Test
    fun `colorsFor MACCHIATO maps the six roles from the Macchiato palette`() {
        assertEquals(expected(MacchiatoPalette), WidgetGlanceTheme.colorsFor(ThemeFlavor.MACCHIATO))
    }

    @Test
    fun `colorsFor MOCHA maps the six roles from the Mocha palette`() {
        assertEquals(expected(MochaPalette), WidgetGlanceTheme.colorsFor(ThemeFlavor.MOCHA))
    }

    /** FR-015: byte-for-byte the pre-feature WidgetGlanceTheme constants. */
    @Test
    fun `colorsFor MOCHA reproduces the pre-feature WidgetGlanceTheme constants exactly`() {
        val colors = WidgetGlanceTheme.colorsFor(ThemeFlavor.MOCHA)
        assertEquals(ColorProvider(androidx.compose.ui.graphics.Color(0xFF1E1E2E)), colors.background)
        assertEquals(ColorProvider(androidx.compose.ui.graphics.Color(0xFF313244)), colors.surface)
        assertEquals(ColorProvider(androidx.compose.ui.graphics.Color(0xFF45475A)), colors.surfaceVariant)
        assertEquals(ColorProvider(androidx.compose.ui.graphics.Color(0xFFCDD6F4)), colors.textPrimary)
        assertEquals(ColorProvider(androidx.compose.ui.graphics.Color(0xFFA6ADC8)), colors.textSecondary)
        assertEquals(ColorProvider(androidx.compose.ui.graphics.Color(0xFF89B4FA)), colors.accent)
    }
}
