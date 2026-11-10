package com.zagent.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// ─────────────────────────────────────────────────────────────────────────────
// MàJ 101-104 — Moteur multi-fournisseurs :
//  101 · Fournisseurs (détection auto, persistance JSON, clés par API)
//  102 · Capacités par modèle (tags, filtres)
//  103 · Routage de la clé + chaîne de repli
// Tout est testé en pur JVM (sans contexte Android).
// ─────────────────────────────────────────────────────────────────────────────
class MoteurMultiFournisseursTest {

    // ── MàJ 101 : détection automatique ─────────────────────────────────────

    @Test
    fun `clé nvapi - détectée comme NVIDIA`() {
        assertEquals(Fournisseurs.ID_NVIDIA, Fournisseurs.detecter("nvapi-AbCdEf123456"))
        assertEquals(Fournisseurs.ID_NVIDIA, Fournisseurs.detecter("NVAPI-toto"))
    }

    @Test
    fun `URL z_ai détectée`() {
        assertEquals(Fournisseurs.ID_ZAI, Fournisseurs.detecter("https://api.z.ai/api/paas/v4/chat/completions"))
    }

    @Test
    fun `URL NVIDIA détectée`() {
        assertEquals(Fournisseurs.ID_NVIDIA, Fournisseurs.detecter("https://integrate.api.nvidia.com/v1/chat/completions"))
    }

    @Test
    fun `URL inconnue - pas de détection`() {
        assertNull(Fournisseurs.detecter("https://api.groq.com/openai/v1/chat/completions"))
        assertNull(Fournisseurs.detecter(""))
        assertNull(Fournisseurs.detecter("   "))
    }

    // ── MàJ 101 : persistance JSON + fournisseurs intégrés ───────────────────

    @Test
    fun `aller-retour JSON avec clé par fournisseur`() {
        val liste = listOf(
            Fournisseur("zai", "z.ai (GLM)", ModelCatalog.URL_ZAI, "sk-za-abc"),
            Fournisseur("nvidia", "NVIDIA NIM", ModelCatalog.URL_NVIDIA, "nvapi-xyz"),
            Fournisseur("perso-42", "Groq", "https://api.groq.com/openai/v1/chat/completions", "gsk_1", personnalise = true)
        )
        val decode = Fournisseurs.decode(Fournisseurs.encode(liste))
        assertEquals(3, decode.size)
        assertEquals("sk-za-abc", decode.first { it.id == "zai" }.cle)
        assertEquals("nvapi-xyz", decode.first { it.id == "nvidia" }.cle)
        assertEquals("gsk_1", decode.first { it.id == "perso-42" }.cle)
        assertTrue(decode.first { it.id == "perso-42" }.personnalise)
    }

    @Test
    fun `les intégrés réapparaissent après un décodage partiel`() {
        // L'utilisateur a "supprimé" z.ai dans une sauvegarde corrompue : complete() le replace.
        val decode = Fournisseurs.decode("""[{"id":"nvidia","nom":"NVIDIA NIM","baseUrl":"${ModelCatalog.URL_NVIDIA}","cle":"nvapi-1"}]""")
        assertTrue(decode.any { it.id == Fournisseurs.ID_ZAI })
        assertTrue(decode.any { it.id == Fournisseurs.ID_NVIDIA })
    }

    @Test
    fun `JSON corrompu - repli sur les intégrés sans crash`() {
        val liste = Fournisseurs.decode("{{{ pas du json")
        assertEquals(Fournisseurs.integres().size, liste.size)
    }

    @Test
    fun `fournisseurs intégrés ont les bonnes URLs`() {
        val integres = Fournisseurs.integres()
        assertEquals(ModelCatalog.URL_ZAI, integres.first { it.id == Fournisseurs.ID_ZAI }.baseUrl)
        assertEquals(ModelCatalog.URL_NVIDIA, integres.first { it.id == Fournisseurs.ID_NVIDIA }.baseUrl)
    }

    // ── MàJ 102 : capacités ──────────────────────────────────────────────────

    @Test
    fun `tout modèle conversationnel sait faire du texte ou du code`() {
        ModelCatalog.all.filter { !it.specialized }.forEach { m ->
            assertTrue("Modèle sans capacité utile : " + m.id, m.conversationnel)
        }
    }

    @Test
    fun `les spécialisés ne sont pas conversationnels`() {
        ModelCatalog.all.filter { it.specialized }.forEach { m ->
            assertFalse(m.conversationnel)
        }
    }

    @Test
    fun `modèles vision bien étiquetés`() {
        val vision = ModelCatalog.conversationnels(Capacite.VISION).map { it.id }
        assertTrue(vision.contains("nvidia/nemotron-3.5-super-vl-120b-a12b"))
        assertTrue(vision.contains("meta/llama-3.2-90b-vision-instruct"))
        assertFalse(vision.contains("poolside/laguna-xs-2.1"))
    }

    @Test
    fun `filtre code contient Laguna mais pas Bolt`() {
        val code = ModelCatalog.conversationnels(Capacite.CODE).map { it.id }
        assertTrue(code.contains("poolside/laguna-xs-2.1"))
        assertFalse(code.contains("nvidia/nemotron-3.5-bolt-e4b"))
    }

    @Test
    fun `favoris épinglés en tête`() {
        val liste = ModelCatalog.conversationnels(null, favoris = setOf("nvidia/nemotron-3.5-bolt-e4b"))
        assertEquals("nvidia/nemotron-3.5-bolt-e4b", liste.first().id)
    }

    @Test
    fun `fournisseurId dérivé de l'URL`() {
        assertEquals(Fournisseurs.ID_ZAI, ModelCatalog.find("glm-4.5-flash")!!.fournisseurId)
        assertEquals(Fournisseurs.ID_NVIDIA, ModelCatalog.find("poolside/laguna-xs-2.1")!!.fournisseurId)
    }

    // ── MàJ 103 : routage de la clé + chaîne de repli ────────────────────────

    @Test
    fun `la clé suit le fournisseur du modèle`() {
        val fournisseurs = listOf(
            Fournisseur("zai", "z.ai", ModelCatalog.URL_ZAI, "cle-zai"),
            Fournisseur("nvidia", "NVIDIA NIM", ModelCatalog.URL_NVIDIA, "cle-nvidia")
        )
        assertEquals("cle-nvidia", ModelCatalog.fournisseurPourModele("poolside/laguna-xs-2.1", fournisseurs)!!.cle)
        assertEquals("cle-zai", ModelCatalog.fournisseurPourModele("glm-4.5-flash", fournisseurs)!!.cle)
        assertNull(ModelCatalog.fournisseurPourModele("org/inconnu", fournisseurs))
    }

    @Test
    fun `fournisseur actif préféré à un doublon inactif`() {
        val fournisseurs = listOf(
            Fournisseur("nvidia", "NVIDIA NIM", ModelCatalog.URL_NVIDIA, "", actif = false),
            Fournisseur("nvidia-2", "NVIDIA NIM (bis)", ModelCatalog.URL_NVIDIA, "cle-bis", actif = true)
        )
        assertEquals("cle-bis", ModelCatalog.fournisseurPourModele("meta/muse-glimmer-30b", fournisseurs)!!.cle)
    }

    @Test
    fun `chaîne de repli - même fournisseur d'abord, jamais de spécialisés`() {
        val chaine = ModelCatalog.chaineRepli("glm-4.5-flash")
        assertEquals("glm-4.7-flash", chaine.first()) // autre GLM, même fournisseur
        assertFalse(chaine.contains("glm-4.5-flash"))
        assertFalse(chaine.any { ModelCatalog.find(it)!!.specialized })
        // puis les NVIDIA, triés du plus rapide au plus lourd
        val nvidia = chaine.dropWhile { ModelCatalog.find(it)!!.baseUrl == ModelCatalog.URL_ZAI }
        assertTrue(nvidia.isNotEmpty())
        assertEquals(
            nvidia.map { ModelCatalog.find(it)!!.speed },
            nvidia.map { ModelCatalog.find(it)!!.speed }.sortedDescending()
        )
    }

    @Test
    fun `chaîne de repli d'un modèle NVIDIA traverse les fournisseurs`() {
        val chaine = ModelCatalog.chaineRepli("nvidia/nemotron-3-ultra-550b-a55b")
        assertTrue(chaine.contains("glm-4.5-flash"))
        val dernier = ModelCatalog.find(chaine.last())!!
        assertEquals(ModelCatalog.URL_ZAI, dernier.baseUrl)
    }
}
