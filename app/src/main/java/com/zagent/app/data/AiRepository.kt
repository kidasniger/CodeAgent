package com.zagent.app.data

import kotlinx.coroutines.delay
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class AiRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(180, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .build()

    sealed class ApiResponse {
        data class Success(
            val messageContent: String?,
            val toolCalls: List<ToolCallData>?
        ) : ApiResponse()
        data class Error(val errorMessage: String, val isCode1305: Boolean = false) : ApiResponse()
    }

    data class ToolCallData(
        val id: String,
        val name: String,
        val arguments: String
    )

    suspend fun callChatCompletions(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: JsonArray,
        onRetryUpdate: (String) -> Unit
    ): ApiResponse {
        val url = if (baseUrl.isBlank()) "https://api.z.ai/api/paas/v4/chat/completions" else baseUrl

        val requestBodyJson = buildJsonObject {
            put("model", model)
            put("messages", messages)
            put("tools", AgentTools.toolsJson)
            put("tool_choice", "auto")
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
                client.newCall(request).execute().use { response ->
                    val responseString = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        val root = Json.parseToJsonElement(responseString).jsonObject
                        val choices = root["choices"]?.jsonArray
                        val firstChoice = choices?.firstOrNull()?.jsonObject
                        val message = firstChoice?.get("message")?.jsonObject
                        val content = message?.get("content")?.jsonPrimitive?.contentOrNull
                        val toolCallsJson = message?.get("tool_calls")?.jsonArray

                        val toolCallsList = toolCallsJson?.mapNotNull { tcElem ->
                            val tcObj = tcElem.jsonObject
                            val id = tcObj["id"]?.jsonPrimitive?.content ?: ""
                            val funcObj = tcObj["function"]?.jsonObject
                            val name = funcObj?.get("name")?.jsonPrimitive?.content ?: ""
                            val args = funcObj?.get("arguments")?.jsonPrimitive?.content ?: "{}"
                            if (id.isNotEmpty() && name.isNotEmpty()) ToolCallData(id, name, args) else null
                        }

                        return ApiResponse.Success(content, toolCallsList)
                    } else {
                        val code = response.code
                        if (code == 401) {
                            return ApiResponse.Error("Clé API invalide (Erreur 401). Vérifiez vos réglages.")
                        }

                        var is1305 = false
                        if (responseString.contains("1305")) {
                            is1305 = true
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
