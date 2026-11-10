@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.zagent.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.zagent.app.R

// ─────────────────────────────────────────────────────────────────────────────
// MàJ 98 — une seule police variable par famille.
// Les anciens fichiers inter_regular/medium/semibold/bold étaient LE MÊME
// fichier variable copié quatre fois (md5 identiques, 122 592 octets chacun),
// idem pour jetbrainsmono (2×) et spacegrotesk_semibold (= bold). Un seul
// fichier + FontVariation.weight() :
//   - allège l'APK d'environ 490 Ko (5 fichiers supprimés) ;
//   - produit de vraies graisses : avant, chaque « poids » rendait l'instance
//     par défaut du fichier variable, donc tout le texte Inter était en 400.
// L'axe wght est supporté nativement à partir d'Android 8.0 (API 26) = minSdk.
// ─────────────────────────────────────────────────────────────────────────────

// Inter — interface (400-700), fichier variable unique (axe wght 100-900).
val Inter = FontFamily(
    Font(
        R.font.inter_variable, FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        R.font.inter_variable, FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        R.font.inter_variable, FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    ),
    Font(
        R.font.inter_variable, FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    )
)

// Space Grotesk — titres & affichage. Deux fichiers variables distincts
// (md5 différents, axe wght 300-700) : medium pour le médium, bold pour
// semi-bold et bold (Compose choisit le fichier au poids le plus proche).
val SpaceGrotesk = FontFamily(
    Font(R.font.spacegrotesk_medium, FontWeight.Medium),
    Font(R.font.spacegrotesk_bold, FontWeight.SemiBold),
    Font(R.font.spacegrotesk_bold, FontWeight.Bold)
)

// JetBrains Mono — code, chemins & noms d'outils (400/600),
// fichier variable unique (axe wght 100-800).
val JetBrainsMono = FontFamily(
    Font(
        R.font.jetbrainsmono_variable, FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        R.font.jetbrainsmono_variable, FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    )
)

/**
 * MàJ 98 — titres serif du preset Minimal (style calme proche de Claude).
 * [FontFamily.Serif] résout vers Noto Serif sur Android : zéro octet ajouté à
 * l'APK — embarquer Source Serif 4 variable coûterait ≈ 1,2 Mo, ce qui
 * annulerait l'allègement obtenu ci-dessus. Pour l'embarquer quand même :
 * déposer le fichier variable dans res/font/ (nom : sourceserif4_variable.ttf)
 * puis remplacer la valeur par FontFamily(Font(R.font.sourceserif4_variable, …)).
 */
val SerifTitres: FontFamily = FontFamily.Serif
    fontFamily = JetBrainsMono,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp
)

/**
 * Échelle typographique du design system :
 * Display 32/40 · Title 25/32 & 20/28 · Body 16/24 & 14/20 · Label 12.
 */
val ZTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold,
        fontSize = 32.sp, lineHeight = 40.sp
    ),
    displayMedium = TextStyle(
        fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold,
        fontSize = 28.sp, lineHeight = 36.sp
    ),
    displaySmall = TextStyle(
        fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, lineHeight = 32.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold,
        fontSize = 28.sp, lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, lineHeight = 32.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp, lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp, lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.3.sp
    )
)
