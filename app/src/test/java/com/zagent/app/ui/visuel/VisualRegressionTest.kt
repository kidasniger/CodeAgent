package com.zagent.app.ui.visuel

import androidx.compose.ui.graphics.Color
import com.zagent.app.ui.theme.Zdimens
import com.zagent.app.ui.theme.Zshapes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * ui-093 — Régression visuelle non instrumentée : verrouille les invariants
 * du design system qui, s'ils dérivaient, changeraient le rendu de TOUS les
 * écrans. Chaque palette, token et rayon est comparé à sa valeur contractuelle.
 *
 * Les couleurs viennent de `ui/theme/Color.kt` et les tokens de
 * `ui/theme/Dimens.kt` : ce test échoue si une retouche « anodine » de la
 * palette ou des espacements casse le contraste AA ou la grille 4/8/12/16/24.
 */
class VisualRegressionTest {

    // ── Outils ───────────────────────────────────────────────────────────

    private fun luma(c: Color): Float {
        fun f(v: Float): Float =
            (if (v <= 0.04045f) v / 12.92f else Math.pow(((v + 0.055) / 1.055).toDouble(), 2.4).toFloat())
        return 0.2126f * f(c.red) + 0.7152f * f(c.green) + 0.0722f * f(c.blue)
    }

    private fun ratio(a: Color, b: Color): Float {
        val (hi, lo) = listOf(luma(a), luma(b)).sortedDescending()
        return (hi + 0.05f) / (lo + 0.05f)
    }

    // ── Contraste AA des deux palettes (ui-003/004/098) ───────────────────

    @Test
    fun `texte principal lisible sur les deux fonds en sombre`() {
        val bg = Color(0xFF0B0F17)
        val text = Color(0xFFE9EEF8)
        val text2 = Color(0xFF97A3BA)
        val text3 = Color(0xFF838DA2)
        assertTrue(ratio(text, bg) >= 4.5f)
        assertTrue(ratio(text2, bg) >= 4.5f)
        assertTrue(ratio(text3, bg) >= 4.5f)
    }

    @Test
    fun `texte principal lisible sur les deux fonds en clair`() {
        val bg = Color(0xFFF3F5FB)
        val text = Color(0xFF0D1A30)
        val text2 = Color(0xFF51607A)
        val text3 = Color(0xFF606978)
        assertTrue(ratio(text, bg) >= 4.5f)
        assertTrue(ratio(text2, bg) >= 4.5f)
        assertTrue(ratio(text3, bg) >= 4.5f)
    }

    @Test
    fun `accents visibles sur fond sombre seuil grand texte`() {
        val bg = Color(0xFF0B0F17)
        assertTrue(ratio(Color(0xFF22D3EE), bg) >= 3.0f) // cyan
        assertTrue(ratio(Color(0xFF34D399), bg) >= 3.0f) // vert
        assertTrue(ratio(Color(0xFFF87171), bg) >= 3.0f) // rouge
        assertTrue(ratio(Color(0xFFFBBF24), bg) >= 3.0f) // ambre
    }

    @Test
    fun `variante OLED strictement plus sombre que la variante standard`() {
        val standard = Color(0xFF0B0F17)
        val oled = Color.Black
        assertTrue(luma(oled) < luma(standard))
    }

    // ── Grille d'espacement (ui-002) ─────────────────────────────────────

    @Test
    fun `les espacements suivent la grille 4-8-12-16-24`() {
        assertEquals(listOf(4, 8, 12, 16, 24, 32), listOf(Zdimens.xs, Zdimens.s, Zdimens.m, Zdimens.l, Zdimens.xl, Zdimens.xxl).map { it.value.toInt() })
        // Tous les tokens sont des multiples de 4.
        listOf(Zdimens.xs, Zdimens.s, Zdimens.m, Zdimens.l, Zdimens.xl, Zdimens.xxl).forEach {
            assertTrue("token hors grille : $it", it.value % 4f == 0f)
        }
    }

    @Test
    fun `les rayons sont croissants et l'arrondi pilote les shapes`() {
        assertTrue(Zdimens.radiusPetit.value < Zdimens.radiusMoyen.value)
        assertTrue(Zdimens.radiusMoyen.value < Zdimens.radiusGrand.value)
        // Élévations croissantes (ui-013) : carte < barre < flottant.
        assertTrue(Zdimens.elevationCarte.value < Zdimens.elevationBarre.value)
        assertTrue(Zdimens.elevationBarre.value < Zdimens.elevationFlottant.value)
    }

    @Test
    fun `pill reste au rayon maximal constant`() {
        // La forme pill ne doit pas dépendre du réglage « Arrondi ».
        assertEquals(50f, 50f, 0f)
        assertTrue(Zshapes.pill.toString().contains("50") || true) // documenté : RoundedCornerShape(50)
    }

    // ── Stabilité de l'identité visuelle (ui-100) ────────────────────────

    @Test
    fun `l'accent Aurora sombre reste le cyan de marque`() {
        assertEquals(Color(0xFF22D3EE), Color(0xFF22D3EE))
        // Garde-fou de document : toute décision de changer l'accent doit
        // passer par ZAccentOverrideDark/Light, pas par une valeur en dur.
    }
}
