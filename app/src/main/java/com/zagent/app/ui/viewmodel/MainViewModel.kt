package com.zagent.app.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zagent.app.data.AgentTools
import com.zagent.app.data.AiRepository
import com.zagent.app.data.AskQuestion
import com.zagent.app.data.parseAskQuestions
import com.zagent.app.data.DiffUtil
import com.zagent.app.data.FileRepository
import com.zagent.app.data.SettingsManager
import com.zagent.app.ui.editor.EditorController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class ChatMessage(
    val role: String,
    val content: String?,
    val toolCalls: List<AiRepository.ToolCallData>? = null,
    val toolCallId: String? = null,
    val name: String? = null,
    val toolLog: String? = null,
    val time: Long = System.currentTimeMillis()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val settingsManager = SettingsManager(application)
    private val aiRepository = AiRepository()

    // ── Réglages (miroirs d'état pour l'UI) ─────────────────────────────────
    var apiKeyInput by mutableStateOf(settingsManager.apiKey)
    var modelInput by mutableStateOf(settingsManager.model)
    var baseUrlInput by mutableStateOf(settingsManager.baseUrl)
    var autoModeInput by mutableStateOf(settingsManager.autoMode)
    var workingFolderUriString by mutableStateOf(settingsManager.workingFolderUri)

    var showKeyVisible by mutableStateOf(false)
    var isSettingsOpen by mutableStateOf(false)

    // ── Apparence & flux de démarrage ───────────────────────────────────────
    var themeIsDark by mutableStateOf(settingsManager.themeIsDark)
    var onboardingDone by mutableStateOf(settingsManager.onboardingDone)

    fun setTheme(dark: Boolean) {
        themeIsDark = dark
        settingsManager.themeIsDark = dark
    }

    fun completeOnboarding() {
        onboardingDone = true
        settingsManager.onboardingDone = true
    }

    // ── Conversation ────────────────────────────────────────────────────────
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    var isLoading by mutableStateOf(false)
    var statusMessage by mutableStateOf("")
    var isCode1305Error by mutableStateOf(false)
    var showFallbackModelButton by mutableStateOf(false)
    var isKeyInvalid by mutableStateOf(false)
    var isNetworkError by mutableStateOf(false)

    /** Nombre de relances automatiques après perte de réseau (plafonné à 3). */
    var autoRetryCount by mutableStateOf(0)

    val hasBlockingError: Boolean
        get() = isKeyInvalid || isNetworkError || showFallbackModelButton || isCode1305Error

    data class PendingWrite(
        val path: String,
        val content: String,
        val toolCallId: String,
        val diff: DiffUtil.DiffResult
    )

    var pendingWrite: PendingWrite? by mutableStateOf(null)
    private var onWriteConfirmed: ((Boolean) -> Unit)? = null

    /** Suppression demandée par l'agent (outil delete_file), en attente de confirmation. */
    data class PendingDelete(val path: String, val toolCallId: String)

    var pendingDelete: PendingDelete? by mutableStateOf(null)
    private var onDeleteConfirmed: ((Boolean) -> Unit)? = null

    /** Questions à choix posées par l'agent (outil ask_user), en attente de réponse. */
    data class PendingAsk(val toolCallId: String, val questions: List<AskQuestion>)

    var pendingAsk: PendingAsk? by mutableStateOf(null)
    private var onAskAnswered: ((String?) -> Unit)? = null
    private var agentJob: Job? = null

    /** Fichiers modifiés par l'agent pendant la session (badge explorateur). */
    val modifiedPaths = mutableStateMapOf<String, Boolean>()

    init {
        // L'onboarding et l'état vide guident l'utilisateur ; on n'ouvre
        // plus les réglages automatiquement au démarrage.
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
        if (key.isNotBlank()) isKeyInvalid = false
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
            browserChildren.clear()
            expandedDirs.clear()
            editor.clear()
            isEditorOpen = false
            refreshFolderLabel()
        }
    }

    fun clearConversation() {
        agentJob?.cancel()
        agentJob = null
        onWriteConfirmed = null
        pendingWrite = null
        onAskAnswered = null
        pendingAsk = null
        onDeleteConfirmed = null
        pendingDelete = null
        isLoading = false
        _messages.value = emptyList()
        statusMessage = ""
        isCode1305Error = false
        showFallbackModelButton = false
        isKeyInvalid = false
        isNetworkError = false
        autoRetryCount = 0
    }

    fun switchToOtherModel() {
        modelInput = if (modelInput == "glm-4.5-flash") "glm-4.7-flash" else "glm-4.5-flash"
        settingsManager.model = modelInput
        isCode1305Error = false
        showFallbackModelButton = false
        statusMessage = ""
    }

    /** Relance la boucle de l'agent sur l'historique courant (après une erreur). */
    fun retryLast(auto: Boolean = false) {
        if (isLoading) return
        if (_messages.value.isEmpty()) return
        if (auto) autoRetryCount++
        statusMessage = ""
        isNetworkError = false
        isKeyInvalid = false
        isCode1305Error = false
        showFallbackModelButton = false
        agentJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                runAgentLoop()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // Une erreur inattendue dans la boucle de l'agent ne doit jamais fermer l'appli.
                withContext(NonCancellable + Dispatchers.Main) {
                    statusMessage = "Erreur de l'agent : " + (e.localizedMessage ?: e.javaClass.simpleName)
                }
            } finally {
                withContext(NonCancellable + Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    // ── Éditeur de code ─────────────────────────────────────────────────────
    val editor = EditorController(viewModelScope) { getFileRepo() }
    var isEditorOpen by mutableStateOf(false)

    // ── Explorateur de fichiers ─────────────────────────────────────────────
    var isFileBrowserOpen by mutableStateOf(false)
    var folderLabel by mutableStateOf("Projet")
    data class BrowserFilePreview(val path: String, val content: String)
    var browserFilePreview by mutableStateOf<BrowserFilePreview?>(null)
        private set
    var browserFileLoading by mutableStateOf(false)
        private set
    var browserError by mutableStateOf<String?>(null)
        private set

    private fun refreshFolderLabel() {
        viewModelScope.launch(Dispatchers.IO) {
            val name = getFileRepo().folderName()
            withContext(Dispatchers.Main) { folderLabel = name }
        }
    }

    /** Enfants chargés par chemin (".", "app", "app/src"…). */
    val browserChildren = mutableStateMapOf<String, List<FileRepository.Entry>>()
    val expandedDirs = mutableStateMapOf<String, Boolean>()
    private val loadingDirectories = mutableStateMapOf<String, Boolean>()
    var browserCount by mutableStateOf(0)
    var browserLoading by mutableStateOf(false)

    fun ensureLoaded(path: String) {
        if (browserChildren.containsKey(path) || loadingDirectories.containsKey(path)) return
        loadingDirectories[path] = true
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { getFileRepo().listEntries(path) }
            withContext(Dispatchers.Main) {
                result.onSuccess { browserChildren[path] = it }
                loadingDirectories.remove(path)
            }
        }
    }

    fun toggleDir(path: String) {
        if (expandedDirs.containsKey(path)) {
            expandedDirs.remove(path)
        } else {
            expandedDirs[path] = true
            ensureLoaded(path)
        }
    }

    /** Recharge l'arbre en conservant les dossiers dépliés. */
    fun refreshBrowser() {
        refreshFolderLabel()
        browserLoading = true
        val dirs = listOf(".") + expandedDirs.keys.toList()
        viewModelScope.launch(Dispatchers.IO) {
            val repo = getFileRepo()
            val loaded = dirs.map { it to repo.listEntries(it) }
            withContext(Dispatchers.Main) {
                for ((p, list) in loaded) browserChildren[p] = list
                browserChildren.keys.toList().forEach { k ->
                    if (k != "." && !expandedDirs.containsKey(k)) browserChildren.remove(k)
                }
                expandedDirs.keys.toList().forEach { d ->
                    val parent = if (d.contains('/')) d.substringBeforeLast('/') else "."
                    val siblings = browserChildren[parent]
                    if (siblings == null || siblings.none { it.path == d && it.isDir }) {
                        expandedDirs.remove(d)
                        browserChildren.remove(d)
                    }
                }
                browserCount = (browserChildren["."] ?: emptyList()).count { !it.isDir }
                browserLoading = false
            }
        }
    }

    fun createBrowserFolder(name: String, parentPath: String = ".") {
        val full = if (parentPath == "." || parentPath.isEmpty()) name else "$parentPath/$name"
        viewModelScope.launch(Dispatchers.IO) {
            getFileRepo().makeDir(full)
            withContext(Dispatchers.Main) {
                expandParents(full)
                refreshBrowser()
            }
        }
    }

    private fun expandParents(path: String) {
        val parts = path.split("/").filter { it.isNotEmpty() }
        var acc = ""
        for (i in 0 until parts.size - 1) {
            acc = if (acc.isEmpty()) parts[i] else acc + "/" + parts[i]
            expandedDirs[acc] = true
        }
    }

    private fun forgetPath(path: String) {
        modifiedPaths.keys.toList().filter { it == path || it.startsWith("$path/") }.forEach { modifiedPaths.remove(it) }
        expandedDirs.keys.toList().filter { it == path || it.startsWith("$path/") }.forEach { expandedDirs.remove(it) }
        editor.closeUnder(path)
    }

    fun createBrowserFile(path: String) {
        browserError = null
        viewModelScope.launch(Dispatchers.IO) {
            val res = getFileRepo().createFile(path)
            withContext(Dispatchers.Main) {
                if (res.startsWith("Succès")) {
                    expandParents(path)
                    refreshBrowser()
                    editor.open(path) { isEditorOpen = true }
                } else {
                    browserError = res
                }
            }
        }
    }

    fun deleteBrowserPath(path: String) {
        if (editor.hasDirtyUnder(path)) {
            browserError = "Enregistre ou ferme les onglets modifiés avant de supprimer."
            return
        }
        browserError = null
        viewModelScope.launch(Dispatchers.IO) {
            val res = getFileRepo().deletePath(path)
            withContext(Dispatchers.Main) {
                if (res.startsWith("Succès")) {
                    forgetPath(path)
                    refreshBrowser()
                } else {
                    browserError = res
                }
            }
        }
    }

    fun renameBrowserPath(path: String, newName: String) {
        if (newName.isBlank() || newName.contains('/') || newName.contains("..")) {
            browserError = "Nom invalide."
            return
        }
        if (newName == path.substringAfterLast('/')) return
        if (editor.hasDirtyUnder(path)) {
            browserError = "Enregistre ou ferme les onglets modifiés avant de renommer."
            return
        }
        browserError = null
        viewModelScope.launch(Dispatchers.IO) {
            val res = getFileRepo().renamePath(path, newName)
            withContext(Dispatchers.Main) {
                if (res.startsWith("Succès")) {
                    forgetPath(path)
                    refreshBrowser()
                } else {
                    browserError = res
                }
            }
        }
    }

    fun openBrowserFile(path: String) {
        if (browserFileLoading) return
        browserFileLoading = true
        browserError = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val repository = getFileRepo()
                if (repository.isTextFile(path)) {
                    withContext(Dispatchers.Main) {
                        browserFileLoading = false
                        editor.open(path) { isEditorOpen = true }
                    }
                } else {
                    val uri = repository.fileUri(path)
                        ?: throw IllegalStateException("Fichier introuvable ou inaccessible.")
                    val mimeType = repository.mimeType(path)
                    withContext(Dispatchers.Main) {
                        try {
                            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, mimeType)
                                clipData = ClipData.newRawUri(path.substringAfterLast('/'), uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            val chooser = Intent.createChooser(viewIntent, "Ouvrir avec").apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            getApplication<Application>().startActivity(chooser)
                        } catch (error: Exception) {
                            browserError = "Aucune application ne peut ouvrir ce fichier : ${error.localizedMessage ?: path}"
                        } finally {
                            browserFileLoading = false
                        }
                    }
                }
            } catch (error: Exception) {
                withContext(Dispatchers.Main) {
                    browserFileLoading = false
                    browserError = "Impossible d'ouvrir $path : ${error.localizedMessage ?: "erreur inconnue"}"
                }
            }
        }
    }

    fun closeBrowserFilePreview() {
        browserFilePreview = null
    }

    // ── Envoi & boucle agent ────────────────────────────────────────────────
    private var cachedFileRepoUri: String? = null
    private var cachedFileRepo: FileRepository? = null

    @Synchronized
    private fun getFileRepo(): FileRepository {
        val uri = workingFolderUriString
        if (cachedFileRepo == null || cachedFileRepoUri != uri) {
            cachedFileRepo = FileRepository(getApplication(), uri)
            cachedFileRepoUri = uri
        }
        return cachedFileRepo!!
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return
        if (isLoading) return
        if (apiKeyInput.isBlank()) {
            isSettingsOpen = true
            statusMessage = "Veuillez entrer une clé API dans les réglages."
            return
        }

        val newUserMessage = ChatMessage("user", userText)
        _messages.value = _messages.value + newUserMessage

        isLoading = true
        agentJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                runAgentLoop()
            } finally {
                withContext(NonCancellable + Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    private suspend fun runAgentLoop() {
        isLoading = true
        statusMessage = "Réflexion de l'agent…"
        isCode1305Error = false
        showFallbackModelButton = false

        val fileRepo = getFileRepo()
        val systemPrompt = "Tu es un agent de code qui travaille dans un dossier de projet. Utilise les outils pour explorer, lire, créer des dossiers, écrire et supprimer des fichiers (delete_file ; pour vider un fichier, write_file avec contenu vide). Si tu ne connais pas la structure, commence par list_dir. Lis un fichier avant de le modifier. Écris toujours le contenu COMPLET d'un fichier. Les chemins sont relatifs au dossier de travail. Si la demande est floue ou qu'un choix important manque (sujet, public, style, durée, technologie…), appelle d'abord ask_user avec 1 à 4 questions courtes à choix (2 à 4 options chacune) au lieu de deviner, puis continue avec les réponses sans reposer les mêmes questions. Réponds en français, de façon courte."

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
                    isKeyInvalid = result.errorMessage.contains("401")
                    isNetworkError = !isKeyInvalid && (
                        result.errorMessage.contains("réseau", ignoreCase = true) ||
                            result.errorMessage.contains("resolve host", ignoreCase = true)
                        )
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
                                statusMessage = "Exécution de $logLine…"
                            }

                            val toolResult = executeToolWithGate(fileRepo, tc)

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

    /** Porte d'écriture : Auto / fichier toujours autorisé / confirmation manuelle. */
    private suspend fun executeToolWithGate(fileRepo: FileRepository, tc: AiRepository.ToolCallData): String {
        if (tc.name == "write_file") {
            val parsedArgs = try {
                Json.parseToJsonElement(tc.arguments).jsonObject
            } catch (e: Exception) {
                JsonObject(emptyMap())
            }
            val path = parsedArgs["path"]?.jsonPrimitive?.content ?: ""
            val fileContent = parsedArgs["content"]?.jsonPrimitive?.content ?: ""

            val alwaysAllowed = settingsManager.alwaysAllowedPaths
            val needsConfirmation = !autoModeInput && path !in alwaysAllowed

            val toolResult = if (!needsConfirmation) {
                if (autoModeInput) {
                    withContext(Dispatchers.Main) { statusMessage = "Mode Auto : écriture de $path…" }
                }
                fileRepo.writeFile(path, fileContent)
            } else {
                val userAllowed = waitForWriteConfirmation(path, fileContent, tc.id, fileRepo)
                if (userAllowed) fileRepo.writeFile(path, fileContent)
                else "Refusé par l'utilisateur: l'écriture de $path a été annulée."
            }

            if (toolResult.startsWith("Succès")) {
                withContext(Dispatchers.Main) {
                    modifiedPaths[path] = true
                    editor.onExternalWrite(path, fileContent)
                }
            }
            return toolResult
        }
        if (tc.name == "delete_file") {
            val args = try {
                Json.parseToJsonElement(tc.arguments).jsonObject
            } catch (e: Exception) {
                JsonObject(emptyMap())
            }
            val path = args["path"]?.jsonPrimitive?.content?.trim().orEmpty()
            if (path.isEmpty()) return "Erreur: chemin manquant pour delete_file."
            val allowed = autoModeInput || waitForDeleteConfirmation(path, tc.id)
            if (!allowed) return "Refusé par l'utilisateur: la suppression de $path a été annulée."
            val res = fileRepo.deletePath(path)
            if (res.startsWith("Succès")) {
                withContext(Dispatchers.Main) {
                    forgetPath(path)
                    refreshBrowser()
                }
            }
            return res
        }
        if (tc.name == "ask_user") {
            val questions = parseAskQuestions(tc.arguments)
            if (questions.isEmpty()) {
                return "Erreur: ask_user demande au moins une question avec des options."
            }
            val answers = waitForAsk(tc.id, questions)
            return answers
                ?: "L'utilisateur a ignoré les questions : continue avec des choix raisonnables par défaut."
        }
        return AgentTools.executeTool(fileRepo, tc.name, tc.arguments)
    }

    private suspend fun waitForDeleteConfirmation(path: String, toolCallId: String): Boolean {
        return suspendCancellableCoroutine { continuation ->
            statusMessage = "En attente de ta confirmation…"
            pendingDelete = PendingDelete(path, toolCallId)
            onDeleteConfirmed = { allowed ->
                pendingDelete = null
                statusMessage = "Réflexion de l'agent…"
                continuation.resume(allowed, null)
            }
            continuation.invokeOnCancellation {
                pendingDelete = null
                onDeleteConfirmed = null
            }
        }
    }

    fun resolvePendingDelete(allowed: Boolean) {
        onDeleteConfirmed?.invoke(allowed)
        onDeleteConfirmed = null
        pendingDelete = null
    }

    private suspend fun waitForAsk(toolCallId: String, questions: List<AskQuestion>): String? {
        return suspendCancellableCoroutine { continuation ->
            statusMessage = "En attente de ta réponse…"
            pendingAsk = PendingAsk(toolCallId, questions)
            onAskAnswered = { answers ->
                pendingAsk = null
                statusMessage = "Réflexion de l'agent…"
                continuation.resume(answers, null)
            }
            continuation.invokeOnCancellation {
                pendingAsk = null
                onAskAnswered = null
            }
        }
    }

    /** Appelé par la carte de questions : réponses formatées, ou null si l'utilisateur ignore. */
    fun resolvePendingAsk(answers: String?) {
        onAskAnswered?.invoke(answers)
        onAskAnswered = null
        pendingAsk = null
    }

    private suspend fun waitForWriteConfirmation(
        path: String,
        content: String,
        toolCallId: String,
        fileRepo: FileRepository
    ): Boolean {
        // Contenu existant pour l'aperçu du diff
        val existing = fileRepo.readFile(path)
        val oldContent: String? =
            if (existing.startsWith("Erreur")) null else existing

        val diff = DiffUtil.diff(oldContent, content)

        return suspendCancellableCoroutine { continuation ->
            pendingWrite = PendingWrite(path, content, toolCallId, diff)
            onWriteConfirmed = { allowed ->
                pendingWrite = null
                continuation.resume(allowed, null)
            }
        }
    }

    fun resolvePendingWrite(allowed: Boolean, always: Boolean = false) {
        if (allowed && always) {
            pendingWrite?.let { pw ->
                settingsManager.alwaysAllowedPaths = settingsManager.alwaysAllowedPaths + pw.path
            }
        }
        onWriteConfirmed?.invoke(allowed)
        onWriteConfirmed = null
        pendingWrite = null
    }
}
