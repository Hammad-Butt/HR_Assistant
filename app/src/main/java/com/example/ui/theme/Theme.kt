package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

data class CustomAppColors(
    val isDark: Boolean,
    val canvasBg: Color,
    val surface1: Color,
    val surface2: Color,
    val surface3: Color,
    val separator: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textPlaceholder: Color,
    val green: Color,
    val greenDark: Color,
    val greenSoft: Color,
    val red: Color,
    val redSoft: Color
)

private val DarkAppColors = CustomAppColors(
    isDark = true,
    canvasBg = CanvasBlack,
    surface1 = SurfaceDark1,
    surface2 = SurfaceDark2,
    surface3 = SurfaceDark3,
    separator = Separator,
    textPrimary = TextWhite,
    textSecondary = TextMuted,
    textPlaceholder = TextPlaceholder,
    green = SystemGreen,
    greenDark = SystemGreenDark,
    greenSoft = GreenSoft,
    red = SystemRed,
    redSoft = RedSoft
)

private val LightAppColors = CustomAppColors(
    isDark = false,
    canvasBg = CanvasLight,
    surface1 = SurfaceLight1,
    surface2 = SurfaceLight2,
    surface3 = SurfaceLight3,
    separator = SeparatorLight,
    textPrimary = TextDark,
    textSecondary = TextMutedLight,
    textPlaceholder = TextPlaceholderLight,
    green = SystemGreenLight,
    greenDark = SystemGreenLightDark,
    greenSoft = GreenSoftLight,
    red = SystemRedLight,
    redSoft = RedSoftLight
)

val LocalCustomAppColors = staticCompositionLocalOf { DarkAppColors }

object AppTheme {
    val colors: CustomAppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCustomAppColors.current
}

private val DarkColorScheme = darkColorScheme(
    primary = SystemGreen,
    onPrimary = CanvasBlack,
    primaryContainer = GreenSoft,
    onPrimaryContainer = SystemGreen,
    secondary = SurfaceDark2,
    onSecondary = TextWhite,
    background = CanvasBlack,
    onBackground = TextWhite,
    surface = SurfaceDark1,
    onSurface = TextWhite,
    surfaceVariant = SurfaceDark2,
    onSurfaceVariant = TextMuted,
    outline = Separator,
    error = SystemRed,
    onError = CanvasBlack,
    errorContainer = RedSoft,
    onErrorContainer = SystemRed
)

private val LightColorScheme = lightColorScheme(
    primary = SystemGreenLight,
    onPrimary = Color.White,
    primaryContainer = GreenSoftLight,
    onPrimaryContainer = SystemGreenLight,
    secondary = SurfaceLight2,
    onSecondary = TextDark,
    background = CanvasLight,
    onBackground = TextDark,
    surface = SurfaceLight1,
    onSurface = TextDark,
    surfaceVariant = SurfaceLight2,
    onSurfaceVariant = TextMutedLight,
    outline = SeparatorLight,
    error = SystemRedLight,
    onError = Color.White,
    errorContainer = RedSoftLight,
    onErrorContainer = SystemRedLight
)

@Composable
fun HRAssistantTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val appColors = if (darkTheme) DarkAppColors else LightAppColors
    val bgArgb = if (darkTheme) CanvasBlack.toArgb() else CanvasLight.toArgb()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = bgArgb
                window.navigationBarColor = bgArgb
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalCustomAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
