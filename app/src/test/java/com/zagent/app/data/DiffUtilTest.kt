package com.zagent.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * corr-070 — Tests de data/DiffUtil.kt : fichiers vides, CRLF, très gros diff
 * (bascule LCS → naïf au-delà de 300 lignes par côté).
 */
class DiffUtilTest {

    @Test
    fun `ancien null = création intégrale`() {
        val r = DiffUtil.diff(null, "a\nb")
        assertEquals(2, r.added)
        assertEquals(0, r.removed)
        assertEquals(false, r.isUpdate)
        assertTrue(r.lines.all { it.kind == DiffUtil.Kind.ADD })
    }

    @Test
    fun `contenu vide ancien, nouveau vide`() {
        val r = DiffUtil.diff("", "")
        assertEquals(0, r.added)
        assertEquals(0, r.removed)
    }

    @Test
    fun `nouveau vide = suppression intégrale`() {
        val r = DiffUtil.diff("a\nb\n", "")
        assertEquals(0, r.added)
        assertEquals(3, r.removed) // "a","b","" (fins de ligne)
    }

    @Test
    fun `CRLF - les lignes ne contiennent pas de caractère de retour`() {
        val old = "ligne1\r\nligne2\r\n"
        val new = "ligne1\r\nligne2 modifiée\r\n"
        val r = DiffUtil.diff(old, new)
        // "ligne2" doit apparaître comme DEL et "ligne2 modifiée" comme ADD
        val texts = r.lines.map { it.text }
        assertTrue(texts.contains("ligne2 modifiée"))
        assertTrue(r.lines.any { it.kind == DiffUtil.Kind.DEL && it.text == "ligne2" })
        assertTrue(r.lines.none { it.text.contains('\r') })
    }

    @Test
    fun `compteurs ajout-suppression cohérents`() {
        val old = "a\nb\nc\nd"
        val new = "a\nX\nY\nc\nd"
        val r = DiffUtil.diff(old, new)
        assertEquals(2, r.added)
        assertEquals(1, r.removed)
        assertTrue(r.isUpdate)
    }

    @Test
    fun `identique = contexte pur`() {
        val r = DiffUtil.diff("a\nb", "a\nb")
        assertEquals(0, r.added)
        assertEquals(0, r.removed)
        assertTrue(r.lines.all { it.kind == DiffUtil.Kind.CTX })
    }

    @Test
    fun `gros diff bascule en mode naïf sans exception`() {
        val sbOld = StringBuilder()
        val sbNew = StringBuilder()
        repeat(600) { i ->
            sbOld.append("ancien $i\n")
            sbNew.append("nouveau $i\n")
        }
        val r = DiffUtil.diff(sbOld.toString(), sbNew.toString())
        assertEquals(600, r.added)
        assertEquals(600, r.removed)
    }

    @Test
    fun `gros diff partiel LCS correct`() {
        val old = (1..400).joinToString("\n") { "ligne $it" }
        val new = (1..400).joinToString("\n") { if (it == 250) "ligne modifiée" else "ligne $it" }
        val r = DiffUtil.diff(old, new)
        assertEquals(1, r.added)
        assertEquals(1, r.removed)
    }

    @Test
    fun `numérotation présente sur les lignes`() {
        val r = DiffUtil.diff("a", "b")
        val del = r.lines.first { it.kind == DiffUtil.Kind.DEL }
        val add = r.lines.first { it.kind == DiffUtil.Kind.ADD }
        assertEquals(1, del.oldNum)
        assertNullNumber(add.oldNum)
        assertEquals(1, add.newNum)
    }

    private fun assertNullNumber(n: Int?) = assertEquals(null, n)
}
