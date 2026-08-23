package com.choice.app.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider

/**
 * Glance-compatible mirror of the app's Catppuccin Mocha palette
 * ([com.choice.app.ui.theme.Color]). Glance cannot consume a Compose `MaterialTheme` instance
 * directly, so widget code styles itself off these named [ColorProvider]s instead (research.md
 * §9) rather than inlining literal colors or forking a second palette.
 */
object WidgetGlanceTheme {
    val background = ColorProvider(Color(0xFF1E1E2E)) // Base
    val surface = ColorProvider(Color(0xFF313244)) // Surface0
    val surfaceVariant = ColorProvider(Color(0xFF45475A)) // Surface1
    val textPrimary = ColorProvider(Color(0xFFCDD6F4)) // Text
    val textSecondary = ColorProvider(Color(0xFFA6ADC8)) // Subtext0
    val accent = ColorProvider(Color(0xFF89B4FA)) // Blue
}
