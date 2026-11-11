package com.zagent.app.update

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

object ReleaseParser {
    private val SHA_REGEX = Regex("^[0-9a-f]{64}$")
    private val json = Json { ignoreUnknownKeys = true }

    private fun JsonObject.string(key: String): String =
        when (val value = this[key]) {
            is JsonPrimitive -> if (value.isString) value.content else ""
            else -> ""
        }

    fun normalizeSha(raw: String): String? {
        val value = raw.trim().lowercase()
        return if (SHA_REGEX.matches(value)) value else null
    }

    /**
     * MA65 : analyse une réponse GitHub (une release). [allowBeta] accepte les
     * prereleases (canal bêta, MA67) ; les drafts sont toujours refusées.
     */
    fun parse(jsonText: String, allowBeta: Boolean = false): ParseResult {
        val root = try {
            json.parseToJsonElement(jsonText).jsonObject
        } catch (_: Exception) {
            return ParseResult.Failure("Réponse GitHub invalide.")
        }

        val tag = root.string("tag_name").trim()
        if (tag.isEmpty()) return ParseResult.Failure("Release sans numéro de version.")

        // MA65 : une draft n'est jamais proposée ; une prerelease seulement en canal bêta.
        if ((root["draft"] as? JsonPrimitive)?.booleanOrNull == true) {
            return ParseResult.Failure("Release en préparation, pas encore publiée.")
        }
        val isPrerelease = (root["prerelease"] as? JsonPrimitive)?.booleanOrNull == true
        if (isPrerelease && !allowBeta) {
            return ParseResult.Failure("Aucune version stable plus récente.")
        }

        val assets = root["assets"] as? JsonArray
            ?: return ParseResult.Failure("La release $tag ne contient aucun fichier.")

        var apk: ApkAsset? = null
        val shaFiles = HashMap<String, String>()

        for (element in assets) {
            val asset = element as? JsonObject ?: continue
            val name = asset.string("name")
            val url = asset.string("browser_download_url")
            if (name.isBlank() || url.isBlank()) continue

            if (name.endsWith(".apk", ignoreCase = true)) {
                val digest = asset.string("digest")
                val sha = if (digest.startsWith("sha256:", ignoreCase = true)) {
                    normalizeSha(digest.substringAfter(':'))
                } else {
                    null
                }
                val size = (asset["size"] as? JsonPrimitive)?.longOrNull ?: -1L
                val candidate = ApkAsset(name, size, url, sha)
                // MA65 : plusieurs assets possibles — priorité à « universal »,
                // puis « arm64-v8a », puis le premier APK trouvé.
                apk = when {
                    apk == null -> candidate
                    name.contains("universal", ignoreCase = true) &&
                        !apk.name.contains("universal", ignoreCase = true) -> candidate
                    apk.name.contains("arm64-v8a", ignoreCase = true) &&
                        name.contains("universal", ignoreCase = true) -> candidate
                    !apk.name.contains("universal", true) &&
                        name.contains("arm64-v8a", true) &&
                        !apk.name.contains("arm64-v8a", true) -> candidate
                    else -> apk
                }
            } else if (name.endsWith(".sha256", ignoreCase = true)) {
                shaFiles[name] = url
            }
        }

        val found = apk ?: return ParseResult.Failure("La release $tag n'a pas d'APK compatible.")
        val info = ReleaseInfo(
            tag = tag,
            notes = cleanNotes(root.string("body")),
            apk = found,
            sha256Url = shaFiles[found.name + ".sha256"]
        )
        return ParseResult.Success(info)
    }

    /**
     * MA67 : extrait les objets release d'une réponse « /releases » (liste JSON).
     * Retourne null si la réponse n'est pas une liste.
     */
    fun parseArray(jsonText: String): List<String>? {
        val root = try {
            json.parseToJsonElement(jsonText)
        } catch (_: Exception) {
            return null
        }
        val arr = root as? JsonArray ?: return null
        return arr.map { it.toString() }
    }

    fun cleanNotes(raw: String): String {
        val lines = raw.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { it.replace("**", "").replace("`", "").replace(Regex("^[-*•]\\s*"), "").trim() }
            .filter { it.isNotEmpty() }
        return if (lines.isEmpty()) "• Corrections et améliorations." else lines.joinToString("\n") { "• $it" }
    }

    /**
     * maj-073 — Notes brutes pour l'écran Nouveautés : garde le markdown simple
     * (titres, puces) au lieu de l'aplatir comme [cleanNotes], car il est rendu
     * par MarkdownText.
     */
    fun notesBrutes(raw: String): String =
        raw.trim().ifEmpty { "Corrections et améliorations." }

    /**
     * maj-073 — Analyse une réponse « /releases » (liste) en résumés légers pour
     * la section Nouveautés. Les drafts sont ignorées ; date limitée à la partie
     * jour (YYYY-MM-DD). Renvoie au plus [limit] entrées.
     */
    fun parseSummaries(jsonText: String, limit: Int = 5): List<ReleaseSummary> {
        val arr = try {
            json.parseToJsonElement(jsonText) as? JsonArray
        } catch (_: Exception) {
            return emptyList()
        } ?: return emptyList()
        return arr.take(limit).mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val tag = o.string("tag_name").trim()
            if (tag.isEmpty() || o.string("draft") == "true") return@mapNotNull null
            ReleaseSummary(
                tag = tag,
                name = o.string("name").ifBlank { tag },
                date = o.string("published_at").take(10),
                notes = notesBrutes(o.string("body")),
                prerelease = o.string("prerelease") == "true"
            )
        }
    }
}
