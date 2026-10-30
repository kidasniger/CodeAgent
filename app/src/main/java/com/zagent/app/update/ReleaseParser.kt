package com.zagent.app.update

import org.json.JSONObject

object ReleaseParser {
    private val SHA_REGEX = Regex("^[0-9a-f]{64}$")

    private fun JSONObject.str(key: String): String = if (isNull(key)) "" else optString(key, "")

    fun normalizeSha(raw: String): String? {
        val value = raw.trim().lowercase()
        return if (SHA_REGEX.matches(value)) value else null
    }

    fun parse(json: String): ParseResult {
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            return ParseResult.Failure("Réponse GitHub invalide.")
        }
        val tag = root.str("tag_name").trim()
        if (tag.isEmpty()) return ParseResult.Failure("Release sans numéro de version.")
        val assets = root.optJSONArray("assets")
            ?: return ParseResult.Failure("La release $tag ne contient aucun fichier.")

        var apk: ApkAsset? = null
        val shaFiles = HashMap<String, String>()
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            val name = asset.str("name")
            val url = asset.str("browser_download_url")
            if (name.isBlank() || url.isBlank()) continue
            if (name.endsWith(".apk", ignoreCase = true)) {
                if (apk == null) {
                    val digest = asset.str("digest")
                    val sha = if (digest.startsWith("sha256:", ignoreCase = true)) {
                        normalizeSha(digest.substringAfter(':'))
                    } else {
                        null
                    }
                    apk = ApkAsset(name, asset.optLong("size", -1L), url, sha)
                }
            } else if (name.endsWith(".sha256", ignoreCase = true)) {
                shaFiles[name] = url
            }
        }
        val found = apk ?: return ParseResult.Failure("La release $tag n'a pas d'APK compatible.")
        val info = ReleaseInfo(
            tag = tag,
            notes = cleanNotes(root.str("body")),
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