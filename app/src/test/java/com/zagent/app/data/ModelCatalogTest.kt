package com.zagent.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

// ─────────────────────────────────────────────────────────────────────────────
// Intégration NVIDIA NIM — la requête doit toujours partir vers l'URL du bon
// fournisseur, sans écraser une passerelle personnalisée.
// ─────────────────────────────────────────────────────────────────────────────
class ModelCatalogTest {

    @Test
    fun `modèle NVIDIA sans URL perso - va vers NVIDIA`() {
        assertEquals(
            ModelCatalog.URL_NVIDIA,
            ModelCatalog.urlPourModele("nvidia/nemotron-3-ultra-550b-a55b", "")
        )
        assertEquals(
            ModelCatalog.URL_NVIDIA,
            ModelCatalog.urlPourModele("meta/llama-guard-4-12b", null)
        )
    }

    @Test
    fun `modèle NVIDIA avec URL z_ai connue - bascule vers NVIDIA`() {
        assertEquals(
            ModelCatalog.URL_NVIDIA,
            ModelCatalog.urlPourModele("poolside/laguna-xs-2.1", ModelCatalog.URL_ZAI)
        )
    }

    @Test
    fun `modèle GLM avec URL NVIDIA connue - rebascule vers z_ai`() {
        assertEquals(
            ModelCatalog.URL_ZAI,
            ModelCatalog.urlPourModele("glm-4.5-flash", ModelCatalog.URL_NVIDIA)
        )
    }

    @Test
    fun `passerelle personnalisée jamais écrasée`() {
        val proxy = "https://mon-proxy.local/v1/chat/completions"
        assertEquals(
            proxy,
            ModelCatalog.urlPourModele("nvidia/nemotron-3.5-bolt-e4b", proxy)
        )
        assertEquals(
            proxy,
            ModelCatalog.urlPourModele("glm-4.7-flash", proxy)
        )
        assertEquals(
            proxy,
            ModelCatalog.urlPourModele("org/modele-inconnu", proxy)
        )
    }

    @Test
    fun `modèle inconnu sans URL perso - défaut z_ai`() {
        assertEquals(
            ModelCatalog.URL_ZAI,
            ModelCatalog.urlPourModele("org/modele-inconnu", "")
        )
        assertEquals(
            ModelCatalog.URL_ZAI,
            ModelCatalog.urlPourModele("org/modele-inconnu", "   ")
        )
    }

    @Test
    fun `catalogue NVIDIA complet et non vide`() {
        // 12 modèles de discussion/code + 5 spécialisés (modèles en timeout et
        // retirés du catalogue NVIDIA exclus, comme testé par l'utilisateur).
        assertEquals(12, ModelCatalog.nvidiaChat.size)
        assertEquals(5, ModelCatalog.nvidiaSpecialises.size)
        assertEquals(2, ModelCatalog.glm.size)
        (ModelCatalog.nvidiaChat + ModelCatalog.nvidiaSpecialises).forEach {
            assertEquals(ModelCatalog.URL_NVIDIA, it.baseUrl)
        }
    }
}
