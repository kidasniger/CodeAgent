package com.zagent.app.ui.theme

import android.content.Context
import android.provider.Settings
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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import com.zagent.app.data.HapticLevel

/**
 * Accès court aux tokens étendus : `Zcolors.text2`.
 * L'accent personnalisé est mémorisé séparément pour le clair et le sombre (mise à jour 7) ;
 * le mode actif est déduit de [ZColors.isDark] de la palette en cours.
 */
val Zcolors: ZColors
    @Composable get() {
        val base = LocalZColors.current
        val override = if (base.isDark) ZAccentOverrideDark.value else ZAccentOverrideLight.value
        return override?.let { base.withAccent(it) } ?: base
    }

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
    val contrast = StylePreset("HIGH_CONTRAST", "NONE", "#005FCC", 0f, 0f, "Chat", 12f)

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

/** Rayon courant des surfaces, fourni par [ZAgentTheme] selon le réglage « Arrondi » (8 à 28 dp). */
val LocalZRadius = compositionLocalOf { 16.dp }

/**
 * MàJ 95 — vrai quand les animations doivent être réduites : réglage interne
 * « Réduire les animations » OU échelle d'animation du système à 0
 * (Settings.Global.ANIMATOR_DURATION_SCALE). Les glissements, le shimmer et
 * les blobs se remplacent alors par des fondus courts ou des états statiques.
 */
val LocalReduceMotion = compositionLocalOf { false }

/** MàJ 91 — niveau de retour haptique global, fourni par [ZAgentTheme] depuis MainActivity. */
val LocalHapticNiveau = compositionLocalOf { HapticLevel.NORMAL }

/** Constantes d'animation partagées du Lot 8. */
object ZMouvement {
    const val ENTREE_MESSAGE_MS = 200      // MàJ 89
    const val GLISSEMENT_MESSAGE_DP = 8f   // MàJ 89
    const val FONDU_APP_MS = 400           // MàJ 93
    const val FONDU_REDUIT_MS = 90         // MàJ 95 — fondu court de repli
    const val SHIMMER_MS = 1250            // MàJ 90
}

/** MàJ 95 — échelle d'animation du système ; 0 signifie « animations désactivées ». */
fun echelleAnimationsSysteme(context: Context): Float =
    Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
    )
    reglageInterne || echelleAnimationsSysteme(context) == 0f

/**
 * Formes du design system pilotées par le réglage « Arrondi » (mise à jour 13) :
 * - `Zshapes.card` : cartes, bulles, champs et boutons ;
 * - `Zshapes.pill` : chips, pastilles et barre d'onglets (arrondi maximal constant) ;
 * - `Zshapes.sheet` : grandes surfaces (aperçus, feuilles, dialogues), rayon amplifié.
 */
object Zshapes {
    val card: Shape
        @Composable get() = RoundedCornerShape(LocalZRadius.current)

    val pill: Shape
        get() = RoundedCornerShape(50)

    val sheet: Shape
        @Composable get() {
            val r = LocalZRadius.current
            return RoundedCornerShape((r.value * 1.5f).coerceAtMost(32f).dp)
        }
}

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
    fontFamilyId: String = "INTER",
    fontScale: Float = 1f,
    densityScale: Float = 1f,
    reduceMotion: Boolean = false,
    hapticLevel: HapticLevel = HapticLevel.NORMAL,
    content: @Composable () -> Unit
) {
    val minimal = stylePreset == StylePresetIds.MINIMAL
    val contrast = stylePreset == StylePresetIds.CONTRAST
    val context = LocalContext.current
    val useDynamic = dynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val baseScheme = when {
        useDynamic && darkTheme -> dynamicDarkColorScheme(context)
        useDynamic -> dynamicLightColorScheme(context)
        minimal && darkTheme -> MinimalDarkScheme
        minimal -> MinimalLightScheme
        contrast && darkTheme -> DarkScheme.copy(
            background = Color.Black, surface = Color(0xFF080808),
            outline = Color.White, outlineVariant = Color(0xFFBDBDBD)
        )
        contrast -> LightScheme.copy(
            background = Color.White, surface = Color.White,
            outline = Color.Black, outlineVariant = Color(0xFF424242)
        )
        darkTheme -> DarkScheme
        else -> LightScheme
    }
    val baseColors = when {
        minimal && darkTheme -> MinimalDarkZColors
        minimal -> MinimalLightZColors
        contrast && darkTheme -> AuroraDarkZColors.copy(
            bg = Color.Black, bg2 = Color(0xFF080808),
            surface = Color(0xFF101010), surface2 = Color(0xFF181818),
            surface3 = Color(0xFF242424), border = Color.White,
            border2 = Color(0xFFBDBDBD), text = Color.White,
            text2 = Color(0xFFE0E0E0), text3 = Color(0xFFBDBDBD)
        )
        contrast -> AuroraLightZColors.copy(
            bg = Color.White, bg2 = Color.White,
            surface = Color.White, surface2 = Color(0xFFF5F5F5),
            surface3 = Color(0xFFEEEEEE), border = Color.Black,
            border2 = Color(0xFF424242), text = Color.Black,
            text2 = Color(0xFF212121), text3 = Color(0xFF424242)
        )
        darkTheme -> AuroraDarkZColors
        else -> AuroraLightZColors
    }
    val oledColors = if (oledBlack && darkTheme) baseColors.copy(
        bg = Color.Black, bg2 = Color(0xFF080808),
        surface = Color(0xFF0A0A0A), surface2 = Color(0xFF111111),
        surface3 = Color(0xFF181818), border = Color(0xFF292929),
        border2 = Color(0xFF3A3A3A)
    ) else baseColors
    val activeAccentOverride = if (darkTheme) ZAccentOverrideDark.value else ZAccentOverrideLight.value
    val dynamicPalette = if (useDynamic && activeAccentOverride == null) oledColors.withAccent(baseScheme.primary) else oledColors
    val zcolors = dynamicPalette.copy(
        bg = animateColorAsState(dynamicPalette.bg, tween(300), label = "theme-bg").value,
        bg2 = animateColorAsState(dynamicPalette.bg2, tween(300), label = "theme-bg2").value,
        surface = animateColorAsState(dynamicPalette.surface, tween(300), label = "theme-surface").value,
        surface2 = animateColorAsState(dynamicPalette.surface2, tween(300), label = "theme-surface2").value,
        surface3 = animateColorAsState(dynamicPalette.surface3, tween(300), label = "theme-surface3").value,
        border = animateColorAsState(dynamicPalette.border, tween(300), label = "theme-border").value,
        border2 = animateColorAsState(dynamicPalette.border2, tween(300), label = "theme-border2").value,
        text = animateColorAsState(dynamicPalette.text, tween(300), label = "theme-text").value,
        text2 = animateColorAsState(dynamicPalette.text2, tween(300), label = "theme-text2").value,
        text3 = animateColorAsState(dynamicPalette.text3, tween(300), label = "theme-text3").value,
        cyan = animateColorAsState(dynamicPalette.cyan, tween(300), label = "theme-accent").value,
        violet = animateColorAsState(dynamicPalette.violet, tween(300), label = "theme-accent2").value,
        green = animateColorAsState(dynamicPalette.green, tween(300), label = "theme-green").value,
        amber = animateColorAsState(dynamicPalette.amber, tween(300), label = "theme-amber").value,
        red = animateColorAsState(dynamicPalette.red, tween(300), label = "theme-red").value,
        onGrad = animateColorAsState(dynamicPalette.onGrad, tween(300), label = "theme-on-gradient").value,
        cyanBg = animateColorAsState(dynamicPalette.cyanBg, tween(300), label = "theme-cyan-bg").value,
        violetBg = animateColorAsState(dynamicPalette.violetBg, tween(300), label = "theme-violet-bg").value,
        greenBg = animateColorAsState(dynamicPalette.greenBg, tween(300), label = "theme-green-bg").value,
        amberBg = animateColorAsState(dynamicPalette.amberBg, tween(300), label = "theme-amber-bg").value,
        redBg = animateColorAsState(dynamicPalette.redBg, tween(300), label = "theme-red-bg").value,
        codeBg = animateColorAsState(dynamicPalette.codeBg, tween(300), label = "theme-code-bg").value,
        codeBorder = animateColorAsState(dynamicPalette.codeBorder, tween(300), label = "theme-code-border").value,
        codePlain = animateColorAsState(dynamicPalette.codePlain, tween(300), label = "theme-code-plain").value,
        scrim = animateColorAsState(dynamicPalette.scrim, tween(300), label = "theme-scrim").value,
        gradGlow = animateColorAsState(dynamicPalette.gradGlow, tween(300), label = "theme-gradient-glow").value,
        gridline = animateColorAsState(dynamicPalette.gridline, tween(300), label = "theme-gridline").value
    )
    val scheme = if (oledBlack && darkTheme) {
        baseScheme.copy(
            background = Color.Black,
            surface = Color(0xFF0A0A0A),
            surfaceVariant = Color(0xFF111111),
            surfaceContainer = Color(0xFF0A0A0A),
            surfaceContainerHigh = Color(0xFF111111),
            surfaceContainerHighest = Color(0xFF181818),
            outline = Color(0xFF292929),
            outlineVariant = Color(0xFF3A3A3A)
        )
    } else baseScheme
    val radius = cornerRadius.coerceIn(8, 28).dp
    val shapes = Shapes(
        extraSmall = RoundedCornerShape((radius.value * 0.5f).dp),
        small = RoundedCornerShape((radius.value * 0.65f).dp),
        medium = RoundedCornerShape(radius),
        large = RoundedCornerShape(radius),
        extraLarge = RoundedCornerShape(radius)
    )
    val selectedFont = when (fontFamilyId) {
        "SYSTEM" -> FontFamily.Default
        "SPACE_GROTESK" -> SpaceGrotesk
        else -> Inter
    }
    // MàJ 98 — preset Minimal : titres en serif (FontFamily.Serif, voir Type.kt),
    // corps inchangé. Si l'utilisateur a choisi la police « Système », on la
    // respecte pour l'ensemble (titres compris).
    val fontTitres = if (minimal && fontFamilyId != "SYSTEM") SerifTitres else selectedFont
    // MàJ 97 — l'échelle va désormais de 85 % à 130 %.
    val typography = scaledTypography(selectedFont, fontTitres, fontScale.coerceIn(0.85f, 1.30f))
    val currentDensity = LocalDensity.current
    val scaledDensity = Density(
        density = currentDensity.density * densityScale.coerceIn(0.88f, 1.12f),
        fontScale = currentDensity.fontScale
    )
    CompositionLocalProvider(
        LocalZColors provides zcolors,
        LocalZRadius provides radius,
        LocalDensity provides scaledDensity,
        LocalReduceMotion provides reduceMotion,
        LocalHapticNiveau provides hapticLevel
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}


private fun TextStyle.withInterfaceFont(font: FontFamily, scale: Float): TextStyle = copy(
    fontFamily = font,
    fontSize = fontSize * scale,
    lineHeight = lineHeight * scale
)

/**
 * MàJ 97/98 — construit la typographie finale :
 * - [policeCorps] remplace la famille de tous les styles (choix « Police ») ;
 * - [policeTitres] ne remplace la famille que des styles de titres
 *   (display, headline, title) — c'est là que le preset Minimal applique le serif ;
 * - [scale] multiplie tailles et interlignes (85-130 %). Les valeurs en sp
 *     restent ensuite multipliées par le fontScale du système via [Density].
 */
private fun scaledTypography(policeCorps: FontFamily, policeTitres: FontFamily, scale: Float) = ZTypography.copy(
    displayLarge = ZTypography.displayLarge.withInterfaceFont(policeTitres, scale),
    displayMedium = ZTypography.displayMedium.withInterfaceFont(policeTitres, scale),
    displaySmall = ZTypography.displaySmall.withInterfaceFont(policeTitres, scale),
    headlineLarge = ZTypography.headlineLarge.withInterfaceFont(policeTitres, scale),
    headlineMedium = ZTypography.headlineMedium.withInterfaceFont(policeTitres, scale),
    headlineSmall = ZTypography.headlineSmall.withInterfaceFont(policeTitres, scale),
    titleLarge = ZTypography.titleLarge.withInterfaceFont(policeTitres, scale),
    titleMedium = ZTypography.titleMedium.withInterfaceFont(policeTitres, scale),
    titleSmall = ZTypography.titleSmall.withInterfaceFont(policeCorps, scale),
    bodyLarge = ZTypography.bodyLarge.withInterfaceFont(policeCorps, scale),
    bodyMedium = ZTypography.bodyMedium.withInterfaceFont(policeCorps, scale),
    bodySmall = ZTypography.bodySmall.withInterfaceFont(policeCorps, scale),
    labelLarge = ZTypography.labelLarge.withInterfaceFont(policeCorps, scale),
    labelMedium = ZTypography.labelMedium.withInterfaceFont(policeCorps, scale),
    labelSmall = ZTypography.labelSmall.withInterfaceFont(policeCorps, scale)
)
