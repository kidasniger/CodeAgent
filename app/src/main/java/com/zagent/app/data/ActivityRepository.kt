package com.zagent.app.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

// ─────────────────────────────────────────────────────────────────────────────
// MA65 / MA64 — Journal d'activité de l'agent : chronologie de ce que l'agent a
// fait (outils, fichiers, commandes), avec snapshot du contenu d'avant écriture
// pour permettre l'annulation d'une écriture récente.
// ─────────────────────────────────────────────────────────────────────────────

/** Statuts affichables d'un événement d'activité. */
object ActivityStatus {
    const val OK = "OK"
    const val ERREUR = "Erreur"
    const val REFUSE = "Refusé"
    const val ANNULE = "Annulé"
}

/**
 * Un événement du journal d'activité (MA65).
 * [undoPath] / [undoContent] : snapshot d'avant une écriture de fichier
 * (undoContent = null signifie que le fichier n'existait pas → l'annulation le supprime).
 */
@Serializable
data class ActivityEvent(
    val id: Long,
    val ts: Long,
    val kind: String,
    val label: String,
    val detail: String = "",
    val status: String = ActivityStatus.OK,
    val durationMs: Long = 0,
    val conversationId: String = "",
    val undoPath: String? = null,
    val undoContent: String? = null
)

class ActivityRepository(context: Context) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val file = File(File(context.filesDir, "activity").apply { mkdirs() }, "events.json")

    fun load(): List<ActivityEvent> = runCatching {
        if (!file.exists()) return emptyList()
        json.decodeFromString(ListSerializer(ActivityEvent.serializer()), file.readText())
    }.getOrNull().orEmpty().sortedByDescending { it.ts }

    fun add(event: ActivityEvent) {
        runCatching {
            val list = (load() + event).sortedByDescending { it.ts }
            // Plafond : 300 événements ; on retire les snapshots d'annulation trop volumineux
            // au-delà des 50 plus récents pour contenir la taille du fichier.
            val trimmed = list.take(300).mapIndexed { i, e ->
                if (i >= 50) e.copy(undoContent = null) else e
            }
            file.writeText(json.encodeToString(ListSerializer(ActivityEvent.serializer()), trimmed))
        }
    }

    /** Marque un événement comme annulé après avoir restauré le fichier (MA65). */
    fun markUndone(id: Long) {
        runCatching {
            val list = load().map { if (it.id == id) it.copy(status = ActivityStatus.ANNULE, undoContent = null) else it }
            file.writeText(json.encodeToString(ListSerializer(ActivityEvent.serializer()), list))
        }
    }

    fun clear() {
        runCatching { file.delete() }
    }
}
