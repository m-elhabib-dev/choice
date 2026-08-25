package com.choice.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * WCAG 2.1 relative-luminance contrast ratio (https://www.w3.org/TR/WCAG21/#dfn-relative-luminance),
 * used by [buildColorScheme] and [buildExtendedColors] to pick a readable "on-color" for accent
 * surfaces — FR-009/SC-004.
 */
internal fun wcagContrastRatio(a: Color, b: Color): Double {
    fun relativeLuminance(color: Color): Double {
        fun linearize(component: Float): Double {
            val c = component.toDouble()
            return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * linearize(color.red) +
            0.7152 * linearize(color.green) +
            0.0722 * linearize(color.blue)
    }
    val lighter = maxOf(relativeLuminance(a), relativeLuminance(b))
    val darker = minOf(relativeLuminance(a), relativeLuminance(b))
    return (lighter + 0.05) / (darker + 0.05)
}

private const val NORMAL_TEXT_CONTRAST_MIN = 4.5

/**
 * Picks a readable foreground for [background]: [preferred] (the flavor's own `base`, keeping
 * today's appearance wherever it already passes), then [fallback] (`text`, the palette's other
 * contrast extreme), and only pure black/white — outside the Catppuccin palette — when neither
 * named extreme reaches 4.5:1 against this specific [background].
 *
 * This is needed because a handful of Latte's mid-toned accent colors (e.g. `flamingo`, `green`,
 * `yellow`, `sky`) sit roughly midway in luminance between Latte's own `base` and `text`, so
 * neither reaches 4.5:1 against them — a property of those specific hex values, not fixable by
 * choosing a different *named* palette role. Every other accent, in every flavor, already passes
 * via [preferred] and is returned unchanged (research.md §2 addendum).
 */
internal fun readableOnColor(background: Color, preferred: Color, fallback: Color): Color {
    if (wcagContrastRatio(preferred, background) >= NORMAL_TEXT_CONTRAST_MIN) return preferred
    if (wcagContrastRatio(fallback, background) >= NORMAL_TEXT_CONTRAST_MIN) return fallback
    return if (wcagContrastRatio(Color.Black, background) >= wcagContrastRatio(Color.White, background)) {
        Color.Black
    } else {
        Color.White
    }
}
