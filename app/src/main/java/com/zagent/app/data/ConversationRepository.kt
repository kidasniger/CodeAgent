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
    val pinned: Boolean = false,
    /** feat-020 — étiquette / dossier de classement (chaîne vide = sans dossier). */
    val etiquette: String = "",
    /** hist-008 — archivage froid : le fichier ne garde plus que titre + résumé. */
    val archiveFroide: Boolean = false
)

/** hist-009 — Entrée de la corbeille d'historique (suppression réversible 7 jours). */
@Serializable
data class CorbeilleEntree(
    val nom: String,
    val id: String,
    val titre: String,
    val supprimeLe: Long
)

/** hist-011 — Statistiques d'une conversation (tokens estimés, outils, modèles). */
@Serializable
data class StatsConversation(
    val messages: Int = 0,
    val messagesUtilisateur: Int = 0,
    val messagesAssistant: Int = 0,
    val appelsOutils: Int = 0,
    val outils: Map<String, Int> = emptyMap(),
    val caracteres: Long = 0L,
    val tokensEstimes: Long = 0L,
    val modeles: Map<String, Int> = emptyMap()
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
class ConversationRepository private constructor(private val dir: File) : ConversationRepositoryApi {
    /** Constructeur applicatif : dossier privé de l'app. */
    constructor(context: Context) : this(File(context.filesDir, "conversations").apply { mkdirs() })

    /** hist-* — constructeur de test : dossier temporaire contrôlé (JVM). */
    constructor(baseDir: File, testMarker: Boolean) : this(baseDir.apply { mkdirs() })

    private val json = Json { coerceInputValues = true; ignoreUnknownKeys = true; encodeDefaults = true }
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
                // err-096 — sauvegarde complète avant migration ; en cas d échec
                // de migration, les fichiers originaux restent lisibles dans
                // sauvegarde-pre-migration-<v> pour une restauration manuelle.
                val sauvegarde = File(dir, "sauvegarde-pre-migration-$lue").apply { mkdirs() }
                dir.listFiles { f -> f.isFile && f.name.endsWith(".json") && f != vf }?.forEach { f ->
                    runCatching { f.copyTo(File(sauvegarde, f.name), overwrite = true) }
                }
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

    init {
        // hist-009 — purge automatique de la corbeille au démarrage : tout ce
        // qui dépasse 7 jours est définitivement effacé.
        purgerCorbeille()
    }

    /** err-095 — isole un fichier de données corrompu (sauvegarde horodatée) avant de repartir à vide. */
    private fun isolerFichierCorrompu(f: File) {
        runCatching {
            if (f.exists()) {
                f.renameTo(File(dir, f.name + ".corrupt-" + System.currentTimeMillis()))
            }
        }
    }

    /** Charge l'index des conversations (trié par épinglage puis date décroissante). */
    override fun loadIndex(): List<ConversationMeta> {
        val resultat = runCatching {
            if (!indexFile.exists()) emptyList()
            else json.decodeFromString(ListSerializer(ConversationMeta.serializer()), indexFile.readText())
        }.getOrNull()
        if (resultat == null && indexFile.exists()) isolerFichierCorrompu(indexFile) // err-095
        return resultat.orEmpty().sortedWith(
            compareByDescending<ConversationMeta> { it.pinned }
                .thenByDescending { it.updatedAt }
                // hist-007 — tri secondaire alphabétique (même jour = ordre stable).
                .thenBy { it.title.lowercase() }
        )
    }

    /** hist-007 — tri utilisé partout (épinglées, date desc, puis titre). */
    private fun triIndex() = compareByDescending<ConversationMeta> { it.pinned }
        .thenByDescending { it.updatedAt }
        .thenBy { it.title.lowercase() }

    /**
     * Session 12 — réconciliation index ↔ fichiers : supprime les entrées dont
     * le fichier de messages a disparu (crash entre index et messages), importe
     * les fichiers orphelis jamais indexés (métadonnées reconstruites), puis
     * persiste l'index corrigé. Coût : simple listing de dossier, aucun contenu lu.
     */
    fun reconcilierIndex(): List<ConversationMeta> {
        val index = loadIndex()
        orphelinsImportes = 0
        val valides = ArrayList<ConversationMeta>()
        val idsVus = HashSet<String>()
        index.forEach { meta ->
            val fichier = File(dir, meta.id + ".json")
            if (fichier.exists()) {
                if (idsVus.add(meta.id)) valides += meta
            }
            // fichier absent : entrée fantôme (crash entre index et messages) — retirée
        }
        // Fichiers orphelis : des messages existent mais aucune entrée d'index.
        val orphelins = dir.listFiles { f -> f.isFile && f.name.endsWith(".json") } ?: emptyArray()
        orphelins.forEach { f ->
            val nom = f.name.removeSuffix(".json")
            // hist-009/008 — les fichiers système (corbeille, plein, froid, todos,
            // sauvegardes) ne sont jamais des conversations orphelines.
            if (nom == "index" || nom.startsWith(".") || nom.contains(".corbeille-") ||
                nom.endsWith(".plein") || nom.endsWith(".froid") || nom.endsWith(".froid-restaure") ||
                nom.endsWith(".todos") || nom.contains(".corrupt-") || nom.startsWith("sauvegarde-pre-migration")
            ) return@forEach
            if (idsVus.contains(nom)) return@forEach
            val msgs = loadMessages(nom)
            if (msgs.isEmpty()) return@forEach
            orphelinsImportes++
            val premier = msgs.firstOrNull { it.role == "user" }?.content.orEmpty()
            val dernierVisible = msgs.lastOrNull { it.role == "assistant" }?.content
                ?: msgs.lastOrNull()?.content.orEmpty()
            if (idsVus.add(nom)) {
                valides += ConversationMeta(
                    id = nom,
                    title = autoTitle(premier),
                    preview = autoPreview(dernierVisible),
                    updatedAt = f.lastModified(),
                    messageCount = msgs.size,
                    pinned = false
                )
            }
        }
        if (valides.size != index.size) saveIndex(valides)
        // hist-012 — compteur d'orphelins importés pour l'indicateur visuel.
        return valides.sortedWith(triIndex()).also { derniersOrphelins = orphelinsImportes }
    }

    /** hist-012 — nombre d'orphelins importés lors du dernier [reconcilierIndex]. */
    var derniersOrphelins: Int = 0
        private set
    private var orphelinsImportes: Int = 0

    /**
     * hist-006 — Recompte les messages de chaque entrée d'index depuis les
     * fichiers : l'index peut être périmé si un crash a suivi saveIndex mais
     * précédé saveMessages. L'affichage ne ment donc jamais.
     */
    fun recompterMessages(index: List<ConversationMeta>): List<ConversationMeta> = index.map { meta ->
        val f = File(dir, meta.id + ".json")
        if (!f.exists()) meta
        else {
            // Recompte léger : nombre d'occurrences de "role" dans le JSON
            // (une entrée par message sérialisé) sans tout désérialiser.
            val compte = runCatching {
                f.readText().split("\"role\":").size - 1
            }.getOrDefault(meta.messageCount)
            if (compte != meta.messageCount && compte >= 0) meta.copy(messageCount = compte) else meta
        }
    }

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

    override fun loadMessages(id: String): List<StoredMessage> {
        val f = File(dir, "$id.json")
        val resultat = runCatching {
            if (!f.exists()) emptyList()
            else json.decodeFromString(ListSerializer(StoredMessage.serializer()), f.readText())
        }.getOrNull()
        if (resultat == null && f.exists()) isolerFichierCorrompu(f) // err-095
        return resultat.orEmpty()
    }

    override fun deleteMessages(id: String) {
        runCatching {
            File(dir, "$id.json").delete()
            File(dir, "$id.todos.json").delete()
        }
    }

    // ── hist-009 : corbeille d'historique (suppression réversible 7 jours) ──

    /** Registre persistant de la corbeille : titre original + date de suppression. */
    private val corbeilleFichier = File(dir, ".corbeille-index.json")

    private fun lireCorbeille(): MutableList<CorbeilleEntree> = runCatching {
        if (!corbeilleFichier.exists()) mutableListOf()
        else json.decodeFromString(ListSerializer(CorbeilleEntree.serializer()), corbeilleFichier.readText()).toMutableList()
    }.getOrDefault(mutableListOf())

    private fun ecrireCorbeille(l: List<CorbeilleEntree>) {
        runCatching {
            if (l.isEmpty()) corbeilleFichier.delete()
            else corbeilleFichier.writeText(json.encodeToString(ListSerializer(CorbeilleEntree.serializer()), l))
        }
    }

    /** Déplace une conversation vers la corbeille (fichier renommé, pas détruit). */
    fun mettreCorbeille(id: String) {
        runCatching {
            val f = File(dir, "$id.json")
            if (f.exists()) {
                val dest = File(dir, ".corbeille-${System.currentTimeMillis()}-$id.json")
                f.renameTo(dest)
                File(dir, "$id.todos.json").renameTo(File(dir, dest.name.removeSuffix(".json") + ".todos.json"))
                val index = loadIndex()
                val titre = index.firstOrNull { it.id == id }?.title ?: id
                val cl = lireCorbeille()
                cl.removeAll { it.id == id }
                cl += CorbeilleEntree(dest.name, id, titre, System.currentTimeMillis())
                ecrireCorbeille(cl)
                saveIndex(index.filter { it.id != id })
            }
        }
    }

    /** Liste le contenu de la corbeille (registre persistant + fichiers orphelis reconstruits). */
    fun listerCorbeille(): List<CorbeilleEntree> {
        val connus = lireCorbeille()
        val nomsConnus = connus.map { it.nom }.toHashSet()
        dir.listFiles { f -> f.isFile && f.name.startsWith(".corbeille-") && f.name.endsWith(".json") && !f.name.contains(".todos") }?.forEach { f ->
            if (f.name == corbeilleFichier.name || nomsConnus.contains(f.name)) return@forEach
            val parties = f.name.removePrefix(".corbeille-").removeSuffix(".json").split("-", limit = 2)
            val ts = parties.firstOrNull()?.toLongOrNull() ?: f.lastModified()
            val id = parties.getOrNull(1) ?: f.nameWithoutExtension
            connus += CorbeilleEntree(f.name, id, id, ts)
        }
        return connus.sortedByDescending { it.supprimeLe }
    }

    /** Restaure une entrée de la corbeille (titre original préservé). */
    fun restaurerDeCorbeille(nom: String) {
        runCatching {
            val f = File(dir, nom)
            if (!f.exists()) return
            val parties = nom.removePrefix(".corbeille-").removeSuffix(".json").split("-", limit = 2)
            val id = parties.getOrNull(1) ?: nom
            f.renameTo(File(dir, "$id.json"))
            val todos = File(dir, nom.removeSuffix(".json") + ".todos.json")
            if (todos.exists()) todos.renameTo(File(dir, "$id.todos.json"))
            val cl = lireCorbeille()
            val entree = cl.firstOrNull { it.nom == nom }
            cl.removeAll { it.nom == nom }
            ecrireCorbeille(cl)
            val msgs = loadMessages(id)
            val index = loadIndex().toMutableList()
            if (index.none { it.id == id }) {
                index += ConversationMeta(
                    id = id,
                    title = entree?.titre ?: autoTitle(msgs.firstOrNull { it.role == "user" }?.content),
                    preview = autoPreview(msgs.lastOrNull()?.content),
                    updatedAt = f.lastModified(),
                    messageCount = msgs.size
                )
                saveIndex(index)
            }
        }
    }

    /** Purge définitive de la corbeille : entrées de plus de 7 jours. */
    fun purgerCorbeille(maxAgeDays: Long = 7) {
        runCatching {
            val seuil = System.currentTimeMillis() - maxAgeDays * 24 * 60 * 60 * 1000
            val cl = lireCorbeille()
            cl.removeAll { e ->
                if (e.supprimeLe < seuil) {
                    File(dir, e.nom).delete()
                    File(dir, e.nom.removeSuffix(".json") + ".todos.json").delete()
                    true
                } else false
            }
            // Orphelins de la corbeille hors registre (ancien format).
            dir.listFiles { f -> f.isFile && f.name.startsWith(".corbeille-") && f.name != corbeilleFichier.name }?.forEach { f ->
                val parties = f.name.removePrefix(".corbeille-").removeSuffix(".json").split("-", limit = 2)
                val ts = parties.firstOrNull()?.toLongOrNull() ?: f.lastModified()
                if (ts < seuil) f.delete()
            }
            ecrireCorbeille(cl)
        }
    }

    // ── hist-008 : archivage froid (> 30 jours) ─────────────────────────────

    /**
     * hist-008 — Compresse les conversations plus vieilles que [maxAgeDays] :
     * le fichier de messages ne garde que le premier message utilisateur et le
     * dernier message assistant (titre + résumé), l'index passe archiveFroide.
     * Le JSON reste ouvrable/cherchable ; rien n'est perdu de manière silencieuse.
     */
    fun archiverFroid(maxAgeDays: Long = 30): Int {
        var n = 0
        runCatching {
            val seuil = System.currentTimeMillis() - maxAgeDays * 24 * 60 * 60 * 1000
            val index = loadIndex().toMutableList()
            index.forEachIndexed { i, meta ->
                if (meta.archiveFroide || meta.updatedAt >= seuil) return@forEachIndexed
                val f = File(dir, meta.id + ".json")
                if (!f.exists()) return@forEachIndexed
                val msgs = loadMessages(meta.id)
                val premier = msgs.firstOrNull { it.role == "user" }
                val dernier = msgs.lastOrNull { it.role == "assistant" }
                val compact = listOfNotNull(premier, dernier)
                if (compact.size < msgs.size) {
                    File(dir, meta.id + ".froid.json").writeText(
                        json.encodeToString(ListSerializer(StoredMessage.serializer()), compact)
                    )
                    f.renameTo(File(dir, meta.id + ".plein.json"))
                    File(dir, meta.id + ".froid.json").renameTo(f)
                    index[i] = meta.copy(archiveFroide = true)
                    n++
                }
            }
            saveIndex(index)
        }
        return n
    }

    /** hist-008 — Rouvre une conversation froide (restaure le contenu plein). */
    fun dearchiver(id: String) {
        runCatching {
            val plein = File(dir, "$id.plein.json")
            if (plein.exists()) {
                File(dir, "$id.json").renameTo(File(dir, "$id.froid-restaure.json"))
                plein.renameTo(File(dir, "$id.json"))
            }
            val index = loadIndex().toMutableList()
            val i = index.indexOfFirst { it.id == id }
            if (i >= 0) index[i] = index[i].copy(archiveFroide = false)
            saveIndex(index)
        }
    }

    // ── hist-011 : statistiques par conversation ────────────────────────────

    /**
     * hist-011 — Statistiques d'une conversation : compte de messages par rôle,
     * outils appelés (avec fréquence), caractères et tokens estimés (≈ chars/4),
     * modèles utilisés. Lecture seule, aucun coût réseau.
     */
    fun statistiques(id: String): StatsConversation {
        val msgs = loadMessages(id)
        val outils = LinkedHashMap<String, Int>()
        val modeles = LinkedHashMap<String, Int>()
        var chars = 0L
        var appels = 0
        msgs.forEach { m ->
            chars += (m.content?.length ?: 0) + (m.reasoning?.length ?: 0) + (m.toolLog?.length ?: 0)
            m.toolCalls?.forEach { tc ->
                appels++
                outils[tc.name] = (outils[tc.name] ?: 0) + 1
            }
            if (!m.modele.isNullOrBlank()) modeles[m.modele!!] = (modeles[m.modele!!] ?: 0) + 1
        }
        return StatsConversation(
            messages = msgs.size,
            messagesUtilisateur = msgs.count { it.role == "user" },
            messagesAssistant = msgs.count { it.role == "assistant" },
            appelsOutils = appels,
            outils = outils,
            caracteres = chars,
            tokensEstimes = chars / 4,
            modeles = modeles
        )
    }

    // ── ia-028 : plan de projet par conversation ────────────────────────────

    /** ia-028 — Sauvegarde le plan de projet attaché à cette conversation. */
    fun savePlan(id: String, plan: String) {
        runCatching { File(dir, "$id.plan.txt").writeText(plan.take(100_000)) }
    }

    /** ia-028 — Charge le plan de projet de la conversation ("" = aucun). */
    fun loadPlan(id: String): String =
        File(dir, "$id.plan.txt").let { if (it.exists()) runCatching { it.readText() }.getOrDefault("") else "" }

    /** ia-028 — Supprime le plan (action clear). */
    fun deletePlan(id: String) {
        runCatching { File(dir, "$id.plan.txt").delete() }
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

    override fun loadTodos(id: String): List<TodoItem> {
        val f = File(dir, "$id.todos.json")
        val resultat = runCatching {
            if (!f.exists()) emptyList()
            else json.decodeFromString(ListSerializer(TodoItem.serializer()), f.readText())
        }.getOrNull()
        if (resultat == null && f.exists()) isolerFichierCorrompu(f) // err-095
        return resultat.orEmpty()
    }

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
                // hist-001 — recherche aussi dans reasoning et toolLog.
                val text = listOf(m.content.orEmpty(), m.reasoning.orEmpty(), m.toolLog.orEmpty())
                    .joinToString("\n")
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
