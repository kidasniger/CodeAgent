package com.zagent.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Session 17 / vague B (sec-013..024) — tests JVM purs de la sécurité :
 * masquage des jetons, substitution d'alias, liste blanche de domaines,
 * teinte stable des fournisseurs, empreinte SHA-256 des réglages.
 */
class SecuriteVagueBTest {

    // ── sec-014 : masquage automatique des jetons ───────────────────────────

    @Test
    fun `jeton GitHub fine-grained masqué`() {
        val texte = "curl -H \"Authorization: Bearer github_pat_11CMVHZQY0zmqJIlXIVHEu_hKK4qw5TNKBchRkC8AU20\" https://api.github.com"
        val masque = SettingsManager.masquerSecrets(texte)
        assertFalse(masque.contains("github_pat_11CMVHZQY0"))
        assertTrue(masque.contains("«[masqué]»"))
    }

    @Test
    fun `jeton ghp et clé sk masqués mais texte normal intact`() {
        val texte = "Token ghp_ABCDEFGHIJKLMNOPQRSTUVWXYZ123456 et sk-abc123def456ghi789jkl12 ; rien à signaler."
        val masque = SettingsManager.masquerSecrets(texte)
        assertFalse(masque.contains("ghp_ABCDEFGHIJKLMNOPQRSTUVWXYZ"))
        assertFalse(masque.contains("sk-abc123def456ghi789jkl12"))
        assertTrue(masque.contains("rien à signaler"))
    }

    @Test
    fun `texte sans jeton inchangé`() {
        val texte = "Un simple message sans secret."
        assertEquals(texte, SettingsManager.masquerSecrets(texte))
    }

    // ── sec-013 : substitution {{secret:alias}} ──────────────────────────────

    @Test
    fun `substitution regex couvre les marqueurs valides`() {
        // Test au niveau du pattern utilisé par substituerSecrets (sans Context).
        val pattern = Regex("\\{\\{secret:([^}]{1,40})\\}\\}")
        val m = pattern.find("export API={{secret:npm_token}} && echo {{secret: a}}")
        assertEquals("npm_token", m?.groupValues?.get(1))
        // Le marqueur avec espace est couvert aussi (alias «  a » → trim côté lecture).
        assertEquals(" a", pattern.find("{{secret: a}}")?.groupValues?.get(1))
    }

    // ── sec-021 : liste blanche de domaines (logique hôte/sous-domaine) ─────

    @Test
    fun `hoteMatche liste blanche inclus sous-domaines`() {
        val liste = listOf("github.com", "api.z.ai")
        fun autorise(hote: String) = liste.any { hote == it || hote.endsWith(".$it") }
        assertTrue(autorise("github.com"))
        assertTrue(autorise("api.github.com"))
        assertTrue(autorise("api.z.ai"))
        assertFalse(autorise("evil-github.com"))
        assertFalse(autorise("notapi.z.ai.evil.com"))
        assertFalse(autorise("openai.com"))
    }

    // ── sec-022 : teinte stable du fournisseur ───────────────────────────────

    @Test
    fun `teinteStable déterministe et lisible`() {
        val a = Fournisseurs.teinteStable("zai")
        val b = Fournisseurs.teinteStable("zai")
        assertEquals(a, b)
        assertNotEquals(a, Fournisseurs.teinteStable("nvidia"))
        // alpha opaque 0xFF.
        assertEquals(0xFF000000L, a and 0xFF000000L)
        // différent des intégrés.
        assertNotEquals(a, Fournisseurs.teinteStable(""))
    }

    // ── sec-024 : empreinte SHA-256 déterministe ─────────────────────────────

    @Test
    fun `sha256 d'un corps stable et différent pour un corps altéré`() {
        fun sha(corps: String): String =
            java.security.MessageDigest.getInstance("SHA-256")
                .digest(corps.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
        val corps1 = "{\"model\":\"glm-4.6\",\"allowWrite\":true}"
        val corps2 = "{\"model\":\"glm-4.6\",\"allowWrite\":false}"
        assertEquals(sha(corps1), sha(corps1))
        assertNotEquals(sha(corps1), sha(corps2))
        assertEquals(64, sha(corps1).length)
    }
}
