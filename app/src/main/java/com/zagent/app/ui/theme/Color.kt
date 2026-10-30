package com.zagent.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Tokens de marque (issus du design system ZAgent)
// Dégradé d'accent #22D3EE → #6366F1 → #8B5CF6, réservé aux moments clés.
// ─────────────────────────────────────────────────────────────────────────────

// Accents de marque
val ZCyan = Color(0xFF22D3EE)
val ZViolet = Color(0xFF8B5CF6)
val ZIndigo = Color(0xFF6366F1)
val ZGradMid = Color(0xFF7C6CF8)

// Sémantiques (mode sombre)
val ZGreenD = Color(0xFF34D399)
val ZAmberD = Color(0xFFFBBF24)
val ZRedD = Color(0xFFF87171)

// Sémantiques (mode clair, plus profondes)
val ZGreenL = Color(0xFF0E9F6E)
val ZAmberL = Color(0xFFD97706)
val ZRedL = Color(0xFFDC2626)
val ZCyanL = Color(0xFF0891B2)
val ZVioletL = Color(0xFF7C3AED)

// Fond sombre
val ZBgDark = Color(0xFF0B0F17)
val ZBg2Dark = Color(0xFF0E1420)
val ZCodeBgDark = Color(0xFF0D1424)

// Fond clair
val ZBgLight = Color(0xFFF3F5FB)
val ZBg2Light = Color(0xFFFFFFFF)
val ZCodeBg = Color(0xFF0F1526) // les blocs de code restent sombres dans les deux thèmes

// Texte
val ZTextDark = Color(0xFFE9EEF8)
val ZText2Dark = Color(0xFF97A3BA)
val ZText3Dark = Color(0xFF5C6A85)
val ZTextLight = Color(0xFF0D1A30)
val ZText2Light = Color(0xFF51607A)
val ZText3Light = Color(0xFF8B98AD)

// Sur dégradé
val ZOnGradDark = Color(0xFF06121D)
val ZOnGradLight = Color(0xFFFFFFFF)

/**
 * Palette étendue ZAgent, fournie via [LocalZColors].
 * Reprend les variables CSS du design system (surface vitrées, teintes sémantiques…).
 */
@Immutable
data class ZColors(
    val bg: Color,
    val bg2: Color,
    /** Carte glass (alpha faible). */
    val surface: Color,
    /** Éléments surélevés : barre de saisie, chips. */
    val surface2: Color,
    /** Surfaces plus marquées (hover, enfoncé). */
    val surface3: Color,
    val border: Color,
    val border2: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val cyan: Color,
    val violet: Color,
    val green: Color,
    val amber: Color,
    val red: Color,
    /** Texte/icônes posés sur le dégradé. */
    val onGrad: Color,
    /** Teintes translucides pour pastilles et fonds d'icônes. */
    val cyanBg: Color,
    val violetBg: Color,
    val greenBg: Color,
    val amberBg: Color,
    val redBg: Color,
    /** Blocs de code : toujours sombres. */
    val codeBg: Color,
    val codeBorder: Color,
    val codePlain: Color,
    val scrim: Color,
    /** Dégradés de marque. */
    val grad: Brush,
    val gradBtn: Brush,
    /** Couleur d'ombre du dégradé (bouton principal). */
    val gradGlow: Color,
    /** Ligne d'accent dégradée (fichier modifié par l'agent). */
    val gradLine: Brush,
    val gridline: Color,
    val isDark: Boolean = true
)

val DarkZColors = ZColors(
    bg = ZBgDark,
    bg2 = ZBg2Dark,
    surface = Color(0x0E94A3BA),        // rgba(148,163,184,.055)
    surface2 = Color(0x1A94A3BA),       // .10
    surface3 = Color(0x2994A3BA),       // .16
    border = Color(0x2494A3BA),         // .14
    border2 = Color(0x4294A3BA),        // .26
    text = ZTextDark,
    text2 = ZText2Dark,
    text3 = ZText3Dark,
    cyan = ZCyan,
    violet = ZViolet,
    green = ZGreenD,
    amber = ZAmberD,
    red = ZRedD,
    onGrad = ZOnGradDark,
    cyanBg = Color(0x2138E1F8),         // rgba(56,225,248,.13) ~ cyan .13
    violetBg = Color(0x298B5CF6),       // .16
    greenBg = Color(0x2434D399),        // .14
    amberBg = Color(0x21FBBF24),        // .13
    redBg = Color(0x21F87171),          // .13
    codeBg = ZCodeBgDark,
    codeBorder = Color(0x2994A3BA),
    codePlain = Color(0xFFC9D4E8),
    scrim = Color(0x9E04070D),          // rgba(4,7,13,.62)
    grad = Brush.linearGradient(listOf(ZCyan, ZIndigo, ZViolet)),
    gradBtn = Brush.linearGradient(listOf(ZCyan, ZGradMid, ZViolet)),
    gradGlow = Color(0x8038BDF8),
    gradLine = Brush.verticalGradient(listOf(ZCyan, ZViolet)),
    gridline = Color(0x0E94A3BA),
    isDark = true
)

val LightZColors = ZColors(
    bg = ZBgLight,
    bg2 = ZBg2Light,
    surface = Color(0xB8FFFFFF),        // rgba(255,255,255,.72)
    surface2 = Color(0xEBFFFFFF),       // .92
    surface3 = Color(0x0F0D1930),       // .06
    border = Color(0x1A0D1930),         // .10
    border2 = Color(0x2E0D1930),        // .18
    text = ZTextLight,
    text2 = ZText2Light,
    text3 = ZText3Light,
    cyan = ZCyanL,
    violet = ZVioletL,
    green = ZGreenL,
    amber = ZAmberL,
    red = ZRedL,
    onGrad = ZOnGradLight,
    cyanBg = Color(0x1A0891B2),         // .10
    violetBg = Color(0x1A7C3AED),       // .10
    greenBg = Color(0x1C0E9F6E),        // .11
    amberBg = Color(0x1CD97706),        // .11
    redBg = Color(0x17DC2626),          // .09
    codeBg = ZCodeBg,
    codeBorder = Color(0x330D1930),
    codePlain = Color(0xFFC9D4E8),
    scrim = Color(0x730D1930),          // rgba(13,25,48,.45)
    grad = Brush.linearGradient(listOf(ZCyan, ZIndigo, ZViolet)),
    gradBtn = Brush.linearGradient(listOf(Color(0xFF0FB8D6), Color(0xFF6D5EF5), ZVioletL)),
    gradGlow = Color(0x734F7DEB),
    gradLine = Brush.verticalGradient(listOf(ZCyan, ZVioletL)),
    gridline = Color(0x0B0D1930),
    isDark = false
)

val LocalZColors = staticCompositionLocalOf { DarkZColors }
