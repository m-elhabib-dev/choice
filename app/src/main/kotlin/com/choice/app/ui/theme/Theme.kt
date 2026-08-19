package com.choice.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CatppuccinMochaDark = darkColorScheme(
    primary = Mauve,
    onPrimary = Base,
    primaryContainer = Surface0,
    onPrimaryContainer = Text,
    secondary = Flamingo,
    onSecondary = Base,
    secondaryContainer = Surface0,
    onSecondaryContainer = Text,
    tertiary = Teal,
    onTertiary = Base,
    tertiaryContainer = Surface0,
    onTertiaryContainer = Text,
    background = Base,
    onBackground = Text,
    surface = Mantle,
    onSurface = Text,
    surfaceVariant = Surface0,
    onSurfaceVariant = Overlay2,
    error = Red,
    onError = Base,
    errorContainer = Maroon,
    onErrorContainer = Text,
    outline = Overlay1,
    outlineVariant = Surface2,
    inverseSurface = Text,
    inverseOnSurface = Base,
    inversePrimary = Blue,
    surfaceTint = Mauve,
)

@Composable
fun ChoiceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CatppuccinMochaDark,
        typography = ChoiceTypography,
        content = content,
    )
}
