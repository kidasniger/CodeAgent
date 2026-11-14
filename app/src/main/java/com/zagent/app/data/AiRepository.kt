package com.zagent.app.data

import kotlinx.coroutines.delay
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.SSLException
import kotlin.random.Random

/**
 * corr-018 — Client IA unique de l'application (protocole chat/completions).
 *
 * Construit la requête OpenAI-compatible (modèle, historique, outils), lit le
 * flux SSE en incrémentant le contenu affiché (onDelta) et le raisonnement
 * (onReasoningDelta), réassemble les appels d'outils par index, gère le repli
 * JSON non-flux et l'annulation (cancelActive). La clé et l'URL viennent du
 * fournisseur du modèle choisi (MàJ 101) ; la configuration réseau commune
 * vit dans Constants.
 */
class AiRepository : AiRepositoryApi {

    /** sec-014 — masquage branché par l'app (SettingsManager::masquerSecrets). */
    var masqueur: ((String) -> String)? = null

    /**
     * sec-014 — applique [masqueur] au champ « content » string de chaque
     * message sortant (les contents multimodaux en JsonArray passent tels quels).
     */
    private fun masquerJsonArray(messages: JsonArray): JsonArray {
        val m = masqueur ?: return messages
        return try {
            JsonArray(messages.map { el ->
                val o = el as? JsonObject ?: return@map el
                val contenu = o["content"] as? JsonPrimitive ?: return@map el
                if (!contenu.isString) return@map el
                JsonObject(o.toMap() + ("content" to JsonPrimitive(m(contenu.content))))
            })
        } catch (e: Exception) {
            messages
        }
    }
    @Volatile
    private var client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(Constants.RESEAU_TIMEOUT_S.toLong(), TimeUnit.SECONDS)
        .readTimeout(Constants.RESEAU_TIMEOUT_S.toLong(), TimeUnit.SECONDS)
        .writeTimeout(Constants.RESEAU_TIMEOUT_S.toLong(), TimeUnit.SECONDS)
        .build()

    /**
     * err-032 — Applique un proxy HTTP optionnel (réseaux d'entreprise) à tous
     * les futurs appels. Appelé au démarrage et quand le réglage change.
     * Null = retour au proxy système d'OkHttp (aucun proxy explicite).
     */
    fun configurerProxy(proxy: java.net.Proxy?) {
        client = OkHttpClient.Builder()
            .connectTimeout(Constants.RESEAU_TIMEOUT_S.toLong(), TimeUnit.SECONDS)
            .readTimeout(Constants.RESEAU_TIMEOUT_S.toLong(), TimeUnit.SECONDS)
            .writeTimeout(Constants.RESEAU_TIMEOUT_S.toLong(), TimeUnit.SECONDS)
            .proxy(proxy)
            .build()
    }

    /** Appel réseau en cours : permet de l'interrompre quand la conversation est vidée. */
    private val activeCall = AtomicReference<Call?>(null)

    /** Annule l'appel réseau en cours (sinon le flux SSE continue en tâche de fond après l'annulation). */
    override fun cancelActive() {
        activeCall.getAndSet(null)?.cancel()
    }

    // ── err-042 — journal réseau consultable (sans clés) ────────────────

    /** Les 60 derniers événements réseau (horodatage, hôte, code/cause) — jamais de clé ni d'en-tête d'autorisation. */
    val journalReseau = ArrayDeque<String>()

    private fun journalReseauAjouter(hote: String, evenement: String) {
        journalReseau.addLast(
            java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.FRANCE).format(java.util.Date()) +
                " — " + hote + " — " + evenement
        )
        while (journalReseau.size > 60) journalReseau.removeFirst()
    }

    /** Hôte d'une URL (pour le journal et les messages) — sans port ni chemin. */
    private fun hoteDe(url: String): String =
        runCatching { java.net.URI(url).host ?: url }.getOrDefault(url)

    /** err-022/023 — nom humain du fournisseur d'après l'URL (messages 401/403/404). */
    private fun nomFournisseur(url: String): String {
        val u = url.lowercase()
        return when {
            "z.ai" in u -> "z.ai (GLM)"
            "nvidia" in u -> "NVIDIA NIM"
            "openrouter" in u -> "OpenRouter"
            "groq" in u -> "Groq"
            "127.0.0.1" in u || "localhost" in u -> "serveur local (Ollama / LM Studio)"
            else -> hoteDe(url)
        }
    }

    private val estServeurLocal: (String) -> Boolean = { u -> "127.0.0.1" in u || "localhost" in u }

    sealed class ApiResponse {
        data class Success(
            val messageContent: String?,
            val toolCalls: List<ToolCallData>?,
            val promptTokens: Int? = null,
            val completionTokens: Int? = null,
            val reasoningContent: String? = null
        ) : ApiResponse()
        data class Error(val errorMessage: String, val isCode1305: Boolean = false) : ApiResponse()
    }

    data class ToolCallData(
        val id: String,
        val name: String,
        val arguments: String
    )


    private class ToolAcc {
        var id = ""
        var name = ""
        val args = StringBuilder()
    }

    /**
     * MàJ 101 — interroge l'endpoint /models du fournisseur : sert au bouton
     * « Tester la connexion » des Réglages et à l'import des modèles d'un
     * fournisseur personnalisé. Retourne les identifiants renvoyés par le serveur.
     */
    /** mod-044 — fenêtres de contexte de la DERNIÈRE détection (id → tokens). */
    var fenetresDerniereDetection: Map<String, Long> = emptyMap()

    override suspend fun listerModeles(baseUrl: String, apiKey: String): List<String> {
        val url = (if (baseUrl.isBlank()) ModelCatalog.URL_ZAI else baseUrl)
            .replace("chat/completions", "models")
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .get()
            .build()
        return client.newCall(request).execute().use { response ->
            // MàJ 105 : OkHttp 5.x — Response.body est non-null (plus de safe-call)
            val bodyStr = response.body.string()
            if (!response.isSuccessful) {
                throw IOException("HTTP " + response.code + " sur /models")
            }
            val root = Json.parseToJsonElement(bodyStr).jsonObject
            val data = (root["data"] as? JsonArray) ?: JsonArray(emptyList())
            // mod-044 — capture context_length quand l'endpoint l'expose.
            val fenetres = LinkedHashMap<String, Long>()
            data.forEach { el ->
                val o = el as? JsonObject ?: return@forEach
                val id = (o["id"] as? JsonPrimitive)?.contentOrNull ?: return@forEach
                if (id.isBlank()) return@forEach
                val ctx = (o["context_length"] as? JsonPrimitive)?.contentOrNull?.toLongOrNull()
                    ?: (o["context_length"] as? JsonPrimitive)?.longOrNull
                    ?: (o["max_context_length"] as? JsonPrimitive)?.contentOrNull?.toLongOrNull()
                if (ctx != null && ctx > 0) fenetres[id] = ctx
            }
            fenetresDerniereDetection = fenetres
            data.mapNotNull { el ->
                (el as? JsonObject)?.get("id")?.let { (it as? JsonPrimitive)?.contentOrNull }
            }.filter { it.isNotBlank() }
        }
    }

    /** Lit la réponse en flux (SSE) : le texte part à [onDelta] au fil de l'eau, les appels d'outils sont assemblés. */
    private fun parseStream(
        response: Response,
        onDelta: (String) -> Unit,
        onReasoningDelta: (String) -> Unit
    ): ApiResponse {
        // MàJ 105 : OkHttp 5.x — body non-null ; un corps vide retombe sur Success(null, null) via ifEmpty
        val body = response.body
        val content = StringBuilder()
        val reasoning = StringBuilder()
        val tools = sortedMapOf<Int, ToolAcc>()
        val raw = StringBuilder()
        var sawSse = false
        var usagePrompt: Int? = null
        var usageCompletion: Int? = null
        val source = body.source()
        var sseFini = false
        var finishReason: String? = null
        while (true) {
            val line = source.readUtf8Line() ?: break
            if (!line.startsWith("data:")) {
                if (!sawSse) raw.append(line).append('\n')
                continue
            }
            sawSse = true
            val data = line.removePrefix("data:").trim()
            if (data.isEmpty()) continue
            if (data == "[DONE]") {
                sseFini = true
                break
            }
            val obj = try {
                Json.parseToJsonElement(data).jsonObject
            } catch (e: Exception) {
                continue
            }
            (obj["usage"] as? JsonObject)?.let { u ->
                (u["prompt_tokens"] as? JsonPrimitive)?.intOrNull?.let { usagePrompt = it }
                (u["completion_tokens"] as? JsonPrimitive)?.intOrNull?.let { usageCompletion = it }
            }
            val first = (obj["choices"] as? JsonArray)?.firstOrNull() as? JsonObject ?: continue
            // err-038 — finish_reason pour distinguer une réponse tronquée (length) d'un arrêt normal.
            (first["finish_reason"] as? JsonPrimitive)?.contentOrNull?.let {
                if (!it.isNullOrBlank()) finishReason = it
            }
            val delta = first["delta"] as? JsonObject ?: continue
            val piece = (delta["content"] as? JsonPrimitive)?.contentOrNull
            if (!piece.isNullOrEmpty()) {
                content.append(piece)
                onDelta(piece)
            }
            val thinkPiece = (delta["reasoning_content"] as? JsonPrimitive)?.contentOrNull
            if (!thinkPiece.isNullOrEmpty()) {
                reasoning.append(thinkPiece)
                onReasoningDelta(thinkPiece)
            }
            (delta["tool_calls"] as? JsonArray)?.forEachIndexed { pos, el ->
                val o = el as? JsonObject ?: return@forEachIndexed
                val idx = (o["index"] as? JsonPrimitive)?.intOrNull ?: pos
                val acc = tools.getOrPut(idx) { ToolAcc() }
                (o["id"] as? JsonPrimitive)?.contentOrNull?.let { if (it.isNotEmpty()) acc.id = it }
                val f = o["function"] as? JsonObject
                (f?.get("name") as? JsonPrimitive)?.contentOrNull?.let { if (it.isNotEmpty()) acc.name = it }
                (f?.get("arguments") as? JsonPrimitive)?.contentOrNull?.let { acc.args.append(it) }
            }
        }
        if (!sawSse && raw.isNotBlank()) return parseFullJson(raw.toString(), onDelta, onReasoningDelta)
        val calls = tools.entries.mapNotNull { (idx, a) ->
            if (a.name.isEmpty()) null
            else ToolCallData(a.id.ifEmpty { "call_$idx" }, a.name, a.args.toString().ifBlank { "{}" })
        }
        // err-038 — réponse vide (rien du tout, ou max_tokens atteint sans contenu).
        if (content.isBlank() && calls.isEmpty()) {
            return ApiResponse.Error(
                if (finishReason == "length")
                    "Le modèle a atteint la limite de sortie (max_tokens) sans produire de réponse : " +
                        "augmente max_tokens dans Réglages › Génération, ou reformule la demande."
                else
                    "Le modèle a renvoyé une réponse vide. Réessaie, ou change de modèle dans Réglages."
            )
        }
        // err-033 — flux coupé en pleine réponse : le contenu partiel est conservé et signalé.
        if (sawSse && !sseFini && (content.isNotEmpty() || calls.isNotEmpty())) {
            content.append("\n\n[flux interrompu avant la fin de la réponse — le message est peut-être incomplet]")
        }
        // err-038 — finish_reason = length avec du contenu : troncature signalée.
        if (finishReason == "length") {
            content.append("\n[réponse tronquée : limite max_tokens atteinte]")
        }
        return ApiResponse.Success(
            content.toString().ifEmpty { null }, calls.ifEmpty { null }, usagePrompt, usageCompletion,
            reasoning.toString().ifBlank { null }
        )
    }

    /** Repli : le fournisseur a ignoré le mode flux et renvoie un JSON complet. */
    private fun parseFullJson(
        text: String,
        onDelta: (String) -> Unit,
        onReasoningDelta: (String) -> Unit
    ): ApiResponse {
        val root = Json.parseToJsonElement(text).jsonObject
        val message = (root["choices"] as? JsonArray)?.firstOrNull()?.jsonObject?.get("message") as? JsonObject
        val content = (message?.get("content") as? JsonPrimitive)?.contentOrNull
        if (!content.isNullOrEmpty()) onDelta(content)
        val reasoning = (message?.get("reasoning_content") as? JsonPrimitive)?.contentOrNull
        if (!reasoning.isNullOrEmpty()) onReasoningDelta(reasoning)
        val calls = (message?.get("tool_calls") as? JsonArray)?.mapNotNull { el ->
            val o = el.jsonObject
            val id = (o["id"] as? JsonPrimitive)?.contentOrNull ?: ""
            val f = o["function"] as? JsonObject
            val name = (f?.get("name") as? JsonPrimitive)?.contentOrNull ?: ""
            val args = (f?.get("arguments") as? JsonPrimitive)?.contentOrNull ?: "{}"
            if (id.isNotEmpty() && name.isNotEmpty()) ToolCallData(id, name, args) else null
        }
        val usage = root["usage"] as? JsonObject
        val up = (usage?.get("prompt_tokens") as? JsonPrimitive)?.intOrNull
        val uc = (usage?.get("completion_tokens") as? JsonPrimitive)?.intOrNull
        return ApiResponse.Success(content, calls, up, uc, reasoning)
    }

    override suspend fun callChatCompletions(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: JsonArray,
        onRetryUpdate: (String) -> Unit,
        onDelta: (String) -> Unit,
        onReasoningDelta: (String) -> Unit,
        /** MàJ 104 — false pour un échange pur (mode Duo) : aucun outil transmis. */
        avecOutils: Boolean,
        /** maj-083 — paramètres de génération conseillés par le modèle (null = défauts). */
        generation: GenerationParams?
    ): ApiResponse {
        // Source unique de l'URL par défaut : ModelCatalog.URL_ZAI (com.zagent.app.data).
        val url = if (baseUrl.isBlank()) ModelCatalog.URL_ZAI else baseUrl

        // sec-014 — masquage des jetons branché par l'app avant tout envoi :
        // toute valeur ressemblant à un token dans un message part masquée.
        val messagesMasques = masquerJsonArray(messages)

        val requestBodyJson = buildJsonObject {
            put("model", model)
            put("messages", messagesMasques)
            if (avecOutils) {
                put("tools", AgentTools.toolsJson)
                put("tool_choice", "auto")
            }
            // maj-083 : seuls les paramètres renseignés partent dans la requête.
            if (generation?.temperature != null) put("temperature", generation.temperature)
            if (generation?.topP != null) put("top_p", generation.topP)
            if (generation?.maxTokens != null) put("max_tokens", generation.maxTokens)
            put("stream", true)
        }

        val requestBody = requestBodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        var attempt = 0
        val maxAttempts = 6
        var delaySeconds = 5L

        while (attempt < maxAttempts) {
            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()

            try {
                val call = client.newCall(request)
                activeCall.set(call)
                try {
                    call.execute().use { response ->
                    if (response.isSuccessful) {
                        return parseStream(response, onDelta, onReasoningDelta)
                    } else {
                        val responseString = response.body.string()
                        val code = response.code
                        journalReseauAjouter(hoteDe(url), "HTTP $code")
                        if (code == 401) {
                            // err-022 — le nom du fournisseur est nommément désigné.
                            return ApiResponse.Error(
                                "Clé refusée par " + nomFournisseur(url) + " (401). " +
                                    "Vérifie ou renouvelle la clé de ce fournisseur dans Réglages › Fournisseurs."
                            )
                        }
                        if (code == 403) {
                            // err-023 — accès au modèle refusé : cause et suites possibles.
                            return ApiResponse.Error(
                                "Accès refusé par " + nomFournisseur(url) + " (403) : ta clé n'a pas les droits pour ce modèle " +
                                    "(modèle privé, région restreinte ou plan insuffisant). Choisis un autre modèle, " +
                                    "ou vérifie ton abonnement/le site du fournisseur."
                            )
                        }
                        if (code == 404) {
                            // err-024 — modèle introuvable ou retiré : alternative proposée.
                            return ApiResponse.Error(
                                "Modèle « " + model + " » introuvable ou retiré chez " + nomFournisseur(url) + " (404). " +
                                    "Choisis un autre modèle dans le sélecteur de modèles ; " +
                                    "le modèle par défaut du fournisseur reste la valeur sûre."
                            )
                        }
                        // err-040 — quota / crédit épuisé : message clair, pas de
                        // nouvel essai automatique (le résultat ne changera pas).
                        if (code == 402 || responseString.contains("insufficient_quota", true)
                            || responseString.contains("quota exceeded", true)
                            || responseString.contains("exceeded your current quota", true)
                        ) {
                            return ApiResponse.Error(
                                "Quota épuisé (402) : le compte du fournisseur n'a plus de crédit. " +
                                    "Recharge le compte ou bascule sur un autre modèle/fournisseur."
                            )
                        }
                        // err-041 — le message d'erreur du fournisseur, quand il est
                        // exploitable, est recopié pour aider au diagnostic.
                        val messageFournisseur = runCatching {
                            Json.parseToJsonElement(responseString).jsonObject["error"]?.jsonObject
                                ?.get("message")?.jsonPrimitive?.content
                        }.getOrNull()

                        val is1305 = try {
                            val err = Json.parseToJsonElement(responseString).jsonObject["error"]?.jsonObject
                            err?.get("code")?.jsonPrimitive?.content == "1305"
                        } catch (e: Exception) {
                            false
                        }

                        if (code == 429 || code == 500 || code == 502 || code == 503 || code == 529 || is1305) {
                            attempt++
                            if (attempt >= maxAttempts) {
                                val msg = when {
                                    is1305 ->
                                        "Le modèle est momentanément surchargé (1305). " +
                                            "Essaie un autre modèle (bouton « Changer de modèle ») ou réessaie dans un instant."
                                    code == 529 ->
                                        // err-028 — surcharge du modèle : proposer explicitement un autre modèle.
                                        "Le modèle est surchargé (529). " +
                                            "Essaie un autre modèle (bouton « Changer de modèle ») ou réessaie dans un instant."
                                    else ->
                                        "Le serveur est surchargé (HTTP $code). Réessaie dans un instant."
                                }
                                return ApiResponse.Error(msg, is1305)
                            }
                            // err-026 — respecte Retry-After (en secondes) quand le serveur l'envoie.
                            // err-027 — backoff exponentiel plafonné + jitter anti-tempête.
                            val retryAfter = response.headers["Retry-After"]?.trim()
                                ?.takeIf { it.matches(Regex("\\d+")) }?.toLongOrNull() ?: 0L
                            val backoff = (delaySeconds * 2L).coerceAtMost(60L)
                            delaySeconds = if (retryAfter > 0) retryAfter.coerceAtMost(120L) else backoff
                            val jitter = if (delaySeconds > 2) Random.nextLong(0, delaySeconds / 2) else 0L
                            onRetryUpdate("Serveur surchargé, nouvel essai dans ${delaySeconds + jitter}s (tentative $attempt/$maxAttempts)...")
                            delay((delaySeconds + jitter) * 1000L)
                            continue
                        } else if (code == 400) {
                            // err-039 — contexte dépassé : consigne claire plutôt qu'un 400 générique.
                            val contexteDepasse = responseString.contains("context_length_exceeded", true) ||
                                responseString.contains("maximum context length", true) ||
                                responseString.contains("context window", true) ||
                                responseString.contains("too many tokens", true)
                            return ApiResponse.Error(
                                if (contexteDepasse)
                                    "Contexte dépassé (400) : la conversation est trop longue pour ce modèle. " +
                                        "Ouvre une nouvelle conversation ou vide l'historique (menu ⋯ du chat)."
                                else
                                    "La requête a été refusée (400). Le modèle ou un message est peut-être invalide : ouvre une nouvelle conversation."
                            )
                        } else {
                            // err-041 : détail du fournisseur repris s'il existe (truncature prudente).
                            val detail = messageFournisseur?.take(160)
                            return ApiResponse.Error(
                                if (detail.isNullOrBlank()) "Le serveur a répondu une erreur inattendue (HTTP $code). Réessaie dans un instant."
                                else "Erreur du fournisseur (HTTP $code) : $detail"
                            )
                        }
                    }
                    }
                } finally {
                    activeCall.compareAndSet(call, null)
                }
            } catch (e: UnknownHostException) {
                // err-031 — DNS : message adapté ; err-045 — cas particulier du serveur local.
                journalReseauAjouter(hoteDe(url), "DNS introuvable")
                return ApiResponse.Error(
                    if (estServeurLocal(url))
                        "Serveur local injoignable ($url) : vérifie qu'il tourne sur ton ordinateur, " +
                            "que le téléphone est sur le même réseau Wi-Fi, et utilise l'IP du PC " +
                            "(ex : http://192.168.1.20:11434/v1/chat/completions) plutôt que localhost."
                    else
                        "Impossible de résoudre l'adresse du serveur (DNS) : vérifie ta connexion Internet " +
                            "ou l'URL du fournisseur dans Réglages."
                )
            } catch (e: SSLException) {
                // err-030 — certificat / horloge : message adapté.
                journalReseauAjouter(hoteDe(url), "Erreur TLS : " + (e.message?.take(120) ?: "inconnue"))
                return ApiResponse.Error(
                    "Problème de certificat TLS : vérifie la date et l'heure du téléphone (Réglages système) puis réessaie ; " +
                        "pour un serveur auto-hébergé (Ollama…), son certificat est peut-être invalide ou non approuvé."
                )
            } catch (e: IOException) {
                attempt++
                journalReseauAjouter(hoteDe(url), "E/S réseau (essai $attempt/$maxAttempts) : " + (e.javaClass.simpleName))
                if (attempt >= maxAttempts) {
                    val human = if (e is java.net.SocketTimeoutException) {
                        // err-025 — le délai réel est nommé (180 s) ; les nouvelles tentatives sont automatiques.
                        "Délai dépassé (" + Constants.RESEAU_TIMEOUT_S + " s sans réponse du serveur). " +
                            "Vérifie ta connexion réseau puis réessaie."
                    } else if (estServeurLocal(url)) {
                        "Serveur local injoignable ($url) : vérifie que le serveur tourne, que le téléphone est sur le même réseau, " +
                            "et que l'URL pointe l'IP du PC (pas localhost)."
                    } else {
                        "Impossible de joindre le serveur (erreur réseau). Vérifie ta connexion Internet puis réessaie."
                    }
                    return ApiResponse.Error(human)
                }
                onRetryUpdate("Erreur réseau, nouvel essai dans ${delaySeconds}s...")
                delay(delaySeconds * 1000L)
                delaySeconds += 5L
            } catch (e: Exception) {
                return ApiResponse.Error("Une erreur inattendue est survenue : " + com.zagent.app.util.ZLog.masquer(e.localizedMessage ?: e.javaClass.simpleName))
            }
        }
        return ApiResponse.Error("Échec de la requête après plusieurs tentatives.")
    }
}
