package com.choice.app.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The three status-color roles Material3's [androidx.compose.material3.ColorScheme] has no slot
 * for, needed to satisfy FR-009's full role list — data-model.md §4. "Disabled text" is
 * deliberately not part of this set; use `MaterialTheme.colorScheme.onSurface.copy(alpha =
 * 0.38f)`, the standard Material3 convention, instead.
 */
data class ChoiceExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
)

/**
 * Mirrors [buildColorScheme]'s formula for `error`/`onError`/`errorContainer`/`onErrorContainer`,
 * applied to `green`/`yellow`/`sky` (research.md §2). `onSuccess`/`onWarning`/`onInfo` use
 * [readableOnColor] rather than a fixed `base` literal — see [buildColorScheme]'s doc comment;
 * these three are exactly the accents where Latte's `base` (and `text`) both fail 4.5:1.
 */
fun buildExtendedColors(palette: CatppuccinPalette): ChoiceExtendedColors = ChoiceExtendedColors(
    success = palette.green,
    onSuccess = readableOnColor(palette.green, palette.base, palette.text),
    successContainer = palette.surface0,
    onSuccessContainer = palette.text,
    warning = palette.yellow,
    onWarning = readableOnColor(palette.yellow, palette.base, palette.text),
    warningContainer = palette.surface0,
    onWarningContainer = palette.text,
    info = palette.sky,
    onInfo = readableOnColor(palette.sky, palette.base, palette.text),
    infoContainer = palette.surface0,
    onInfoContainer = palette.text,
)

/** Provided by [ChoiceTheme] alongside `MaterialTheme` — no default; always set within the theme. */
val LocalChoiceExtendedColors = compositionLocalOf<ChoiceExtendedColors> {
    error("LocalChoiceExtendedColors not provided — read it from within ChoiceTheme { }")
}
