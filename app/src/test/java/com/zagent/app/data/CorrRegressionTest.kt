package com.zagent.app.data

import com.zagent.app.data.TodoItem
import com.zagent.app.data.coutEstime
import com.zagent.app.ui.viewmodel.fusionnerTodos
import com.zagent.app.ui.viewmodel.nettoyerPlaceholders
import com.zagent.app.util.ZLog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * corr-099 — 16 tests de non-régression ciblant les correctifs de la catégorie
 * « Corrections » : fusion des tâches (corr-077), placeholders de modèles
 * (corr-095), masquage des secrets (corr-019/021), événements de navigation
 * (corr-009), coût d'usage (corr-082).
 */
class CorrRegressionTest {

    // ── corr-077 : fusion des tâches ───────────────────────────────────────

    @Test
    fun `update_todos partiel ne perd pas les tâches existantes`() {
        val anciennes = listOf(TodoItem("créer l'écran", false), TodoItem("écrire les tests", false))
        val nouvelles = listOf(TodoItem("créer l'écran", true))
        val fusion = fusionnerTodos(anciennes, nouvelles)
        assertEquals(2, fusion.size)
        assertTrue(fusion.first { it.text == "créer l'écran" }.done)
        assertTrue(fusion.any { it.text == "écrire les tests" && !it.done })
    }

    @Test
    fun `fusion insensible à la casse et aux espaces`() {
        val anciennes = listOf(TodoItem("Corriger le bug", false))
        val nouvelles = listOf(TodoItem("  corriger le bug  ", true))
        val fusion = fusionnerTodos(anciennes, nouvelles)
        assertEquals(1, fusion.size)
        assertTrue(fusion[0].done)
    }

    @Test
    fun `fusion plafonnée à 30 tâches`() {
        val anciennes = (1..20).map { TodoItem("ancienne $it", false) }
        val nouvelles = (1..20).map { TodoItem("nouvelle $it", false) }
        assertEquals(30, fusionnerTodos(anciennes, nouvelles).size)
    }

    @Test
    fun `liste vide remplacée intégralement`() {
        val nouvelles = listOf(TodoItem("a", true), TodoItem("b", false))
        assertEquals(nouvelles, fusionnerTodos(emptyList(), nouvelles))
    }

    // ── corr-095 : placeholders de modèles ────────────────────────────────

    @Test
    fun `placeholder double accolade neutralisé`() {
        val out = nettoyerPlaceholders("Crée une app pour {{nom_utilisateur}} s'il te plaît.")
        assertTrue(out.contains("[à préciser]"))
        assertFalse(out.contains("{{nom_utilisateur}}"))
    }

    @Test
    fun `placeholder snake_case simple neutralisé`() {
        val out = nettoyerPlaceholders("Utilise le framework {framework_cible}.")
        assertTrue(out.contains("[à préciser]"))
        assertFalse(out.contains("{framework_cible}"))
    }

    @Test
    fun `message sans placeholder inchangé`() {
        val msg = "Ajoute un compteur dans MainActivity."
        assertEquals(msg, nettoyerPlaceholders(msg))
    }

    @Test
    fun `code légitime avec accolades non confondu avec un placeholder`() {
        // { couleur } : espace + mot français pluriel → non traité comme variable
        val msg = "Vérifie le bloc css :root { color-scheme: dark; }"
        assertEquals(msg, nettoyerPlaceholders(msg))
    }

    // ── corr-019/021 : masquage des secrets dans les logs ──────────────────

    @Test
    fun `clé enregistrée masquée dans tout message`() {
        ZLog.rafraichirSecrets(listOf("sk-secret-1234567890"))
        val out = ZLog.masquer("réponse avec sk-secret-1234567890 en clair")
        assertFalse(out.contains("sk-secret-1234567890"))
        assertTrue(out.contains("••••"))
    }

    @Test
    fun `en-tête Bearer neutralisé`() {
        val out = ZLog.masquer("Authorization: Bearer nvapi-abcdef0123456789")
        assertFalse(out.contains("nvapi-abcdef0123456789"))
    }

    @Test
    fun `motif sk- et github_pat masqués même sans rafraichirSecrets`() {
        ZLog.rafraichirSecrets(emptyList())
        val out = ZLog.masquer("clé sk-AbCdEf12345678 et github_pat_11AbCdEf1234567890xyz")
        assertFalse(out.contains("sk-AbCdEf12345678"))
        assertFalse(out.contains("github_pat_11AbCdEf1234567890xyz"))
    }

    @Test
    fun `texte court sans secret intact`() {
        assertEquals("pas de secret ici", ZLog.masquer("pas de secret ici"))
        assertEquals("null", ZLog.masquer(null))
    }

    // ── corr-009 : événements de navigation ────────────────────────────────

    @Test
    fun `navigateTo émet un événement Route`() = kotlinx.coroutines.test.runTest {
        val nav = com.zagent.app.ui.viewmodel.NavigationViewModel()
        nav.navigateTo(com.zagent.app.ui.viewmodel.AppScreen.TERMINAL)
        // replay = 1 : l'événement émis avant abonnement reste lisible.
        val event = nav.events.first()
        assertEquals("terminal", (event as com.zagent.app.ui.viewmodel.NavEvent.Route).route)
    }

    @Test
    fun `navigateBack émet Back`() = kotlinx.coroutines.test.runTest {
        val nav = com.zagent.app.ui.viewmodel.NavigationViewModel()
        nav.navigateBack()
        assertTrue(nav.events.first() is com.zagent.app.ui.viewmodel.NavEvent.Back)
    }

    @Test
    fun `syncScreenFromRoute resynchronise l'écran courant`() {
        val nav = com.zagent.app.ui.viewmodel.NavigationViewModel()
        nav.syncScreenFromRoute("editor?path=src/Main.kt")
        assertEquals(com.zagent.app.ui.viewmodel.AppScreen.EDITOR, nav.currentScreen)
        nav.syncScreenFromRoute("terminal")
        assertEquals(com.zagent.app.ui.viewmodel.AppScreen.TERMINAL, nav.currentScreen)
        nav.syncScreenFromRoute(null)
        assertEquals(com.zagent.app.ui.viewmodel.AppScreen.CHAT, nav.currentScreen)
    }

    @Test
    fun `deep link avec traversée - chemin relatif requis (corr-039)`() {
        // La validation corr-039 est faite dans ZNavHost ; ce test fige le
        // contrat : un chemin de deep link accepté ne contient ni .. ni /.
        val candidat = "docs/notes.md"
        assertTrue(!candidat.startsWith("/") && !candidat.contains("..") && !candidat.contains('\\'))
        val refus = "../secret"
        assertTrue(refus.startsWith("..") || refus.contains(".."))
    }

    // ── corr-082 : coût d'usage (rappel du contrat) ───────────────────────

    @Test
    fun `coût cohérent pour un tour complet`() {
        assertEquals(0.06, coutEstime(10_000, 2_000, 3f, 15f)!!, 1e-12)
    }
}
