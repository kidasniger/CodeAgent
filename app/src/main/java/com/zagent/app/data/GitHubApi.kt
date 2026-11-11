package com.zagent.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * feat-088 / feat-089 — Client GitHub REST minimal (token personnel chiffré,
 * stocké via SettingsManager). Deux usages :
 *  - créer une pull request depuis la branche courante (feat-088) ;
 *  - lire le statut des GitHub Actions du dépôt (feat-089).
 *
 * L'appelant est responsable du thread : toutes les méthodes sont bloquantes
 * (elles basculent sur Dispatchers.IO elles-mêmes) et renvoient un [Result].
 */
object GitHubApi {

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun JsonObject.string(key: String): String =
        when (val v = this[key]) {
            is JsonPrimitive -> if (v.isString) v.content else v.content
            else -> ""
        }

    /** Extrait owner/repo d'une URL d'origine git (https ou ssh). */
    fun parseOwnerRepo(remoteUrl: String): Pair<String, String>? {
        val s = remoteUrl.trim().removeSuffix("/")
        val m = Regex("github\\.com[/:]([^/:]+)/([^/]+?)(\\.git)?/?$").find(s) ?: return null
        val (owner, repo) = m.destructured
        if (owner.isBlank() || repo.isBlank()) return null
        return owner to repo
    }

    /** Endpoint API associé à un remote github.com (protection URL inconnue). */
    fun apiBase(remoteUrl: String): String? {
        val (owner, repo) = parseOwnerRepo(remoteUrl) ?: return null
        return "https://api.github.com/repos/$owner/$repo"
    }

    /** feat-089 — Liste les derniers runs d'Actions du dépôt (5 par défaut). */
    suspend fun actionsRuns(remoteUrl: String, token: String, limit: Int = 5): Result<List<ActionsRun>> =
        withContext(Dispatchers.IO) {
            val base = apiBase(remoteUrl)
                ?: return@withContext Result.failure(IllegalArgumentException("L'origine n'est pas un dépôt GitHub."))
            try {
                val req = Request.Builder()
                    .url("$base/actions/runs?per_page=$limit")
                    .addHeader("Accept", "application/vnd.github+json")
                    .addHeader("User-Agent", "ZAgent-Android")
                    .addAuth(token)
                    .get()
                    .build()
                client.newCall(req).execute().use { resp ->
                    val body = resp.body.string()
                    if (!resp.isSuccessful) return@use Result.failure(Exception("GitHub HTTP ${resp.code}"))
                    val root = json.parseToJsonElement(body).jsonObject
                    val runs = (root["workflow_runs"] as? JsonArray)?.mapNotNull { el ->
                        val o = el as? JsonObject ?: return@mapNotNull null
                        ActionsRun(
                            id = o.string("id"),
                            name = o.string("name").ifBlank { "workflow" },
                            branch = o.string("head_branch"),
                            status = o.string("status"),
                            conclusion = o.string("conclusion"),
                            runNumber = (o["run_number"] as? JsonPrimitive)?.intOrNull ?: 0,
                            createdAt = o.string("created_at").take(10)
                        )
                    }.orEmpty()
                    Result.success(runs)
                } ?: Result.failure(Exception("Réponse vide."))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** feat-088 — Ouvre une pull request de [head] vers [base] (défaut : branche par défaut du dépôt). */
    suspend fun createPullRequest(
        remoteUrl: String,
        token: String,
        head: String,
        title: String,
        body: String,
        base: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val base2 = apiBase(remoteUrl)
            ?: return@withContext Result.failure(IllegalArgumentException("L'origine n'est pas un dépôt GitHub."))
        try {
            val brancheBase = base ?: branchParDefaut(remoteUrl, token)
            val payload = buildString {
                append("{")
                append("\"title\":").append(json.encodeToString(JsonPrimitive(title)))
                append(",\"head\":\"").append(head.replace("\"", "")).append("\"")
                append(",\"base\":\"").append(brancheBase.replace("\"", "")).append("\"")
                if (body.isNotBlank()) append(",\"body\":").append(json.encodeToString(JsonPrimitive(body)))
                append("}")
            }
            val req = Request.Builder()
                .url("$base2/pulls")
                .addHeader("Accept", "application/vnd.github+json")
                .addHeader("User-Agent", "ZAgent-Android")
                .addAuth(token)
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            client.newCall(req).execute().use { resp ->
                val text = resp.body.string()
                when {
                    resp.code == 201 -> {
                        val url = runCatching {
                            json.parseToJsonElement(text).jsonObject.string("html_url")
                        }.getOrDefault("")
                        Result.success(url.ifBlank { "PR créée." })
                    }
                    resp.code == 422 -> Result.failure(Exception("Une PR existe déjà pour cette branche, ou les branches sont identiques."))
                    resp.code == 401 || resp.code == 403 -> Result.failure(Exception("Jeton GitHub refusé (HTTP ${resp.code}). Vérifie-le dans Réglages."))
                    else -> Result.failure(Exception("GitHub a répondu HTTP ${resp.code}."))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Branche par défaut du dépôt (utile comme base de PR). */
    private suspend fun branchParDefaut(remoteUrl: String, token: String): String =
        withContext(Dispatchers.IO) {
            val base = apiBase(remoteUrl) ?: return@withContext "main"
            try {
                val req = Request.Builder()
                    .url(base)
                    .addHeader("Accept", "application/vnd.github+json")
                    .addHeader("User-Agent", "ZAgent-Android")
                    .addAuth(token)
                    .get()
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use "main"
                    val d = json.parseToJsonElement(resp.body.string()).jsonObject
                    (d["default_branch"] as? JsonPrimitive)?.content ?: "main"
                } ?: "main"
            } catch (e: Exception) {
                "main"
            }
        }

    private fun Request.Builder.addAuth(token: String): Request.Builder =
        if (token.isBlank()) this else addHeader("Authorization", "Bearer $token")

    /** Un run GitHub Actions, tel qu'affiché dans l'onglet Actions (feat-089). */
    data class ActionsRun(
        val id: String,
        val name: String,
        val branch: String,
        val status: String,
        val conclusion: String,
        val runNumber: Int,
        val createdAt: String
    ) {
        /** Libellé FR court : succès / échec / en cours / annulé. */
        val libelle: String
            get() = when {
                status == "in_progress" || status == "queued" -> "en cours"
                conclusion == "success" -> "succès"
                conclusion == "failure" -> "échec"
                conclusion == "cancelled" -> "annulé"
                conclusion == "skipped" -> "ignoré"
                else -> conclusion.ifBlank { status }
            }
    }
}
