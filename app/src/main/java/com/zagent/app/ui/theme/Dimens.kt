package com.zagent.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ui-002 — Design tokens centralisés : espacements, rayons et élévations.
 *
 * Avant : les valeurs étaient dispersées en littéraux `dp` dans les écrans
 * (audit ui-001 : ~1 900 littéraux, 94 % déjà sur la grille 4/8/12/16/24).
 * Les nouveaux composants et les retouches doivent piocher ici ; les
 * littéraux existants convergent vers ces tokens au fil des refactorings.
 *
 * Grille d'espacement : 4 (serré), 8 (courant), 12 (aéré), 16 (section),
 * 24 (page). Tout écart doit être justifié par l'optique.
 */
object Zdimens {
    // ── Espacements ──────────────────────────────────────────────────────
    val xs: Dp = 4.dp      // entre icône et texte, padding interne serré
    val s: Dp = 8.dp       // espacement courant dans une ligne
    val m: Dp = 12.dp      // padding de cartes, listes aérées
    val l: Dp = 16.dp      // marge de section
    val xl: Dp = 24.dp     // marge de page
    val xxl: Dp = 32.dp    // respiration majeure (héros, onboarding)

    // ── Rayons (complètent LocalZRadius piloté par le réglage « Arrondi ») ──
    val radiusPetit: Dp = 8.dp    // chips, badges, champs
    val radiusMoyen: Dp = 16.dp   // cartes, bulles (défaut LocalZRadius)
    val radiusGrand: Dp = 24.dp   // feuilles, grandes surfaces

    // ── Élévations (ui-013 — ombres cohérentes sur toutes les cartes) ──────
    val elevationPlate: Dp = 0.dp     // surfaces sur fond, séparées par bordure
    val elevationCarte: Dp = 2.dp     // cartes posées (verre FASTEST)
    val elevationBarre: Dp = 10.dp    // barres flottantes, tab bar (verre BALANCED)
    val elevationFlottant: Dp = 18.dp // feuilles modales, menus (verre QUALITY)

    // ── Tailles d'icônes (ui-014 — jeu d'icônes uniforme) ──────────────────
    val iconS: Dp = 16.dp    // icône inline dans du texte
    val iconM: Dp = 20.dp    // icône de bouton / barre (défaut)
    val iconL: Dp = 24.dp    // icône de carte / section
    val iconXl: Dp = 32.dp   // illustration d'état vide
}
