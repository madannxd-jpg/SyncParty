package com.syncparty.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    secondary = AccentCyan,
    tertiary = AccentPink,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceLightDark,
    onPrimary = TextPrimaryDark,
    onSecondary = BackgroundDark,
    onTertiary = TextPrimaryDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = CardBorderDark,
    error = DangerRed
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryPurple,
    secondary = AccentCyan,
    tertiary = AccentPink,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onPrimary = TextPrimaryDark,
    onSecondary = TextPrimaryDark,
    onTertiary = TextPrimaryDark,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = CardBorderLight,
    error = DangerRed
)

enum class ThemeMode {
    DARK, LIGHT, SYSTEM
}

val LocalThemeMode = compositionLocalOf { mutableStateOf(ThemeMode.DARK) }

@Composable
fun SyncPartyTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
