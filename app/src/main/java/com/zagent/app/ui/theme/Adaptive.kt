package com.zagent.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * resp-01 — Kit responsive : bornes de hauteur calculées sur l'écran réel.
 *
 * Un seul téléphone de référence n'existe pas : écrans de 320 à 480 dp de
 * large, paysage autour de 360 dp de haut, échelle de police jusqu'à 2.0.
 * Les panneaux dont la hauteur était figée (dialogues 300/320/360 dp,
 * consoles 140/180 dp) débordaient de la fenêtre en paysage ou sur les
 * petits téléphones. Chaque borne est désormais le minimum entre la taille
 * voulue (composée pour un téléphone standard) et une fraction de la hauteur
 * réelle de l'écran : le contenu reste visible et scrollable partout, et
 * garde exactement sa taille historique sur un téléphone portrait classique.
 */

/** Hauteur de l'écran en dp (approximation suffisante pour fixer des bornes). */
@Composable
fun hauteurEcranDp(): Dp = LocalConfiguration.current.screenHeightDp.dp

/**
 * Borne une hauteur voulue par une fraction de l'écran :
 * `hauteurBorneDp(360.dp, 0.55f)` vaut 360 dp sur un téléphone portrait
 * standard, mais rétrécit en paysage ou sur un petit écran pour que le
 * panneau ne dépasse jamais la fenêtre (contenu scrollable à l'intérieur).
 */
@Composable
fun hauteurBorneDp(cible: Dp, fraction: Float): Dp {
    val h = hauteurEcranDp() * fraction
    return if (h < cible) h else cible
}
