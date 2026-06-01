package com.sonari.speak2easy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private fun darkScheme(c: SonariColors) = darkColorScheme(
    primary = c.accent,
    onPrimary = c.buttonText,
    secondary = c.accentSecondary,
    background = c.background,
    onBackground = c.textPrimary,
    surface = c.surfacePrimary,
    onSurface = c.textPrimary,
    surfaceVariant = c.surfaceSecondary,
    onSurfaceVariant = c.textSecondary,
    error = c.error,
    outline = c.border,
)

private fun lightScheme(c: SonariColors) = lightColorScheme(
    primary = c.accent,
    onPrimary = c.buttonText,
    secondary = c.accentSecondary,
    background = c.background,
    onBackground = c.textPrimary,
    surface = c.surfacePrimary,
    onSurface = c.textPrimary,
    surfaceVariant = c.surfaceSecondary,
    onSurfaceVariant = c.textSecondary,
    error = c.error,
    outline = c.border,
)

/** Convenience accessor for the active palette: `SonariTheme.colors.accent`, etc. */
object SonariTheme {
    val colors: SonariColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSonariColors.current
}

@Composable
fun Speak2EasyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val sonari = if (darkTheme) SonariDarkColors else SonariLightColors
    val colorScheme = if (darkTheme) darkScheme(sonari) else lightScheme(sonari)

    CompositionLocalProvider(LocalSonariColors provides sonari) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SonariTypography,
            content = content,
        )
    }
}
