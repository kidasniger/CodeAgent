package com.zagent.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Accès court aux tokens étendus : `Zcolors.text2`. */
val Zcolors: ZColors
    @Composable get() = ZAccentOverride.value?.let { LocalZColors.current.withAccent(it) } ?: LocalZColors.current

/**
 * Paramètres d'un style visuel complet.
 * palette : identifiant de palette ; glassMaterial : matériau de surface ;
 * colour : couleur d'accent hexadécimale ; frost/film : intensités normalisées ;
 * activeTab : onglet mis en évidence ; radius : rayon des surfaces en dp.
 */
data class StylePreset(
    val palette: String,
    val glassMaterial: String,
    val colour: String,
    val frost: Float,
    val film: Float,
    val activeTab: String,
    val radius: Float
)

object StylePresetIds {
    const val MINIMAL = "MINIMAL"
    const val GLASS = "GLASS"
    const val AURORA = "AURORA"
    const val CONTRAST = "CONTRAST"
    const val CUSTOM = "CUSTOM"
}

object StylePresets {
    val minimal = StylePreset("CLAUDE", "NONE", "#C96442", 0f, 0f, "Chat", 18f)
    val aurora = StylePreset("AURORA", "LIQUID_QUALITY", "#22D3EE", 0.82f, 0.32f, "Chat", 24f)
    val glass = StylePreset("ZAGENT", "LIQUID_QUALITY", "#22D3EE", 0.82f, 0.32f, "Chat", 24f)
    val contrast = StylePreset("HIGH_CONTRAST", "NONE", "#FFFFFF", 0f, 0f, "Chat", 12f)

    fun fromId(id: String): StylePreset = when (id) {
        StylePresetIds.MINIMAL -> minimal
        StylePresetIds.CONTRAST -> contrast
        StylePresetIds.AURORA -> aurora
        else -> glass
    }
}

/** Mode de thème choisi par l'utilisateur : suit le système, ou force clair/sombre. */
enum class ThemeMode { SYSTEM, LIGHT, DARK, SCHEDULED }

/**
 * Convertit le mode choisi en thème effectif.
 * SYSTEM suit le réglage sombre d'Android ; LIGHT et DARK sont forcés.
 */
fun ThemeMode.resolveDarkTheme(systemDark: Boolean): Boolean = when (this) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SCHEDULED -> false
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

private val MinimalDarkScheme = darkColorScheme(
    primary = Color(0xFFE08A68),
    onPrimary = Color(0xFF262624),
    primaryContainer = Color(0xFF49352D),
    onPrimaryContainer = Color(0xFFF0EEE6),
    secondary = Color(0xFFE08A68),
    onSecondary = Color(0xFF262624),
    tertiary = Color(0xFFE08A68),
    background = Color(0xFF262624),
    onBackground = Color(0xFFF0EEE6),
    surface = Color(0xFF30302E),
    onSurface = Color(0xFFF0EEE6),
    surfaceVariant = Color(0xFF383835),
    onSurfaceVariant = Color(0xFFA8A69E),
    surfaceContainer = Color(0xFF30302E),
    surfaceContainerHigh = Color(0xFF383835),
    surfaceContainerHighest = Color(0xFF41413D),
    outline = Color(0x3DFFFFFF),
    outlineVariant = Color(0x24141413),
    error = ZRedD,
    onError = Color(0xFF262624),
    errorContainer = Color(0x21F87171),
    onErrorContainer = Color(0xFFF0EEE6),
    scrim = Color(0xB3262624)
)

private val MinimalLightScheme = lightColorScheme(
    primary = Color(0xFFC96442),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF4E4DC),
    onPrimaryContainer = Color(0xFF141413),
    secondary = Color(0xFFC96442),
    onSecondary = Color.White,
    tertiary = Color(0xFFC96442),
    background = Color(0xFFFAF9F5),
    onBackground = Color(0xFF141413),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF141413),
    surfaceVariant = Color(0xFFF4F3EF),
    onSurfaceVariant = Color(0xFF5E5D59),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF4F3EF),
    surfaceContainerHighest = Color(0xFFECEBE6),
    outline = Color(0x24141413),
    outlineVariant = Color(0x14141413),
    error = ZRedL,
    onError = Color.White,
    errorContainer = Color(0x17DC2626),
    onErrorContainer = Color(0xFF141413),
    scrim = Color(0x730D0D0C)
)

@Composable
fun ZAgentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    stylePreset: String = StylePresetIds.AURORA,
    oledBlack: Boolean = false,
    dynamicColors: Boolean = false,
    cornerRadius: Int = 16,
    content: @Composable () -> Unit
) {
    val minimal = stylePreset == StylePresetIds.MINIMAL
    val context = LocalContext.current
    val useDynamic = dynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val baseScheme = when {
        useDynamic && darkTheme -> dynamicDarkColorScheme(context)
        useDynamic -> dynamicLightColorScheme(context)
        minimal && darkTheme -> MinimalDarkScheme
        minimal -> MinimalLightScheme
        darkTheme -> DarkScheme
        else -> LightScheme
    }
    val baseColors = when {
        minimal && darkTheme -> MinimalDarkZColors
        minimal -> MinimalLightZColors
        darkTheme -> AuroraDarkZColors
        else -> AuroraLightZColors
    }
    val oledColors = if (oledBlack && darkTheme) baseColors.copy(
        bg = Color.Black, bg2 = Color(0xFF080808),
        surface = Color(0xFF0A0A0A), surface2 = Color(0xFF111111),
        surface3 = Color(0xFF181818), border = Color(0xFF292929),
        border2 = Color(0xFF3A3A3A)
    ) else baseColors
    val zcolors = oledColors.copy(
        bg = animateColorAsState(oledColors.bg, tween(300), label = "theme-bg").value,
        bg2 = animateColorAsState(oledColors.bg2, tween(300), label = "theme-bg2").value,
        surface = animateColorAsState(oledColors.surface, tween(300), label = "theme-surface").value,
        surface2 = animateColorAsState(oledColors.surface2, tween(300), label = "theme-surface2").value,
        surface3 = animateColorAsState(oledColors.surface3, tween(300), label = "theme-surface3").value,
        border = animateColorAsState(oledColors.border, tween(300), label = "theme-border").value,
        border2 = animateColorAsState(oledColors.border2, tween(300), label = "theme-border2").value,
        text = animateColorAsState(oledColors.text, tween(300), label = "theme-text").value,
        text2 = animateColorAsState(oledColors.text2, tween(300), label = "theme-text2").value,
        text3 = animateColorAsState(oledColors.text3, tween(300), label = "theme-text3").value,
        cyan = animateColorAsState(oledColors.cyan, tween(300), label = "theme-accent").value,
        violet = animateColorAsState(oledColors.violet, tween(300), label = "theme-accent2").value
    )
    val scheme = if (useDynamic) baseScheme.copy(
        primary = baseScheme.primary,
        secondary = baseScheme.secondary,
        tertiary = baseScheme.tertiary
    ) else baseScheme
    val radius = cornerRadius.coerceIn(8, 28).dp
    val shapes = Shapes(
        extraSmall = RoundedCornerShape((radius.value * 0.5f).dp),
        small = RoundedCornerShape((radius.value * 0.65f).dp),
        medium = RoundedCornerShape(radius),
        large = RoundedCornerShape((radius.value * 1.15f).dp),
        extraLarge = RoundedCornerShape((radius.value * 1.5f).dp)
    )
    CompositionLocalProvider(LocalZColors provides zcolors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = ZTypography,
            shapes = shapes,
            content = content
        )
    }
}
