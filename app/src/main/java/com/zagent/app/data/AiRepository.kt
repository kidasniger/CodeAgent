package com.zagent.app.data

import kotlinx.coroutines.delay
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class AiRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(180, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .build()

    /** Appel réseau en cours : permet de l'interrompre quand la conversation est vidée. */
    private val activeCall = AtomicReference<Call?>(null)

    /** Annule l'appel réseau en cours (sinon le flux SSE continue en tâche de fond après l'annulation). */
    fun cancelActive() {
        activeCall.getAndSet(null)?.cancel()
    }

    sealed class ApiResponse {
        data class Success(
            val messageContent: String?,
            val toolCalls: List<ToolCallData>?,
            val promptTokens: Int? = null,
            val completionTokens: Int? = null
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

    /** Lit la réponse en flux (SSE) : le texte part à [onDelta] au fil de l'eau, les appels d'outils sont assemblés. */
    private fun parseStream(response: Response, onDelta: (String) -> Unit): ApiResponse {
        val body = response.body ?: return ApiResponse.Success(null, null)
        val content = StringBuilder()
        val tools = sortedMapOf<Int, ToolAcc>()
        val raw = StringBuilder()
        var sawSse = false
        var usagePrompt: Int? = null
        var usageCompletion: Int? = null
        val source = body.source()
        while (true) {
            val line = source.readUtf8Line() ?: break
            if (!line.startsWith("data:")) {
                if (!sawSse) raw.append(line).append('\n')
                continue
            }
            sawSse = true
            val data = line.removePrefix("data:").trim()
            if (data.isEmpty()) continue
            if (data == "[DONE]") break
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
            val delta = first["delta"] as? JsonObject ?: continue
            val piece = (delta["content"] as? JsonPrimitive)?.contentOrNull
            if (!piece.isNullOrEmpty()) {
                content.append(piece)
                onDelta(piece)
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
        if (!sawSse && raw.isNotBlank()) return parseFullJson(raw.toString(), onDelta)
        val calls = tools.entries.mapNotNull { (idx, a) ->
            if (a.name.isEmpty()) null
            else ToolCallData(a.id.ifEmpty { "call_$idx" }, a.name, a.args.toString().ifBlank { "{}" })
        }
        return ApiResponse.Success(content.toString().ifEmpty { null }, calls.ifEmpty { null }, usagePrompt, usageCompletion)
    }

    /** Repli : le fournisseur a ignoré le mode flux et renvoie un JSON complet. */
    private fun parseFullJson(text: String, onDelta: (String) -> Unit): ApiResponse {
        val root = Json.parseToJsonElement(text).jsonObject
        val message = (root["choices"] as? JsonArray)?.firstOrNull()?.jsonObject?.get("message") as? JsonObject
        val content = (message?.get("content") as? JsonPrimitive)?.contentOrNull
        if (!content.isNullOrEmpty()) onDelta(content)
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
        return ApiResponse.Success(content, calls, up, uc)
    }

    suspend fun callChatCompletions(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: JsonArray,
        onRetryUpdate: (String) -> Unit,
        onDelta: (String) -> Unit = {}
    ): ApiResponse {
        val url = if (baseUrl.isBlank()) "https://api.z.ai/api/paas/v4/chat/completions" else baseUrl

        val requestBodyJson = buildJsonObject {
            put("model", model)
            put("messages", messages)
            put("tools", AgentTools.toolsJson)
            put("tool_choice", "auto")
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
                        return parseStream(response, onDelta)
                    } else {
                        val responseString = response.body?.string() ?: ""
                        val code = response.code
                        if (code == 401) {
                            return ApiResponse.Error("Clé API invalide (Erreur 401). Vérifiez vos réglages.")
                        }

                        val is1305 = try {
                            val err = Json.parseToJsonElement(responseString).jsonObject["error"]?.jsonObject
                            err?.get("code")?.jsonPrimitive?.content == "1305"
                        } catch (e: Exception) {
                            false
                        }

                        if (code == 429 || code == 500 || code == 502 || code == 503 || code == 529 || is1305) {
                            attempt++
                            if (attempt >= maxAttempts) {
                                val msg = if (is1305) {
                                    "Le modèle est surchargé (Code 1305). Voulez-vous essayer l'autre modèle ?"
                                } else {
                                    "Serveur surchargé (HTTP $code). Échec après $maxAttempts essais."
                                }
                                return ApiResponse.Error(msg, is1305)
                            }
                            onRetryUpdate("Serveur surchargé, nouvel essai dans ${delaySeconds}s (tentative $attempt/$maxAttempts)...")
                            delay(delaySeconds * 1000L)
                            delaySeconds += 5L
                            continue
                        } else {
                            return ApiResponse.Error("Erreur API (HTTP $code): $responseString")
                        }
                    }
                    }
                } finally {
                    activeCall.compareAndSet(call, null)
                }
            } catch (e: IOException) {
                attempt++
                if (attempt >= maxAttempts) {
                    return ApiResponse.Error("Erreur réseau: ${e.localizedMessage}")
                }
                onRetryUpdate("Erreur réseau, nouvel essai dans ${delaySeconds}s...")
                delay(delaySeconds * 1000L)
                delaySeconds += 5L
            } catch (e: Exception) {
                return ApiResponse.Error("Erreur inattendue: ${e.localizedMessage}")
            }
        }
        return ApiResponse.Error("Échec de la requête après plusieurs tentatives.")
    }
}
