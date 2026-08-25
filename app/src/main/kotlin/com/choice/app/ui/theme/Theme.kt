package com.choice.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * The app's one role-mapping formula, parameterized over a [CatppuccinPalette] (research.md §2) —
 * the mapping the app already used for Mocha, now run once per flavor, with deliberate fixes
 * where real WCAG measurement showed the original formula falls short of FR-009/SC-004's targets
 * in one or more flavors (never actually measured before this feature — research.md §2 addendum):
 *
 * - `onSurfaceVariant` sources from [CatppuccinPalette.text] rather than the pre-feature
 *   `overlay2`, which fails the 4.5:1 target against `surfaceVariant` in every flavor (down to
 *   2.56:1 in Latte). The trade-off: secondary/caption text on a `surfaceVariant` background now
 *   renders at full-text-color weight rather than a subtly muted tone.
 * - `onPrimary`/`onSecondary`/`onTertiary`/`onError` are picked by [readableOnColor] instead of a
 *   fixed `base` literal: `base` already reaches 4.5:1 against every accent color in every flavor
 *   *except* a handful of Latte's mid-toned accents (`flamingo`, `green`, `yellow`, `sky` — see
 *   [buildExtendedColors]), where [readableOnColor] falls back to pure black/white. Every
 *   already-passing pairing (all of Mocha/Frappé/Macchiato, and Latte's `primary`/`error`) is
 *   returned unchanged.
 * - `outline`/`outlineVariant` source from `subtext0`/`overlay2` rather than the pre-feature
 *   `overlay1`/`surface2`, which fail the 3:1 large-graphical target — `outlineVariant` in every
 *   flavor (as low as 1.91:1 in Latte), `outline` in Latte alone (2.83:1). The new pair preserves
 *   the same relative hierarchy (`outline` more prominent than `outlineVariant`) while clearing
 *   3:1 everywhere (see [ThemeContrastTest]).
 *
 * [CatppuccinPalette.isLight] selects [lightColorScheme] vs [darkColorScheme]; both are called
 * with the identical named-argument mapping below.
 *
 * Every Catppuccin flavor's own `base`/`text` pair (and `mauve`, `red`, etc.) is independently
 * designed by upstream Catppuccin for mutual contrast within that flavor, so running this
 * identical formula over any of the four palettes yields a self-consistent, readable
 * [ColorScheme] with no per-flavor special-casing.
 */
fun buildColorScheme(palette: CatppuccinPalette): ColorScheme {
    val onPrimary = readableOnColor(palette.mauve, palette.base, palette.text)
    val onSecondary = readableOnColor(palette.flamingo, palette.base, palette.text)
    val onTertiary = readableOnColor(palette.teal, palette.base, palette.text)
    val onError = readableOnColor(palette.red, palette.base, palette.text)

    return if (palette.isLight) {
        lightColorScheme(
            primary = palette.mauve,
            onPrimary = onPrimary,
            primaryContainer = palette.surface0,
            onPrimaryContainer = palette.text,
            secondary = palette.flamingo,
            onSecondary = onSecondary,
            secondaryContainer = palette.surface0,
            onSecondaryContainer = palette.text,
            tertiary = palette.teal,
            onTertiary = onTertiary,
            tertiaryContainer = palette.surface0,
            onTertiaryContainer = palette.text,
            background = palette.base,
            onBackground = palette.text,
            surface = palette.mantle,
            onSurface = palette.text,
            surfaceVariant = palette.surface0,
            onSurfaceVariant = palette.text,
            error = palette.red,
            onError = onError,
            errorContainer = palette.maroon,
            onErrorContainer = palette.text,
            outline = palette.subtext0,
            outlineVariant = palette.overlay2,
            inverseSurface = palette.text,
            inverseOnSurface = palette.base,
            inversePrimary = palette.blue,
            surfaceTint = palette.mauve,
        )
    } else {
        darkColorScheme(
            primary = palette.mauve,
            onPrimary = onPrimary,
            primaryContainer = palette.surface0,
            onPrimaryContainer = palette.text,
            secondary = palette.flamingo,
            onSecondary = onSecondary,
            secondaryContainer = palette.surface0,
            onSecondaryContainer = palette.text,
            tertiary = palette.teal,
            onTertiary = onTertiary,
            tertiaryContainer = palette.surface0,
            onTertiaryContainer = palette.text,
            background = palette.base,
            onBackground = palette.text,
            surface = palette.mantle,
            onSurface = palette.text,
            surfaceVariant = palette.surface0,
            onSurfaceVariant = palette.text,
            error = palette.red,
            onError = onError,
            errorContainer = palette.maroon,
            onErrorContainer = palette.text,
            outline = palette.subtext0,
            outlineVariant = palette.overlay2,
            inverseSurface = palette.text,
            inverseOnSurface = palette.base,
            inversePrimary = palette.blue,
            surfaceTint = palette.mauve,
        )
    }
}

@Composable
fun ChoiceTheme(flavor: ThemeFlavor, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalChoiceExtendedColors provides flavor.extendedColors) {
        MaterialTheme(
            colorScheme = flavor.colorScheme,
            typography = ChoiceTypography,
            content = content,
        )
    }
}
