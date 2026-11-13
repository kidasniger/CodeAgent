package com.zagent.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * env-002 / session 16 — Tests JVM purs de la logique d'environnement
 * (aucun réseau, aucun Android).
 */
class EnvParseTest {

    // ── depotDeOrigine ──────────────────────────────────────────────────────

    @Test
    fun `https simple`() {
        assertEquals("kidasniger/zagent-playground", EnvParse.depotDeOrigine("https://github.com/kidasniger/zagent-playground.git"))
    }

    @Test
    fun `https sans suffixe git`() {
        assertEquals("kidasniger/CodeAgent", EnvParse.depotDeOrigine("https://github.com/kidasniger/CodeAgent"))
    }

    @Test
    fun `https avec jeton dans l url ne fuit pas le jeton`() {
        val url = "https://git:github_pat_ABCDEF123@github.com/kidasniger/CodeAgent.git"
        assertEquals("kidasniger/CodeAgent", EnvParse.depotDeOrigine(url))
    }

    @Test
    fun `ssh git@`() {
        assertEquals("octocat/mon-depôt", EnvParse.depotDeOrigine("git@github.com:octocat/mon-depôt.git"))
    }

    @Test
    fun `https avec user pass arbitraire`() {
        assertEquals("a/b", EnvParse.depotDeOrigine("https://quelquun:motdepasse@github.com/a/b"))
    }

    @Test
    fun `url non github null`() {
        assertNull(EnvParse.depotDeOrigine("https://gitlab.com/a/b"))
        assertNull(EnvParse.depotDeOrigine(""))
        assertNull(EnvParse.depotDeOrigine("github.com/sans-repo"))
    }

    @Test
    fun `segment invalide null`() {
        assertNull(EnvParse.depotDeOrigine("https://github.com/un seul/deux"))
    }

    // ── compteConflits ──────────────────────────────────────────────────────

    @Test
    fun `aucun conflit`() {
        assertEquals(0, EnvParse.compteConflits(""))
        assertEquals(0, EnvParse.compteConflits(" M fichier.txt\n?? nouveau.md\n"))
    }

    @Test
    fun `codes de conflit v1 comptés`() {
        assertEquals(3, EnvParse.compteConflits("UU a.txt\nAA b.txt\nUD c.txt\n M d.txt\n"))
    }

    @Test
    fun `lignes courtes ignorées sans planter`() {
        assertEquals(0, EnvParse.compteConflits("U\n\nX"))
    }

    // ── compteModifs ────────────────────────────────────────────────────────

    @Test
    fun `modifs et untracked comptés`() {
        assertEquals(2, EnvParse.compteModifs(" M a.txt\n?? b.md\n"))
    }

    @Test
    fun `l en-tete branche ne compte pas`() {
        assertEquals(1, EnvParse.compteModifs("## main...origin/main [ahead 1]\n M a.txt\n"))
    }

    @Test
    fun `vide zero`() {
        assertEquals(0, EnvParse.compteModifs(""))
    }

    // ── compteRevList ───────────────────────────────────────────────────────

    @Test
    fun `compteur simple`() {
        assertEquals(3, EnvParse.compteRevList("3\n"))
    }

    @Test
    fun `compteur zero`() {
        assertEquals(0, EnvParse.compteRevList("0"))
    }

    @Test
    fun `sortie illisible moins un`() {
        assertEquals(-1, EnvParse.compteRevList("fatal: mauvaise révision"))
        assertEquals(-1, EnvParse.compteRevList(""))
    }
}
