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
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val dir = File(context.filesDir, "conversations").apply { mkdirs() }
    private val indexFile = File(dir, "index.json")

    /** Charge l'index des conversations (trié par épinglage puis date décroissante). */
    fun loadIndex(): List<ConversationMeta> = runCatching {
        if (!indexFile.exists()) return emptyList()
        json.decodeFromString(ListSerializer(ConversationMeta.serializer()), indexFile.readText())
    }.getOrNull().orEmpty().sortedWith(
        compareByDescending<ConversationMeta> { it.pinned }.thenByDescending { it.updatedAt }
    )

    fun saveIndex(list: List<ConversationMeta>) {
        runCatching {
            indexFile.writeText(json.encodeToString(ListSerializer(ConversationMeta.serializer()), list))
        }
    }

    /** Sauvegarde les messages d'une conversation (contenu plafonné à 200 000 caractères par message). */
    fun saveMessages(id: String, messages: List<StoredMessage>) {
        runCatching {
            File(dir, "$id.json").writeText(
                json.encodeToString(ListSerializer(StoredMessage.serializer()), messages)
            )
        }
    }

    fun loadMessages(id: String): List<StoredMessage> = runCatching {
        val f = File(dir, "$id.json")
        if (!f.exists()) return emptyList()
        json.decodeFromString(ListSerializer(StoredMessage.serializer()), f.readText())
    }.getOrNull().orEmpty()

    fun deleteMessages(id: String) {
        runCatching { File(dir, "$id.json").delete() }
    }

    /**
     * Recherche plein texte dans les conversations sauvegardées (MA58) :
     * renvoie au plus [maxPer] extraits par conversation, conversations plafonnées à 40.
     */
    fun searchContent(query: String, maxPer: Int = 3): List<ConversationHit> {
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
    fun autoTitle(firstUserText: String?): String {
        val raw = firstUserText.orEmpty().trim()
        if (raw.isEmpty()) return "Nouvelle conversation"
        val line = raw.lineSequence().first().trim()
        return line.take(48) + if (line.length > 48) "…" else ""
    }

    /** Aperçu d'une ligne : première ligne du dernier message utile (MA56). */
    fun autoPreview(lastText: String?): String {
        val raw = lastText.orEmpty().trim()
        if (raw.isEmpty()) return ""
        val line = raw.lineSequence().first().trim()
        return line.take(90) + if (line.length > 90) "…" else ""
    }
}
