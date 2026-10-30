package com.zagent.app.update

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
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

    fun parse(jsonText: String): ParseResult {
        val root = try {
            json.parseToJsonElement(jsonText).jsonObject
        } catch (_: Exception) {
            return ParseResult.Failure("Réponse GitHub invalide.")
        }

        val tag = root.string("tag_name").trim()
        if (tag.isEmpty()) return ParseResult.Failure("Release sans numéro de version.")

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
                if (apk == null) {
                    val digest = asset.string("digest")
                    val sha = if (digest.startsWith("sha256:", ignoreCase = true)) {
                        normalizeSha(digest.substringAfter(':'))
                    } else {
                        null
                    }
                    val size = (asset["size"] as? JsonPrimitive)?.longOrNull ?: -1L
                    apk = ApkAsset(name, size, url, sha)
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

    fun cleanNotes(raw: String): String {
        val lines = raw.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { it.replace("**", "").replace("`", "").replace(Regex("^[-*•]\\s*"), "").trim() }
            .filter { it.isNotEmpty() }
        return if (lines.isEmpty()) "Corrections et améliorations." else lines.joinToString("\n") { "• $it" }
    }
}
