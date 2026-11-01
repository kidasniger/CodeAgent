package com.zagent.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Accès court aux tokens étendus : `Zcolors.text2`. */
val Zcolors: ZColors
    @Composable get() = LocalZColors.current

/** Mode de thème choisi par l'utilisateur : suit le système, ou force clair/sombre. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Convertit le mode choisi en thème effectif.
 * SYSTEM suit le réglage sombre d'Android ; LIGHT et DARK sont forcés.
 */
fun ThemeMode.resolveDarkTheme(systemDark: Boolean): Boolean = when (this) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

// Rayons du design system : 8 / 16 / 24 / full
val ZShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

private val DarkScheme = darkColorScheme(
    primary = ZCyan,
    onPrimary = ZOnGradDark,
    primaryContainer = Color(0x298B5CF6),
    onPrimaryContainer = ZTextDark,
    secondary = ZViolet,
    onSecondary = Color.White,
    tertiary = ZCyan,
    background = ZBgDark,
    onBackground = ZTextDark,
    surface = ZBg2Dark,
    onSurface = ZTextDark,
    surfaceVariant = Color(0x1A94A3BA),
    onSurfaceVariant = ZText2Dark,
    surfaceContainer = Color(0x14101826),
    surfaceContainerHigh = Color(0x1A101826),
    surfaceContainerHighest = Color(0x22101826),
    outline = Color(0x4294A3BA),
    outlineVariant = Color(0x2494A3BA),
    error = ZRedD,
    onError = Color(0xFF2B0A0A),
    errorContainer = Color(0x21F87171),
    onErrorContainer = ZTextDark,
    scrim = Color(0x9E04070D)
)

private val LightScheme = lightColorScheme(
    primary = ZCyanL,
    onPrimary = Color.White,
    primaryContainer = Color(0x1A7C3AED),
    onPrimaryContainer = ZTextLight,
    secondary = ZVioletL,
    onSecondary = Color.White,
    tertiary = ZCyanL,
    background = ZBgLight,
    onBackground = ZTextLight,
    surface = ZBg2Light,
    onSurface = ZTextLight,
    surfaceVariant = Color(0x0F0D1930),
    onSurfaceVariant = ZText2Light,
    surfaceContainer = Color(0xFFF6F8FD),
    surfaceContainerHigh = Color(0xFFF0F3FA),
    surfaceContainerHighest = Color(0xFFE9EEF8),
    outline = Color(0x2E0D1930),
    outlineVariant = Color(0x1A0D1930),
    error = ZRedL,
    onError = Color.White,
    errorContainer = Color(0x17DC2626),
    onErrorContainer = ZTextLight,
    scrim = Color(0x730D1930)
)

@Composable
fun ZAgentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val zcolors = if (darkTheme) DarkZColors else LightZColors
    CompositionLocalProvider(LocalZColors provides zcolors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = ZTypography,
            shapes = ZShapes,
            content = content
        )
    }
}
