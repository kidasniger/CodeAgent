package com.zagent.app.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// MA60 — Journal d'usage : une entrée par échange réussi avec l'IA, persistée en
// JSON (filesDir/usage/entries.json) et exportable en CSV.
// ─────────────────────────────────────────────────────────────────────────────

/** Une entrée d'usage tokens/coût (MA60). */
@Serializable
data class UsageEntry(
    val ts: Long,
    val model: String,
    val project: String,
    val tokensIn: Int,
    val tokensOut: Int,
    val cost: Double
) {
    val totalTokens: Int get() = tokensIn + tokensOut
}

/**
 * corr-082 — Calcul du coût d'un échange, isolé pour être testé en JVM.
 * Prix en USD par million de tokens ; renvoie null si aucun prix n'est
 * renseigné. Un flux interrompu (usage partiel du fournisseur) est géré
 * naturellement : le token manquant compte pour 0.
 */
fun coutEstime(tokensIn: Int, tokensOut: Int, prixInPerM: Float, prixOutPerM: Float): Double? {
    // Prix negatif = donnee corrompue ou non renseignee : pas d'estimation fiable.
    if (prixInPerM < 0f || prixOutPerM < 0f) return null
    // Prix nul des deux cotes = gratuit : rien a estimer.
    if (prixInPerM <= 0f && prixOutPerM <= 0f) return null
    return tokensIn.toDouble() * prixInPerM / 1_000_000.0 +
        tokensOut.toDouble() * prixOutPerM / 1_000_000.0
}

/**
 * corr-018 — Journal des consommations : tokens et coûts estimés par session.
 *
 * Entrées plafonnées à Constants.USAGE_ENTREES_MAX, sérialisées en JSON.
 * Alimente l'écran Usage et la ligne d'usage sous les messages (MA45).
 */
class UsageRepository(context: Context) {
    private val json = Json { coerceInputValues = true; ignoreUnknownKeys = true; encodeDefaults = true }
    private val file = File(File(context.filesDir, "usage").apply { mkdirs() }, "entries.json")

    fun load(): List<UsageEntry> = runCatching {
        if (!file.exists()) return emptyList()
        json.decodeFromString(ListSerializer(UsageEntry.serializer()), file.readText())
    }.getOrNull().orEmpty()

    fun add(entry: UsageEntry) {
        runCatching {
            val list = load() + entry
            // Plafond : 2 000 entrées (les plus anciennes sont retirées).
            val trimmed = if (list.size > Constants.USAGE_ENTREES_MAX) list.takeLast(Constants.USAGE_ENTREES_MAX) else list
            file.writeText(json.encodeToString(ListSerializer(UsageEntry.serializer()), trimmed))
        }
    }

    fun clear() {
        runCatching { file.delete() }
    }

    /** Export CSV : date, modèle, projet, tokens entrée/sortie/total, coût (MA60). */
    fun csv(entries: List<UsageEntry> = load()): String {
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val sb = StringBuilder("date;modele;projet;tokens_entree;tokens_sortie;tokens_total;cout_usd\n")
        for (e in entries) {
            sb.append(df.format(Date(e.ts))).append(';')
                .append(e.model.replace(';', ' ')).append(';')
                .append(e.project.replace(';', ' ')).append(';')
                .append(e.tokensIn).append(';')
                .append(e.tokensOut).append(';')
                .append(e.totalTokens).append(';')
                .append(String.format(Locale.US, "%.6f", e.cost)).append('\n')
        }
        return sb.toString()
    }
}
