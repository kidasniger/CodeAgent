package com.zagent.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
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

    /** feat-089 — Liste les derniers runs d'Actions du dépôt (5 par défaut).
     *  Accepte une URL remote ou « owner/repo » (session 15). */
    suspend fun actionsRuns(remoteUrl: String, token: String, limit: Int = 5): Result<List<ActionsRun>> =
        withContext(Dispatchers.IO) {
            val base = depotApiBase(remoteUrl)
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
        val base2 = depotApiBase(remoteUrl)
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

    /** Branche par défaut du dépôt (utile comme base de PR).
     *  Accepte une URL remote ou « owner/repo » (session 15). */
    private suspend fun branchParDefaut(remoteUrl: String, token: String): String =
        withContext(Dispatchers.IO) {
            val base = depotApiBase(remoteUrl) ?: return@withContext "main"
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

    // ════════════════════════════════════════════════════════════════════════
    // Plein contrôle GitHub (session 15, ghc-001) — l'IA peut AGIR sur tout le
    // dépôt et le compte : dépôts, fichiers, branches, issues, pull requests,
    // releases, gists, recherche et déclenchement de workflows. Le jeton est
    // toujours injecté par l'appelant (ViewModel) : il n'apparaît jamais dans
    // les arguments de l'IA, ni dans les résultats, ni dans les journaux.
    // Toutes les fonctions renvoient un texte déjà formaté, directement
    // consommable par le modèle.
    // ════════════════════════════════════════════════════════════════════════

    private val mediaJson = "application/json; charset=utf-8".toMediaType()

    /** Message d'erreur lisible à partir d'un code HTTP et du corps GitHub. */
    private fun erreurHttp(code: Int, corps: String): String {
        val detail = runCatching {
            json.parseToJsonElement(corps).jsonObject.string("message")
        }.getOrDefault("")
        return when (code) {
            401, 403 -> "Jeton GitHub refusé (HTTP $code) — vérifie-le dans Réglages › GitHub" +
                (if (detail.isNotBlank()) " ($detail)" else "")
            404 -> "Introuvable sur GitHub (HTTP 404) — vérifie le dépôt, le chemin ou la référence" +
                (if (detail.isNotBlank()) " ($detail)" else "")
            409 -> "Conflit GitHub (HTTP 409)" + (if (detail.isNotBlank()) " — $detail" else "")
            422 -> "GitHub a refusé la requête (HTTP 422)" + (if (detail.isNotBlank()) " — $detail" else "")
            else -> "GitHub a répondu HTTP $code" + (if (detail.isNotBlank()) " — $detail" else "")
        }
    }

    /**
     * Appel HTTP générique : renvoie (code HTTP, corps texte). Basculle sur
     * Dispatchers.IO ; toute erreur réseau devient Result.failure.
     */
    private suspend fun appel(
        url: String,
        token: String,
        methode: String = "GET",
        payload: String? = null
    ): Result<Pair<Int, String>> = withContext(Dispatchers.IO) {
        try {
            val b = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/vnd.github+json")
                .addHeader("User-Agent", "ZAgent-Android")
                .addAuth(token)
            when (methode) {
                "POST" -> b.post((payload ?: "{}").toRequestBody(mediaJson))
                "PUT" -> b.put((payload ?: "{}").toRequestBody(mediaJson))
                "PATCH" -> b.patch((payload ?: "{}").toRequestBody(mediaJson))
                "DELETE" -> if (payload != null) b.delete(payload.toRequestBody(mediaJson)) else b.delete()
                else -> b.get()
            }
            client.newCall(b.build()).execute().use { resp ->
                Result.success(resp.code to resp.body.string())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** payload JSON compact à partir de paires clé/valeur (valeurs déjà sérialisées). */
    private fun payloadJson(vararg paires: Pair<String, JsonElement>): String =
        buildJsonObject { paires.forEach { (k, v) -> put(k, v) } }.toString()

    private fun jchaine(s: String): JsonElement = JsonPrimitive(s)
    private fun jbooleen(b: Boolean): JsonElement = JsonPrimitive(b)

    private fun JsonObject.tableau(cle: String): JsonArray =
        (this[cle] as? JsonArray) ?: JsonArray(emptyList())

    /**
     * Accepte indifféremment « owner/repo » ou une URL github.com (https/ssh)
     * et renvoie la base API du dépôt. Null si le format n'est pas reconnu.
     */
    fun depotApiBase(repoOuRemote: String): String? {
        val s = repoOuRemote.trim().removeSuffix("/")
        if (s.contains("github.com")) return apiBase(s)
        val m = Regex("^([A-Za-z0-9._-]+)/([A-Za-z0-9._-]+)$").find(s) ?: return null
        val (owner, repo) = m.destructured
        if (owner.isBlank() || repo.isBlank()) return null
        return "https://api.github.com/repos/$owner/$repo"
    }

    /** Encode chaque segment d'un chemin de fichier pour l'URL de l'API contents. */
    private fun cheminUrl(chemin: String): String =
        chemin.trim('/').split('/').joinToString("/") {
            java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20")
        }

    /** Identité du jeton : login, nom, type de compte, dépôts publics. */
    suspend fun utilisateur(token: String): Result<String> {
        val r = appel("https://api.github.com/user", token)
            .getOrElse { return Result.failure(it) }
        if (r.first != 200) return Result.failure(Exception(erreurHttp(r.first, r.second)))
        val d = runCatching { json.parseToJsonElement(r.second).jsonObject }.getOrElse {
            return Result.failure(Exception("Réponse GitHub illisible."))
        }
        val login = d.string("login")
        if (login.isBlank()) return Result.failure(Exception("Réponse GitHub inattendue."))
        val nom = d.string("name").ifBlank { login }
        val publics = (d["public_repos"] as? JsonPrimitive)?.content ?: "?"
        return Result.success(
            "Jeton valide : connecté à GitHub en tant que $login ($nom), compte ${d.string("type")}, $publics dépôts publics."
        )
    }

    /** Liste les dépôts accessibles au jeton, triés par activité récente. */
    suspend fun listeDepots(token: String, limite: Int, visibilite: String): Result<String> {
        val vis = when (visibilite) { "private", "public" -> visibilite; else -> "all" }
        val url = "https://api.github.com/user/repos?per_page=$limite&sort=updated&visibility=$vis&affiliation=owner,collaborator"
        val r = appel(url, token).getOrElse { return Result.failure(it) }
        if (r.first != 200) return Result.failure(Exception(erreurHttp(r.first, r.second)))
        val liste = runCatching {
            json.parseToJsonElement(r.second) as? JsonArray ?: JsonArray(emptyList())
        }.getOrDefault(JsonArray(emptyList()))
        if (liste.isEmpty()) return Result.success("Aucun dépôt accessible avec ce jeton.")
        val lignes = liste.mapNotNull { el ->
            val d = el as? JsonObject ?: return@mapNotNull null
            val prive = (d["private"] as? JsonPrimitive)?.content == "true"
            val etoiles = (d["stargazers_count"] as? JsonPrimitive)?.content ?: "0"
            val desc = d.string("description").take(80)
            d.string("full_name") + " — " + (if (prive) "privé" else "public") +
                ", étoiles " + etoiles + ", branche " + d.string("default_branch").ifBlank { "?" } +
                (if (desc.isNotBlank()) ", " + desc else "")
        }
        return Result.success(if (lignes.isEmpty()) "Aucun dépôt accessible avec ce jeton." else lignes.joinToString("\n"))
    }

    /** Crée un dépôt sur le compte du jeton ; renvoie l'URL du dépôt. */
    suspend fun creerDepot(token: String, nom: String, prive: Boolean, description: String, autoInit: Boolean): Result<String> {
        val payload = buildJsonObject {
            put("name", JsonPrimitive(nom))
            put("private", JsonPrimitive(prive))
            put("auto_init", JsonPrimitive(autoInit))
            if (description.isNotBlank()) put("description", JsonPrimitive(description.take(200)))
        }.toString()
        val r = appel("https://api.github.com/user/repos", token, "POST", payload)
            .getOrElse { return Result.failure(it) }
        if (r.first != 201) return Result.failure(Exception(erreurHttp(r.first, r.second)))
        val d = runCatching { json.parseToJsonElement(r.second).jsonObject }.getOrDefault(JsonObject(emptyMap()))
        return Result.success("Dépôt créé : " + d.string("html_url").ifBlank { "https://github.com/new" })
    }

    /** Lit un fichier (ou liste un dossier) via l'API contents ; texte décodé. */
    suspend fun lireFichier(repoRef: String, token: String, chemin: String, ref: String): Result<String> {
        val base = depotApiBase(repoRef) ?: return Result.failure(IllegalArgumentException("Dépôt non reconnu ($repoRef)."))
        val q = if (ref.isNotBlank()) "?ref=" + java.net.URLEncoder.encode(ref, "UTF-8") else ""
        val r = appel("$base/contents/${cheminUrl(chemin)}$q", token).getOrElse { return Result.failure(it) }
        if (r.first != 200) return Result.failure(Exception(erreurHttp(r.first, r.second)))
        val d = runCatching { json.parseToJsonElement(r.second) }.getOrElse { return Result.failure(Exception("Réponse GitHub illisible.")) }
        if (d is JsonArray) {
            // un dossier : liste les entrées
            val lignes = d.mapNotNull { el ->
                val o = el as? JsonObject ?: return@mapNotNull null
                val type = if (o.string("type") == "dir") "dossier" else "fichier"
                o.string("name") + " (" + type + ((o["size"] as? JsonPrimitive)?.content?.let { ", $it octets" } ?: "") + ")"
            }
            return Result.success(if (lignes.isEmpty()) "Dossier vide sur GitHub." else lignes.joinToString("\n"))
        }
        val o = d as? JsonObject ?: return Result.failure(Exception("Réponse GitHub inattendue."))
        val taille = (o["size"] as? JsonPrimitive)?.content ?: "0"
        val b64 = o.string("content").replace("\n", "").replace("\r", "")
        if (b64.isBlank()) return Result.success("« ${o.string("path")} » est vide ou trop gros pour l'API contents ($taille octets) — passe par git pour les gros fichiers.")
        return runCatching {
            val bytes = java.util.Base64.getDecoder().decode(b64)
            val texte = String(bytes, Charsets.UTF_8)
            if (texte.length > 40000) texte.take(40000) + "\n…[tronqué à 40000 caractères sur $taille octets]"
            else texte
        }.recoverCatching {
            "Contenu binaire (${o.string("encoding")}, $taille octets) — non affichable en texte."
        }
    }

    /** SHA actuel d'un fichier (null si absent). */
    private suspend fun shaFichier(base: String, token: String, chemin: String, branche: String): String? {
        val q = if (branche.isNotBlank()) "?ref=" + java.net.URLEncoder.encode(branche, "UTF-8") else ""
        val r = appel("$base/contents/${cheminUrl(chemin)}$q", token).getOrElse { return null }
        if (r.first != 200) return null
        return runCatching { json.parseToJsonElement(r.second).jsonObject.string("sha") }.getOrNull()
    }

    /** Crée ou met à jour un fichier via l'API contents (commit unique). */
    suspend fun ecrireFichier(
        repoRef: String, token: String, chemin: String, contenu: String, message: String, branche: String
    ): Result<String> {
        val base = depotApiBase(repoRef) ?: return Result.failure(IllegalArgumentException("Dépôt non reconnu ($repoRef)."))
        val sha = shaFichier(base, token, chemin, branche)
        val b64 = java.util.Base64.getEncoder().encodeToString(contenu.toByteArray(Charsets.UTF_8))
        val payload = buildJsonObject {
            put("message", JsonPrimitive(message.ifBlank { "Mise à jour de $chemin via ZAgent" }))
            put("content", JsonPrimitive(b64))
            if (branche.isNotBlank()) put("branch", JsonPrimitive(branche))
            if (sha != null) put("sha", JsonPrimitive(sha))
        }.toString()
        val r = appel("$base/contents/${cheminUrl(chemin)}", token, "PUT", payload).getOrElse { return Result.failure(it) }
        if (r.first != 200 && r.first != 201) return Result.failure(Exception(erreurHttp(r.first, r.second)))
        val d = runCatching { json.parseToJsonElement(r.second).jsonObject }.getOrDefault(JsonObject(emptyMap()))
        val commit = ((d["commit"] as? JsonObject)?.string("sha") ?: "").take(7)
        val action = if (sha != null) "mis à jour" else "créé"
        return Result.success(
            "Fichier $chemin $action sur GitHub" +
                (if (branche.isNotBlank()) " (branche $branche)" else "") +
                (if (commit.isNotBlank()) " — commit $commit" else "") +
                " (${contenu.toByteArray(Charsets.UTF_8).size} octets)."
        )
    }

    /** Supprime un fichier via l'API contents (commit unique). */
    suspend fun supprimerFichier(repoRef: String, token: String, chemin: String, message: String, branche: String): Result<String> {
        val base = depotApiBase(repoRef) ?: return Result.failure(IllegalArgumentException("Dépôt non reconnu ($repoRef)."))
        val sha = shaFichier(base, token, chemin, branche)
            ?: return Result.failure(Exception("Fichier introuvable sur GitHub : $chemin (rien à supprimer)."))
        val payload = buildJsonObject {
            put("message", JsonPrimitive(message.ifBlank { "Suppression de $chemin via ZAgent" }))
            put("sha", JsonPrimitive(sha))
            if (branche.isNotBlank()) put("branch", JsonPrimitive(branche))
        }.toString()
        val r = appel("$base/contents/${cheminUrl(chemin)}", token, "DELETE", payload).getOrElse { return Result.failure(it) }
        if (r.first != 200) return Result.failure(Exception(erreurHttp(r.first, r.second)))
        return Result.success("Fichier $chemin supprimé du dépôt GitHub" + (if (branche.isNotBlank()) " (branche $branche)." else "."))
    }

    /** Liste les branches du dépôt distant. */
    suspend fun listerBranches(repoRef: String, token: String): Result<String> {
        val base = depotApiBase(repoRef) ?: return Result.failure(IllegalArgumentException("Dépôt non reconnu."))
        val r = appel("$base/branches?per_page=100", token).getOrElse { return Result.failure(it) }
        if (r.first != 200) return Result.failure(Exception(erreurHttp(r.first, r.second)))
        val liste = runCatching { json.parseToJsonElement(r.second) as? JsonArray ?: JsonArray(emptyList()) }
            .getOrDefault(JsonArray(emptyList()))
        if (liste.isEmpty()) return Result.success("Aucune branche sur le dépôt distant.")
        val lignes = liste.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val nom = o.string("name")
            val sha = o.string("sha").take(7)
            val protegee = ((o["protection"] as? JsonObject)?.get("enabled") as? JsonPrimitive)?.content == "true"
            "$nom — $sha" + (if (protegee) " (protégée)" else "")
        }
        return Result.success(if (lignes.isEmpty()) "Aucune branche." else lignes.joinToString("\n"))
    }

    /** Crée une branche distante à partir d'une référence existante (défaut : branche par défaut). */
    suspend fun creerBranche(repoRef: String, token: String, nom: String, depuis: String): Result<String> {
        val base = depotApiBase(repoRef) ?: return Result.failure(IllegalArgumentException("Dépôt non reconnu."))
        val source = depuis.ifBlank { "" }
        val refUrl = if (source.isNotBlank()) "heads/${java.net.URLEncoder.encode(source, "UTF-8")}" else ""
        var sha = ""
        if (refUrl.isNotBlank()) {
            val r0 = appel("$base/git/ref/$refUrl", token).getOrElse { return Result.failure(it) }
            if (r0.first == 200) sha = runCatching { json.parseToJsonElement(r0.second).jsonObject.string("sha") }.getOrDefault("")
        }
        if (sha.isBlank()) {
            val info = appel(base, token).getOrElse { return Result.failure(it) }
            if (info.first == 200) sha = runCatching {
                json.parseToJsonElement(info.second).jsonObject.string("default_branch")
            }.getOrDefault("").let { brancheDefaut ->
                if (brancheDefaut.isBlank()) "" else {
                    val r1 = appel("$base/git/ref/heads/$brancheDefaut", token).getOrElse { return Result.failure(it) }
                    runCatching { json.parseToJsonElement(r1.second).jsonObject.string("sha") }.getOrDefault("")
                }
            }
        }
        if (sha.isBlank()) return Result.failure(Exception("Impossible de résoudre la référence source (${source.ifBlank { "branche par défaut" }})."))
        val payload = payloadJson(
            "ref" to jchaine("refs/heads/$nom"),
            "sha" to jchaine(sha)
        )
        val r = appel("$base/git/refs", token, "POST", payload).getOrElse { return Result.failure(it) }
        if (r.first != 201) return Result.failure(Exception(erreurHttp(r.first, r.second)))
        return Result.success("Branche « $nom » créée sur GitHub depuis ${source.ifBlank { "la branche par défaut" }} (${sha.take(7)}).")
    }

    /** Liste les issues (les pull requests sont exclues). */
    suspend fun listerIssues(repoRef: String, token: String, etat: String, limite: Int): Result<String> {
        val base = depotApiBase(repoRef) ?: return Result.failure(IllegalArgumentException("Dépôt non reconnu."))
        val etat2 = when (etat) { "open", "closed", "all" -> etat; else -> "open" }
        val r = appel("$base/issues?state=$etat2&per_page=$limite", token).getOrElse { return Result.failure(it) }
        if (r.first != 200) return Result.failure(Exception(erreurHttp(r.first, r.second)))
        val liste = runCatching { json.parseToJsonElement(r.second) as? JsonArray ?: JsonArray(emptyList()) }
            .getOrDefault(JsonArray(emptyList()))
        val lignes = liste.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            if (o["pull_request"] != null) return@mapNotNull null // une PR n'est pas une issue
            val labels = o.tableau("labels").mapNotNull { (it as? JsonObject)?.string("name") }
            "#" + o.string("number") + " [" + o.string("state") + "] " + o.string("title").take(90) +
                (if (labels.isNotEmpty()) " (" + labels.joinToString(", ") + ")" else "")
        }
        return Result.success(if (lignes.isEmpty()) "Aucune issue ($etat2)." else lignes.joinToString("\n"))
    }

    /** Crée une issue ; renvoie numéro + URL. */
    suspend fun creerIssue(repoRef: String, token: String, titre: String, corps: String, labels: List<String>): Result<String> {
        val base = depotApiBase(repoRef) ?: return Result.failure(IllegalArgumentException("Dépôt non reconnu."))
        val payload = buildJsonObject {
            put("title", JsonPrimitive(titre))
            if (corps.isNotBlank()) put("body", JsonPrimitive(corps))
            if (labels.isNotEmpty()) put("labels", JsonArray(labels.map { JsonPrimitive(it) }))
        }.toString()
        val r = appel("$base/issues", token, "POST", payload).getOrElse { return Result.failure(it) }
        if (r.first != 201) return Result.failure(Exception(erreurHttp(r.first, r.second)))
        val d = runCatching { json.parseToJsonElement(r.second).jsonObject }.getOrDefault(JsonObject(emptyMap()))
        return Result.success("Issue #" + d.string("number") + " créée : " + d.string("html_url"))
    }

    /** Commente une issue (ou une PR : même API). */
    suspend fun commenterIssue(repoRef: String, token: String, numero: Int, commentaire: String): Result<String> {
        val base = depotApiBase(repoRef) ?: return Result.failure(IllegalArgumentException("Dépôt non reconnu."))
        val payload = payloadJson("body" to jchaine(commentaire))
        val r = appel("$base/issues/$numero/comments", token, "POST", payload).getOrElse { return Result.failure(it) }
        if (r.first != 201) return Result.failure(Exception(erreurHttp(r.first, r.second)))
        val d = runCatching { json.parseToJsonElement(r.second).jsonObject }.getOrDefault(JsonObject(emptyMap()))
        return Result.success("Commentaire publié sur #$numero : " + d.string("html_url"))
    }

    /** Change l'état d'une issue : close / reopen. */
    suspend fun etatIssue(repoRef: String, token: String, numero: Int, etat: String): Result<String> {
        val base = depotApiBase(repoRef) ?: return Result.failure(IllegalArgumentException("Dépôt non reconnu."))
        val payload = payloadJson("state" to jchaine(if (etat == "reopen") "open" else "closed"))
        val r = appel("$base/issues/$numero", token, "PATCH", payload).getOrElse { return Result.failure(it) }
        if (r.first != 200) return Result.failure(Exception(erreurHttp(r.first, r.second)))
        return Result.success("Issue #$numero " + (if (etat == "reopen") "réouverte." else "fermée."))
    }

    /** Liste les pull requests du dépôt. */
    suspend fun listerPulls(repoRef: String, token: String, etat: String, limite: Int): Result<String> {
        val base = depotApiBase(repoRef) ?: return Result.failure(IllegalArgumentException("Dépôt non reconnu."))
        val etat2 = when (etat) { "open", "closed", "all" -> etat; else -> "open" }
        val r = appel("$base/pulls?state=$etat2&per_page=$limite", token).getOrElse { return Result.failure(it) }
        if (r.first != 200) return Result.failure(Exception(erreurHttp(r.first, r.second)))
        val liste = runCatching { json.parseToJsonElement(r.second) as? JsonArray ?: JsonArray(emptyList()) }
            .getOrDefault(JsonArray(emptyList()))
        if (liste.isEmpty()) return Result.success("Aucune pull request ($etat2).")
        val lignes = liste.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val draft = (o["draft"] as? JsonPrimitive)?.content == "true"
            val head = (o["head"] as? JsonObject)?.string("ref") ?: "?"
            val baseRef = (o["base"] as? JsonObject)?.string("ref") ?: "?"
            "#" + o.string("number") + " [" + o.string("state") + (if (draft) ", brouillon" else "") + "] " +
                o.string("title").take(80) + " — $head → $baseRef"
        }
        return Result.success(lignes.joinToString("\n"))
    }

    /** Fusionne une pull request (merge / squash / rebase). */
    suspend fun fusionnerPull(repoRef: String, token: String, numero: Int, methode: String): Result<String> {
        val base = depotApiBase(repoRef) ?: return Result.failure(IllegalArgumentException("Dépôt non reconnu."))
        val m = when (methode) { "squash", "rebase", "merge" -> methode; else -> "merge" }
        val payload = payloadJson("merge_method" to jchaine(m))
        val r = appel("$base/pulls/$numero/merge", token, "PUT", payload).getOrElse { return Result.failure(it) }
        if (r.first != 200) return Result.failure(Exception(erreurHttp(r.first, r.second)))
        return Result.success("Pull request #$numero fusionnée ($m).")
    }

    /** Ferme une pull request sans la fusionner. */
    suspend fun fermerPull(repoRef: String, token: String, numero: Int): Result<String> {
        val base = depotApiBase(repoRef) ?: return Result.failure(IllegalArgumentException("Dépôt non reconnu."))
        val payload = payloadJson("state" to jchaine("closed"))
        val r = appel("$base/pulls/$numero", token, "PATCH", payload).getOrElse { return Result.failure(it) }
        if (r.first != 200) return Result.failure(Exception(erreurHttp(r.first, r.second)))
        return Result.success("Pull request #$numero fermée.")
    }

    /** Liste les releases du dépôt. */
    suspend fun listerReleases(repoRef: String, token: String, limite: Int): Result<String> {
        val base = depotApiBase(repoRef) ?: return Result.failure(IllegalArgumentException("Dépôt non reconnu."))
        val r = appel("$base/releases?per_page=$limite", token).getOrElse { return Result.failure(it) }
        if (r.first != 200) return Result.failure(Exception(erreurHttp(r.first, r.second)))
        val liste = runCatching { json.parseToJsonElement(r.second) as? JsonArray ?: JsonArray(emptyList()) }
            .getOrDefault(JsonArray(emptyList()))
        if (liste.isEmpty()) return Result.success("Aucune release publiée.")
        val lignes = liste.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val draft = (o["draft"] as? JsonPrimitive)?.content == "true"
            val pre = (o["prerelease"] as? JsonPrimitive)?.content == "true"
            o.string("tag_name") + " — " + o.string("name").ifBlank { "(sans nom)" } +
                " (" + o.string("published_at").take(10) +
                (if (draft) ", brouillon" else "") + (if (pre) ", pré-release" else "") + ")"
        }
        return Result.success(lignes.joinToString("\n"))
    }

    /** Crée une release (le tag est créé sur la branche par défaut s'il n'existe pas). */
    suspend fun creerRelease(repoRef: String, token: String, tag: String, nom: String, corps: String): Result<String> {
        val base = depotApiBase(repoRef) ?: return Result.failure(IllegalArgumentException("Dépôt non reconnu."))
        val payload = buildJsonObject {
            put("tag_name", JsonPrimitive(tag))
            put("name", JsonPrimitive(nom.ifBlank { tag }))
            if (corps.isNotBlank()) put("body", JsonPrimitive(corps.take(4000)))
        }.toString()
        val r = appel("$base/releases", token, "POST", payload).getOrElse { return Result.failure(it) }
        if (r.first != 201) return Result.failure(Exception(erreurHttp(r.first, r.second)))
        val d = runCatching { json.parseToJsonElement(r.second).jsonObject }.getOrDefault(JsonObject(emptyMap()))
        return Result.success("Release $tag créée : " + d.string("html_url"))
    }

    /** Crée un gist (privé sauf demande explicite). */
    suspend fun creerGist(token: String, description: String, nomFichier: String, contenu: String, publique: Boolean): Result<String> {
        val fichier = nomFichier.ifBlank { "zagent.txt" }
        val payload = buildJsonObject {
            if (description.isNotBlank()) put("description", JsonPrimitive(description.take(200)))
            put("public", JsonPrimitive(publique))
            put("files", buildJsonObject {
                put(fichier, buildJsonObject { put("content", JsonPrimitive(contenu)) })
            })
        }.toString()
        val r = appel("https://api.github.com/gists", token, "POST", payload).getOrElse { return Result.failure(it) }
        if (r.first != 201) return Result.failure(Exception(erreurHttp(r.first, r.second)))
        val d = runCatching { json.parseToJsonElement(r.second).jsonObject }.getOrDefault(JsonObject(emptyMap()))
        return Result.success("Gist créé (" + (if (publique) "public" else "secret") + ") : " + d.string("html_url"))
    }

    /** Recherche GitHub : repositories / issues / code. */
    suspend fun rechercher(token: String, type: String, requete: String, limite: Int): Result<String> {
        val endpoint = when (type) { "issues", "code" -> type; else -> "repositories" }
        val q = java.net.URLEncoder.encode(requete.take(200), "UTF-8")
        val r = appel("https://api.github.com/search/$endpoint?q=$q&per_page=$limite", token)
            .getOrElse { return Result.failure(it) }
        if (r.first != 200) return Result.failure(Exception(erreurHttp(r.first, r.second)))
        val d = runCatching { json.parseToJsonElement(r.second).jsonObject }.getOrDefault(JsonObject(emptyMap()))
        val total = (d["total_count"] as? JsonPrimitive)?.content ?: "?"
        val liste = d.tableau("items")
        if (liste.isEmpty()) return Result.success("Aucun résultat pour « $requete ».")
        val lignes = liste.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            when (endpoint) {
                "repositories" -> o.string("full_name") + " — étoiles " +
                    ((o["stargazers_count"] as? JsonPrimitive)?.content ?: "0") +
                    (if (o.string("description").isNotBlank()) ", " + o.string("description").take(70) else "")
                "issues" -> o.string("repository_url").substringAfterLast("/repos/") + "#" + o.string("number") +
                    " [" + o.string("state") + "] " + o.string("title").take(80)
                else -> o.string("repository_url").substringAfterLast("/repos/") + " : " + o.string("path")
            }
        }
        return Result.success("$total résultat(s) — " + lignes.joinToString("\n"))
    }

    /** Déclenche un workflow GitHub Actions (workflow_dispatch). */
    suspend fun declencherWorkflow(repoRef: String, token: String, workflow: String, ref: String): Result<String> {
        val base = depotApiBase(repoRef) ?: return Result.failure(IllegalArgumentException("Dépôt non reconnu."))
        if (ref.isBlank()) return Result.failure(IllegalArgumentException("La référence (ref) est requise pour déclencher un workflow."))
        val payload = payloadJson("ref" to jchaine(ref))
        val r = appel("$base/actions/workflows/$workflow/dispatches", token, "POST", payload)
            .getOrElse { return Result.failure(it) }
        if (r.first != 204) return Result.failure(Exception(erreurHttp(r.first, r.second)))
        return Result.success("Workflow « $workflow » déclenché sur la référence $ref — suis son statut avec github_actions.")
    }

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
