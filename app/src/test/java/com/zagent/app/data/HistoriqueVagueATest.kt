package com.zagent.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Session 17 / vague A (hist-001..012) — tests JVM purs de l'historique
 * enrichi : recherche reasoning/toolLog, recomptage, tri secondaire,
 * archivage froid, corbeille réversible, statistiques, réconciliation
 * des orphelins (fichiers système exclus).
 */
class HistoriqueVagueATest {

    private fun repoVierge(): Pair<ConversationRepository, File> {
        val base = Files.createTempDirectory("zconv-test-").toFile()
        val repo = ConversationRepository(base, testMarker = true)
        return repo to base
    }

    private fun message(
        role: String,
        content: String?,
        time: Long = System.currentTimeMillis(),
        reasoning: String? = null,
        toolLog: String? = null,
        toolCalls: List<StoredToolCall>? = null,
        modele: String? = null
    ) = StoredMessage(
        role = role, content = content, time = time,
        reasoning = reasoning, toolLog = toolLog, toolCalls = toolCalls, modele = modele
    )

    // ── hist-001 : la recherche couvre reasoning et toolLog ─────────────────

    @Test
    fun `recherche trouve une occurence dans reasoning`() {
        val (repo, base) = repoVierge()
        try {
            repo.saveMessages("a", listOf(message("assistant", "Rien ici.", reasoning = "Le mot-clé XYZZY est décisif.")))
            repo.saveIndex(listOf(ConversationMeta("a", "Titre A", "", System.currentTimeMillis(), 1)))
            val hits = repo.searchContent("XYZZY", 5)
            assertEquals(1, hits.size)
            assertTrue(hits.first().snippet.contains("XYZZY"))
        } finally { base.deleteRecursively() }
    }

    @Test
    fun `recherche trouve une occurence dans toolLog`() {
        val (repo, base) = repoVierge()
        try {
            repo.saveMessages("b", listOf(message("assistant", null, toolLog = "read_file → QWERTZ détecté")))
            repo.saveIndex(listOf(ConversationMeta("b", "Titre B", "", System.currentTimeMillis(), 1)))
            assertEquals(1, repo.searchContent("QWERTZ", 5).size)
        } finally { base.deleteRecursively() }
    }

    // ── hist-006 : recomptage des messages depuis les fichiers ──────────────

    @Test
    fun `recompterMessages corrige un compte perime`() {
        val (repo, base) = repoVierge()
        try {
            val msgs = listOf(
                message("user", "Bonjour"),
                message("assistant", "Salut !"),
                message("user", "Encore")
            )
            repo.saveMessages("c", msgs)
            // L'index prétend 1 message (périmé).
            val corrigee = repo.recompterMessages(listOf(ConversationMeta("c", "C", "", 0L, 1)))
            assertEquals(3, corrigee.first().messageCount)
        } finally { base.deleteRecursively() }
    }

    // ── hist-007 : tri secondaire alphabétique ───────────────────────────────

    @Test
    fun `tri secondaire alphabétique à date égale`() {
        val (repo, base) = repoVierge()
        try {
            val t = System.currentTimeMillis()
            repo.saveIndex(listOf(
                ConversationMeta("x", "Zèbre", "", t, 1),
                ConversationMeta("y", "Alphabet", "", t, 1),
                ConversationMeta("z", "Milieu", "", t, 1)
            ))
            val titres = repo.loadIndex().map { it.title }
            assertEquals(listOf("Alphabet", "Milieu", "Zèbre"), titres)
        } finally { base.deleteRecursively() }
    }

    // ── hist-008 : archivage froid + désarchivage ────────────────────────────

    @Test
    fun `archivage froid compresse et dearchiver restaure`() {
        val (repo, base) = repoVierge()
        try {
            val ancien = System.currentTimeMillis() - 40L * 24 * 60 * 60 * 1000
            val msgs = listOf(
                message("user", "Première demande", ancien),
                message("assistant", "Réponse A", ancien + 1),
                message("user", "Suite", ancien + 2),
                message("assistant", "Réponse B finale", ancien + 3)
            )
            repo.saveMessages("d", msgs)
            repo.saveIndex(listOf(ConversationMeta("d", "Vieille", "", ancien + 3, 4)))
            val n = repo.archiverFroid(30)
            assertEquals(1, n)
            val meta = repo.loadIndex().first()
            assertTrue(meta.archiveFroide)
            // Compact = 1er user + dernier assistant.
            val compact = repo.loadMessages("d")
            assertEquals(2, compact.size)
            assertEquals("Première demande", compact[0].content)
            assertEquals("Réponse B finale", compact[1].content)
            // Le plein est conservé pour rouverture.
            repo.dearchiver("d")
            assertFalse(repo.loadIndex().first().archiveFroide)
            assertEquals(4, repo.loadMessages("d").size)
        } finally { base.deleteRecursively() }
    }

    // ── hist-009 : corbeille réversible ──────────────────────────────────────

    @Test
    fun `corbeille met liste restaure puis purge au-delà de 7 jours`() {
        val (repo, base) = repoVierge()
        try {
            val msgs = listOf(message("user", "À supprimer"), message("assistant", "Réponse"))
            repo.saveMessages("e", msgs)
            repo.saveIndex(listOf(ConversationMeta("e", "Supprimable", "", System.currentTimeMillis(), 2)))
            repo.mettreCorbeille("e")
            assertTrue(repo.loadIndex().isEmpty())
            assertTrue(repo.loadMessages("e").isEmpty())
            val corbeille = repo.listerCorbeille()
            assertEquals(1, corbeille.size)
            assertEquals("e", corbeille.first().id)
            repo.restaurerDeCorbeille(corbeille.first().nom)
            assertEquals(2, repo.loadMessages("e").size)
            assertEquals("Supprimable", repo.loadIndex().first().title)
            // Purge : une entrée vieille de 8 jours disparaît.
            val vieille = File(base, ".corbeille-1-e.json")
            vieille.writeText("[]") // timestamp 1 ms → dépassé
            repo.purgerCorbeille(7)
            assertFalse(vieille.exists())
        } finally { base.deleteRecursively() }
    }

    // ── hist-011 : statistiques ──────────────────────────────────────────────

    @Test
    fun `statistiques compte messages outils tokens et modeles`() {
        val (repo, base) = repoVierge()
        try {
            repo.saveMessages("f", listOf(
                message("user", "Premier message utilisateur", toolCalls = null),
                message(
                    "assistant", "Réponse",
                    toolCalls = listOf(
                        StoredToolCall("1", "read_file", "{}"),
                        StoredToolCall("2", "read_file", "{}"),
                        StoredToolCall("3", "edit_file", "{}")
                    ),
                    modele = "glm-4.6"
                )
            ))
            val s = repo.statistiques("f")
            assertEquals(2, s.messages)
            assertEquals(1, s.messagesUtilisateur)
            assertEquals(1, s.messagesAssistant)
            assertEquals(3, s.appelsOutils)
            assertEquals(2, s.outils["read_file"])
            assertEquals(1, s.outils["edit_file"])
            assertTrue(s.tokensEstimes > 0)
            assertEquals(1, s.modeles["glm-4.6"])
        } finally { base.deleteRecursively() }
    }

    // ── hist-012 : réconciliation compte les orphelins, ignore les fichiers système ──

    @Test
    fun `réconciliation importe les orphelins et ignore corbeille froid plein`() {
        val (repo, base) = repoVierge()
        try {
            repo.saveMessages("g", listOf(message("user", "Référencée"), message("assistant", "ok")))
            repo.saveMessages("orph-1", listOf(message("user", "Orpheline réelle")))
            // Fichiers système qui NE doivent PAS devenir des conversations.
            File(base, ".corbeille-123-abc.json").writeText("[]")
            File(base, "h.plein.json").writeText("[]")
            File(base, "i.froid.json").writeText("[]")
            val index = repo.reconcilierIndex()
            assertEquals(2, index.size)
            assertEquals(1, repo.derniersOrphelins)
            assertTrue(index.none { it.id == ".corbeille-123-abc" })
            assertTrue(index.none { it.id == "h.plein" })
        } finally { base.deleteRecursively() }
    }
}
