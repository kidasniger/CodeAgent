package com.zagent.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * corr-082 — Tests du calcul tokens/coût : totalTokens, coutEstime (prix
 * manquants, flux interrompu avec usage partiel, grands volumes).
 */
class UsageRepositoryTest {

    @Test
    fun `totalTokens somme entrée et sortie`() {
        assertEquals(1500, UsageEntry(0, "glm", "p", 1000, 500, 0.0).totalTokens)
        assertEquals(0, UsageEntry(0, "glm", "p", 0, 0, 0.0).totalTokens)
    }

    @Test
    fun `coût null quand aucun prix renseigné`() {
        assertNull(coutEstime(10_000, 2_000, 0f, 0f))
        assertNull(coutEstime(10_000, 2_000, -1f, 5f)) // prix négatif = non renseigné
    }

    @Test
    fun `coût standard deux prix`() {
        // 10 000 tokens entrée à 3 $/M + 2 000 sortie à 15 $/M
        // = 0,03 + 0,03 = 0,06
        assertEquals(0.06, coutEstime(10_000, 2_000, 3f, 15f)!!, 1e-12)
    }

    @Test
    fun `flux interrompu — usage partiel compté pour zéro`() {
        // Le fournisseur n'a renvoyé que l'usage entrée : la sortie compte 0
        // et le coût reste calculable.
        assertEquals(0.03, coutEstime(10_000, 0, 3f, 15f)!!, 1e-12)
        assertEquals(0.0, coutEstime(0, 0, 3f, 15f)!!, 1e-12)
    }

    @Test
    fun `seul le prix de sortie suffit`() {
        assertEquals(0.03, coutEstime(0, 2_000, 0f, 15f)!!, 1e-12)
    }

    @Test
    fun `grands volumes — précision double`() {
        val c = coutEstime(1_000_000, 1_000_000, 3f, 15f)!!
        assertEquals(18.0, c, 1e-9)
    }
}
