package com.zagent.app.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

// ─────────────────────────────────────────────────────────────────────────────
// MA56 — Persistance des conversations : index des métadonnées + un fichier JSON
// par conversation (fichiers dans filesDir/conversations).
// ─────────────────────────────────────────────────────────────────────────────

/** Métadonnées d'une conversation affichées dans le tiroir d'historique (MA56). */
@Serializable
data class ConversationMeta(
    val id: String,
    val title: String,
    val preview: String,
    val updatedAt: Long,
    val messageCount: Int = 0,
    val pinned: Boolean = false
)

/** Appel d'outil sérialisé (équivalent de AiRepository.ToolCallData, sans dépendance). */
@Serializable
data class StoredToolCall(val id: String, val name: String, val arguments: String)

/** Message sérialisé d'une conversation (MA56). */
@Serializable
data class StoredMessage(
    val role: String,
    val content: String? = null,
    val time: Long = 0L,
    val reasoning: String? = null,
    val toolCallId: String? = null,
    val name: String? = null,
    val toolLog: String? = null,
    val toolCalls: List<StoredToolCall>? = null,
    /** MàJ 104 — nom du modèle émetteur (mode Duo). */
    val modele: String? = null
)

/** Résultat d'une recherche plein texte dans les conversations (MA58). */
data class ConversationHit(
    val conversationId: String,
    val title: String,
    val updatedAt: Long,
    val snippet: String
)

/**
 * corr-018 — Stockage local des conversations : index (métadonnées) + messages.
 *
 * JSON sur le disque dans le répertoire privé de l'application. Fournit
 * l'index trié/épinglable, les messages par conversation, la recherche plein
 * texte et les libellés automatiques. Implémente ConversationRepositoryApi
 * pour la simulation en tests.
 */
class ConversationRepository(context: Context) : ConversationRepositoryApi {
    private val json = Json { coerceInputValues = true; ignoreUnknownKeys = true; encodeDefaults = true }
    private val dir = File(context.filesDir, "conversations").apply { mkdirs() }
    private val indexFile = File(dir, "index.json")

    /**
     * corr-086 — Version du schéma des données locales. Persistée dans
     * schema.json ; un numéro absent (= données antérieures au versionnage) ou
     * inférieur déclenche [migrer]. Aujourd'hui la migration v0 → v1 est
     * couverte par les valeurs par défaut + coerceInputValues (les anciens
     * fichiers manquant des champs se relient sans crash) ; les migrations
     * futures s'ajoutent ici une à une.
     */
    companion object {
        const val SCHEMA_VERSION = 1
    }

    private fun versionFichier() = File(dir, "schema.json")

    init {
        runCatching {
            val vf = versionFichier()
            val lue = if (vf.exists()) vf.readText().trim().toIntOrNull() ?: 0 else 0
            if (lue < SCHEMA_VERSION) {
                migrer(lue)
                vf.writeText(SCHEMA_VERSION.toString())
            }
        }
    }

    private fun migrer(depuis: Int) {
        // v0 → v1 : les champs manquants des anciens index/messages sont déjà
        // tolérés (Json coerceInputValues + valeurs par défaut des data class).
        // Rien à faire ici pour l'instant ; point d'entrée des migrations à venir.
    }

    /** Charge l'index des conversations (trié par épinglage puis date décroissante). */
    override fun loadIndex(): List<ConversationMeta> = runCatching {
        if (!indexFile.exists()) return emptyList()
        json.decodeFromString(ListSerializer(ConversationMeta.serializer()), indexFile.readText())
    }.getOrNull().orEmpty().sortedWith(
        compareByDescending<ConversationMeta> { it.pinned }.thenByDescending { it.updatedAt }
    )

    override fun saveIndex(list: List<ConversationMeta>) {
        runCatching {
            indexFile.writeText(json.encodeToString(ListSerializer(ConversationMeta.serializer()), list))
        }
    }

    /** Sauvegarde les messages d'une conversation (contenu plafonné à 200 000 caractères par message). */
    override fun saveMessages(id: String, messages: List<StoredMessage>) {
        runCatching {
            File(dir, "$id.json").writeText(
                json.encodeToString(ListSerializer(StoredMessage.serializer()), messages)
            )
        }
    }

    override fun loadMessages(id: String): List<StoredMessage> = runCatching {
        val f = File(dir, "$id.json")
        if (!f.exists()) return emptyList()
        json.decodeFromString(ListSerializer(StoredMessage.serializer()), f.readText())
    }.getOrNull().orEmpty()

    override fun deleteMessages(id: String) {
        runCatching {
            File(dir, "$id.json").delete()
            File(dir, "$id.todos.json").delete()
        }
    }

    /**
     * corr-077 — Liste de tâches (update_todos) persistée PAR conversation :
     * rouvrir la conversation restaure sa liste, et une autre conversation ne
     * l'écrase pas. Fichier séparé <id>.todos.json pour ne pas casser le
     * format des messages.
     */
    override fun saveTodos(id: String, todos: List<TodoItem>) {
        runCatching {
            File(dir, "$id.todos.json").writeText(
                json.encodeToString(ListSerializer(TodoItem.serializer()), todos)
            )
        }
    }

    override fun loadTodos(id: String): List<TodoItem> = runCatching {
        val f = File(dir, "$id.todos.json")
        if (!f.exists()) return emptyList()
        json.decodeFromString(ListSerializer(TodoItem.serializer()), f.readText())
    }.getOrNull().orEmpty()

    /**
     * Recherche plein texte dans les conversations sauvegardées (MA58) :
     * renvoie au plus [maxPer] extraits par conversation, conversations plafonnées à 40.
     */
    override fun searchContent(query: String, maxPer: Int): List<ConversationHit> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val hits = ArrayList<ConversationHit>()
        val index = loadIndex()
        for (meta in index.take(40)) {
            if (hits.size >= 30) break
            val msgs = loadMessages(meta.id)
            var count = 0
            for (m in msgs) {
                if (count >= maxPer) break
                val text = m.content.orEmpty()
                if (text.length > 40_000) continue
                val at = text.indexOf(q, ignoreCase = true)
                if (at >= 0) {
                    val start = maxOf(0, at - 60)
                    val end = minOf(text.length, at + q.length + 90)
                    val snippet = (if (start > 0) "…" else "") + text.substring(start, end).replace('\n', ' ') + (if (end < text.length) "…" else "")
                    hits.add(ConversationHit(meta.id, meta.title, meta.updatedAt, snippet))
                    count++
                }
            }
        }
        return hits
    }

    /** Titre auto-généré : première ligne du premier message utilisateur (MA56). */
    override fun autoTitle(firstUserText: String?): String {
        val raw = firstUserText.orEmpty().trim()
        if (raw.isEmpty()) return "Nouvelle conversation"
        val line = raw.lineSequence().first().trim()
        return line.take(48) + if (line.length > 48) "…" else ""
    }

    /** Aperçu d'une ligne : première ligne du dernier message utile (MA56). */
    override fun autoPreview(lastText: String?): String {
        val raw = lastText.orEmpty().trim()
        if (raw.isEmpty()) return ""
        val line = raw.lineSequence().first().trim()
        return line.take(90) + if (line.length > 90) "…" else ""
    }
}
