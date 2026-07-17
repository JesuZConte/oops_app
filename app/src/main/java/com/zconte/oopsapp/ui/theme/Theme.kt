package com.zconte.oopsapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonPrimary,
    onPrimary = Color.White,
    secondary = NeonSecondary,
    onSecondary = Color.White,
    tertiary = NeonTertiary,
    onTertiary = Color.White,
    background = NeonBackground,
    onBackground = NeonOnSurface,
    surface = NeonSurface,
    onSurface = NeonOnSurface,
    surfaceVariant = NeonSurfaceVariant,
    outline = NeonOutline,
    error = NeonError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PaperPrimary,
    onPrimary = Color.White,
    secondary = PaperSecondary,
    onSecondary = Color.White,
    tertiary = PaperTertiary,
    onTertiary = Color.White,
    background = PaperBackground,
    onBackground = PaperInk,
    surface = PaperSurface,
    onSurface = PaperInk,
    outline = PaperInk,
    error = PaperError,
    onError = Color.White
)

@Composable
fun OopsappTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalOopsExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = OopsTypography,
            shapes = OopsShapes,
            content = content
        )
    }
}

object OopsTheme {
    val extendedColors: OopsExtendedColors
        @Composable
        get() = LocalOopsExtendedColors.current
}