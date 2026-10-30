package com.zagent.app.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zagent.app.data.AgentTools
import com.zagent.app.data.AiRepository
import com.zagent.app.data.FileRepository
import com.zagent.app.data.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

data class ChatMessage(
    val role: String,
    val content: String?,
    val toolCalls: List<AiRepository.ToolCallData>? = null,
    val toolCallId: String? = null,
    val name: String? = null,
    val toolLog: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val settingsManager = SettingsManager(application)
    private val aiRepository = AiRepository()

    var apiKeyInput by mutableStateOf(settingsManager.apiKey)
    var modelInput by mutableStateOf(settingsManager.model)
    var baseUrlInput by mutableStateOf(settingsManager.baseUrl)
    var autoModeInput by mutableStateOf(settingsManager.autoMode)
    var workingFolderUriString by mutableStateOf(settingsManager.workingFolderUri)

    var showKeyVisible by mutableStateOf(false)
    var isSettingsOpen by mutableStateOf(false)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    var isLoading by mutableStateOf(false)
    var statusMessage by mutableStateOf("")
    var isCode1305Error by mutableStateOf(false)
    var showFallbackModelButton by mutableStateOf(false)

    var pendingWriteFile: PendingWrite? by mutableStateOf(null)

    data class PendingWrite(
        val path: String,
        val content: String,
        val toolCallId: String
    )

    var pendingWrite: PendingWrite? by mutableStateOf(null)
    private var onWriteConfirmed: ((Boolean) -> Unit)? = null

    init {
        if (settingsManager.apiKey.isBlank()) {
            isSettingsOpen = true
        }
    }

    fun saveSettings(key: String, model: String, baseUrl: String, auto: Boolean) {
        settingsManager.apiKey = key
        settingsManager.model = model
        settingsManager.baseUrl = baseUrl
        settingsManager.autoMode = auto
        apiKeyInput = key
        modelInput = model
        baseUrlInput = baseUrl
        autoModeInput = auto
        isSettingsOpen = false
    }

    fun setWorkingFolder(uri: Uri?) {
        if (uri != null) {
            try {
                getApplication<Application>().contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore
            }
            workingFolderUriString = uri.toString()
            settingsManager.workingFolderUri = uri.toString()
        }
    }

    fun clearConversation() {
        _messages.value = emptyList()
        statusMessage = ""
        isCode1305Error = false
        showFallbackModelButton = false
    }

    fun switchToOtherModel() {
        modelInput = if (modelInput == "glm-4.5-flash") "glm-4.7-flash" else "glm-4.5-flash"
        settingsManager.model = modelInput
        isCode1305Error = false
        showFallbackModelButton = false
        statusMessage = "Modèle basculé vers $modelInput."
    }

    private fun getFileRepo(): FileRepository {
        return FileRepository(getApplication(), workingFolderUriString)
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return
        if (apiKeyInput.isBlank()) {
            isSettingsOpen = true
            statusMessage = "Veuillez entrer une clé API dans les réglages."
            return
        }

        val newUserMessage = ChatMessage("user", userText)
        _messages.value = _messages.value + newUserMessage

        viewModelScope.launch(Dispatchers.IO) {
            runAgentLoop()
        }
    }

    private suspend fun runAgentLoop() {
        isLoading = true
        statusMessage = "Réflexion de l'agent..."
        isCode1305Error = false
        showFallbackModelButton = false

        val fileRepo = getFileRepo()
        val systemPrompt = "Tu es un agent de code qui travaille dans un dossier de projet. Utilise les outils pour explorer, lire, créer des dossiers et écrire des fichiers. Si tu ne connais pas la structure, commence par list_dir. Lis un fichier avant de le modifier. Écris toujours le contenu COMPLET d'un fichier. Les chemins sont relatifs au dossier de travail. Réponds en français, de façon courte."

        var stepCount = 0
        val maxSteps = 20

        while (stepCount < maxSteps) {
            stepCount++

            val jsonMessages = buildJsonArray {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                for (msg in _messages.value) {
                    add(buildJsonObject {
                        put("role", msg.role)
                        if (msg.content != null) put("content", msg.content) else put("content", JsonNull)
                        if (msg.toolCallId != null) put("tool_call_id", msg.toolCallId)
                        if (msg.name != null) put("name", msg.name)
                        if (msg.toolCalls != null && msg.toolCalls.isNotEmpty()) {
                            put("tool_calls", buildJsonArray {
                                for (tc in msg.toolCalls) {
                                    add(buildJsonObject {
                                        put("id", tc.id)
                                        put("type", "function")
                                        put("function", buildJsonObject {
                                            put("name", tc.name)
                                            put("arguments", tc.arguments)
                                        })
                                    })
                                }
                            })
                        }
                    })
                }
            }

            val result = aiRepository.callChatCompletions(
                baseUrl = baseUrlInput,
                apiKey = apiKeyInput,
                model = modelInput,
                messages = jsonMessages,
                onRetryUpdate = { retryMsg ->
                    statusMessage = retryMsg
                }
            )

            when (result) {
                is AiRepository.ApiResponse.Error -> {
                    isLoading = false
                    statusMessage = result.errorMessage
                    isCode1305Error = result.isCode1305
                    if (result.isCode1305 || result.errorMessage.contains("surchargé")) {
                        showFallbackModelButton = true
                    }
                    break
                }
                is AiRepository.ApiResponse.Success -> {
                    val content = result.messageContent
                    val toolCalls = result.toolCalls

                    if (toolCalls.isNullOrEmpty()) {
                        val assistantMsg = ChatMessage("assistant", content ?: "")
                        withContext(Dispatchers.Main) {
                            _messages.value = _messages.value + assistantMsg
                            isLoading = false
                            statusMessage = ""
                        }
                        break
                    } else {
                        val assistantMsg = ChatMessage("assistant", content, toolCalls = toolCalls)
                        withContext(Dispatchers.Main) {
                            _messages.value = _messages.value + assistantMsg
                        }

                        for (tc in toolCalls) {
                            val logLine = "🔧 ${tc.name}(...)"
                            withContext(Dispatchers.Main) {
                                statusMessage = "Exécution de $logLine..."
                            }

                            val toolResult = if (tc.name == "write_file" && !autoModeInput) {
                                val parsedArgs = try {
                                    Json.parseToJsonElement(tc.arguments).jsonObject
                                } catch (e: Exception) {
                                    JsonObject(emptyMap())
                                }
                                val path = parsedArgs["path"]?.jsonPrimitive?.content ?: ""
                                val fileContent = parsedArgs["content"]?.jsonPrimitive?.content ?: ""

                                val userAllowed = waitForWriteConfirmation(path, fileContent, tc.id)
                                if (userAllowed) {
                                    fileRepo.writeFile(path, fileContent)
                                } else {
                                    "Refusé par l'utilisateur: l'écriture de $path a été annulée."
                                }
                            } else {
                                AgentTools.executeTool(fileRepo, tc.name, tc.arguments)
                            }

                            val toolMsg = ChatMessage(
                                role = "tool",
                                content = toolResult,
                                toolCallId = tc.id,
                                name = tc.name,
                                toolLog = "🔧 ${tc.name}"
                            )
                            withContext(Dispatchers.Main) {
                                _messages.value = _messages.value + toolMsg
                            }
                        }
                    }
                }
            }
        }

        if (stepCount >= maxSteps) {
            withContext(Dispatchers.Main) {
                isLoading = false
                statusMessage = "Limite de 20 étapes atteinte pour cette demande."
            }
        }
    }

    private suspend fun waitForWriteConfirmation(path: String, content: String, toolCallId: String): Boolean {
        return suspendCancellableCoroutine { continuation ->
            pendingWrite = PendingWrite(path, content, toolCallId)
            onWriteConfirmed = { allowed ->
                pendingWrite = null
                continuation.resume(allowed, null)
            }
        }
    }

    fun resolvePendingWrite(allowed: Boolean) {
        onWriteConfirmed?.invoke(allowed)
        onWriteConfirmed = null
        pendingWrite = null
    }
}
