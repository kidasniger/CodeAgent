package com.zagent.app.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zagent.app.agent.AgentNotifier
import com.zagent.app.agent.AgentService
import com.zagent.app.data.AgentTools
import com.zagent.app.data.AiRepository
import com.zagent.app.data.AskQuestion
import com.zagent.app.data.parseAskQuestions
import com.zagent.app.data.TodoItem
import com.zagent.app.data.parseTodos
import com.zagent.app.data.DiffUtil
import com.zagent.app.data.FileRepository
import com.zagent.app.data.SettingsManager
import com.zagent.app.ui.editor.ConsoleState
import com.zagent.app.ui.editor.EditorController
import com.zagent.app.ui.theme.ThemeMode
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
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

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
    // Chemin absolu du projet (dans le home du terminal). Les anciennes valeurs content:// sont ignorées.
    var workingFolderUriString by mutableStateOf(settingsManager.workingFolderUri?.takeIf { it.startsWith("/") })
    var smartModeInput by mutableStateOf(settingsManager.smartMode)
    var customInstructionsInput by mutableStateOf(settingsManager.customInstructions)

    fun setCustomInstructions(value: String) {
        customInstructionsInput = value
        settingsManager.customInstructions = value
    }

    fun setSmartMode(value: Boolean) {
        smartModeInput = value
        settingsManager.smartMode = value
    }

    var showKeyVisible by mutableStateOf(false)
    var isSettingsOpen by mutableStateOf(false)

    // ── Apparence & flux de démarrage ───────────────────────────────────────
    var themeMode by mutableStateOf(settingsManager.themeMode)
    var onboardingDone by mutableStateOf(settingsManager.onboardingDone)

    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        settingsManager.themeMode = mode
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

    /** Commande demandée par l'agent (outil run_command), en attente de confirmation (hors mode Auto). */
    data class PendingCommand(
        val command: String,
        val toolCallId: String,
        val note: String? = null,
        val changes: GitChanges? = null,
        val footer: String? = null
    )

    var pendingCommand: PendingCommand? by mutableStateOf(null)
    private var onCommandConfirmed: ((Boolean) -> Unit)? = null

    /** Commande en cours d'exécution et sa sortie, affichées en direct dans le chat. */
    var liveCommand by mutableStateOf<String?>(null)
    var liveCommandId by mutableStateOf<String?>(null)
    var liveOutput by mutableStateOf("")
    var liveStartedAt by mutableStateOf(0L)

    /** Texte de l'IA en cours d'écriture (affiché mot à mot avant d'être ajouté à la conversation). */
    var liveText by mutableStateOf("")

    /** Liste de tâches de l'agent (outil update_todos), affichée en haut du chat. */
    var todos by mutableStateOf<List<TodoItem>>(emptyList())

    /** Résumé de la demande en cours : fichiers touchés et commandes lancées. */
    val summaryFiles = mutableStateListOf<String>()
    val summaryCommands = mutableStateListOf<String>()

    /** Questions à choix posées par l'agent (outil ask_user), en attente de réponse. */
    data class PendingAsk(val toolCallId: String, val questions: List<AskQuestion>)

    var pendingAsk: PendingAsk? by mutableStateOf(null)
    private var onAskAnswered: ((String?) -> Unit)? = null
    private var agentJob: Job? = null

    /** État d'avant une écriture de l'agent (oldContent = null : le fichier n'existait pas). */
    data class UndoEntry(val path: String, val oldContent: String?)

    /** Changements faits par l'agent pendant la dernière demande, annulables d'un tap. */
    val agentUndo = mutableStateListOf<UndoEntry>()

    /** Console JS partagée entre l'aperçu et le bouton de correction. */
    val console = ConsoleState()
    var previewRequest by mutableStateOf<String?>(null)
    var previewTick by mutableStateOf(0)

    /** Dernière page HTML créée ou modifiée par l'agent (pour le bouton d'aperçu du chat). */
    val lastAgentHtml: String?
        get() = agentUndo.lastOrNull {
            val e = it.path.substringAfterLast('.', "").lowercase()
            e == "html" || e == "htm"
        }?.path

    /** Ouverture de l'aperçu, uniquement sur action de l'utilisateur. */
    fun openPreview(path: String) {
        console.clear()
        editor.open(path) { isEditorOpen = true }
        isEditorOpen = true
        previewTick++
        previewRequest = path
    }

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


    // ── Limite d'étapes ─────────────────────────────────────────────────────
    var maxStepsInput by mutableStateOf(settingsManager.maxSteps)

    fun setMaxSteps(value: Int) {
        val n = value.coerceIn(5, 100)
        maxStepsInput = n
        settingsManager.maxSteps = n
    }

    /** Vrai quand l'agent s'est arrêté sur la limite d'étapes : le chat propose « Continuer ». */
    var stepLimitReached by mutableStateOf(false)

    /** Relance l'agent sur l'historique courant, avec un nouveau budget d'étapes. */
    fun continueAgent() {
        if (isLoading || !stepLimitReached) return
        stepLimitReached = false
        retryLast()
    }

    // ── Modèle par conversation, tokens et coût ─────────────────────────────
    /** Modèle choisi pour cette conversation (null = réglage global). */
    var chatModel by mutableStateOf<String?>(null)
    var isModelDialogOpen by mutableStateOf(false)

    val effectiveModel: String
        get() = chatModel?.takeIf { it.isNotBlank() } ?: modelInput

    fun selectChatModel(model: String?) {
        chatModel = model?.trim()?.ifEmpty { null }
    }

    var promptTokens by mutableStateOf(0)
    var completionTokens by mutableStateOf(0)

    /** Vrai si une partie du compteur est une estimation (le fournisseur n'a pas renvoyé l'usage). */
    var tokensEstimated by mutableStateOf(false)

    var priceInInput by mutableStateOf(settingsManager.priceInPerM)
    var priceOutInput by mutableStateOf(settingsManager.priceOutPerM)

    val totalTokens: Int get() = promptTokens + completionTokens

    /** Coût de la conversation, ou null si aucun prix n'est renseigné dans les réglages. */
    val estimatedCost: Double?
        get() = if (priceInInput <= 0f && priceOutInput <= 0f) null
        else promptTokens.toDouble() * priceInInput / 1_000_000.0 +
            completionTokens.toDouble() * priceOutInput / 1_000_000.0

    fun setPrices(priceIn: Float, priceOut: Float) {
        priceInInput = priceIn.coerceAtLeast(0f)
        priceOutInput = priceOut.coerceAtLeast(0f)
        settingsManager.priceInPerM = priceInInput
        settingsManager.priceOutPerM = priceOutInput
    }

    // ── Git visuel ──────────────────────────────────────────────────────────
    data class GitChanges(val status: String, val diff: String, val error: String? = null)

    var gitSheetOpen by mutableStateOf(false)
    var gitChanges by mutableStateOf<GitChanges?>(null)
    var gitLoading by mutableStateOf(false)

    // § remplace le signe dollar (évite les modèles de chaînes Kotlin)
    private val gitScript = """
{ git rev-parse --is-inside-work-tree >/dev/null 2>&1 || { echo NOREPO; exit 0; }
git status --short
echo '@@@DIFF@@@'
if git rev-parse --verify HEAD >/dev/null 2>&1; then git diff HEAD --no-color --no-ext-diff; else git diff --cached --no-color --no-ext-diff; fi
git ls-files --others --exclude-standard | head -20 | while IFS= read -r f; do
  if [ -f "§f" ] && grep -Iq . "§f" 2>/dev/null; then
    echo "diff --git a/§f b/§f"; echo "new file"; echo "--- /dev/null"; echo "+++ b/§f"
    echo "@@ -0,0 +1 @@"; head -n 200 "§f" | sed 's/^/+/'
  fi
done; } 2>&1 | head -c 50000
""".replace('§', '$')

    /** Changements du dépôt : état, diff des fichiers suivis et contenu des nouveaux fichiers. */
    suspend fun readGitChanges(): GitChanges {
        val res = terminal.runCommand(gitScript, workingFolderUriString, 20, silent = true, outLimit = 60000) { }
        val first = res.substringBefore('\n')
        val body = res.substringAfter('\n', "")
        if (first.startsWith("Erreur")) return GitChanges("", "", res.take(300))
        if (body.trim() == "NOREPO") return GitChanges("", "", "Ce projet n'est pas un dépôt Git.")
        val status = body.substringBefore("@@@DIFF@@@").trim()
        val diff = body.substringAfter("@@@DIFF@@@", "").trim('\n')
        return GitChanges(status, diff)
    }

    fun openGitChanges() {
        gitSheetOpen = true
        refreshGitChanges()
    }

    fun refreshGitChanges() {
        if (gitLoading) return
        gitLoading = true
        viewModelScope.launch(Dispatchers.IO) {
            val r = readGitChanges()
            withContext(Dispatchers.Main) {
                gitChanges = r
                gitLoading = false
            }
        }
    }

    // ── Serveur de développement ────────────────────────────────────────────
    /** Ouverture de l'aperçu sur l'adresse locale du serveur, uniquement sur action de l'utilisateur. */
    fun openDevPreview() {
        val url = terminal.devUrl ?: return
        console.clear()
        isEditorOpen = true
        previewTick++
        previewRequest = url
    }

    fun stopDevServerByUser() {
        terminal.stopDevServer()
    }

    // ── Import / export de projets ──────────────────────────────────────────
    var transferStatus by mutableStateOf<String?>(null)
    var transferBusy by mutableStateOf(false)

    private fun sanitizeProjectName(raw: String): String {
        val cleaned = raw.trim().replace(Regex("[^A-Za-z0-9._-]+"), "-").trim('-', '.', '_')
        return (cleaned.ifEmpty { "projet-importe" }).take(50)
    }

    private fun uniqueProjectName(base: String): String {
        var name = base
        var i = 2
        while (File(projectsRoot(), name).exists()) {
            name = base.take(44) + "-" + i
            i++
        }
        return name
    }

    private fun displayNameOf(uri: Uri): String? = try {
        getApplication<Application>().contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        }
    } catch (e: Exception) {
        null
    }

    /** Importe un zip du téléphone dans un nouveau projet (~/projects/<nom>). */
    fun importZip(uri: Uri) {
        if (transferBusy) return
        transferBusy = true
        transferStatus = "Import du zip en cours…"
        viewModelScope.launch(Dispatchers.IO) {
            var dest: File? = null
            val msg = try {
                val label = (displayNameOf(uri) ?: "projet-importe").removeSuffix(".zip").removeSuffix(".ZIP")
                val name = uniqueProjectName(sanitizeProjectName(label))
                val d = File(projectsRoot(), name)
                dest = d
                d.mkdirs()
                val count = unzipTo(uri, d)
                withContext(Dispatchers.Main) { selectProject(name) }
                "Importé : $name ($count fichiers)"
            } catch (e: Exception) {
                dest?.deleteRecursively()
                "Erreur d'import : " + (e.localizedMessage ?: e.javaClass.simpleName)
            }
            withContext(Dispatchers.Main) {
                transferStatus = msg
                transferBusy = false
            }
        }
    }

    private fun unzipTo(uri: Uri, dest: File): Int {
        val resolver = getApplication<Application>().contentResolver
        val names = ArrayList<String>()
        (resolver.openInputStream(uri) ?: throw IllegalStateException("Fichier illisible.")).use { ins ->
            ZipInputStream(ins.buffered()).use { z ->
                var e = z.nextEntry
                while (e != null) {
                    names.add(e.name.replace('\\', '/'))
                    e = z.nextEntry
                }
            }
        }
        if (names.isEmpty()) throw IllegalStateException("Le zip est vide ou invalide.")
        val real = names.filter { it.isNotEmpty() && !it.startsWith("__MACOSX/") }
        val firstSegments = real.map { it.substringBefore('/') }.toSet()
        // Un seul dossier racine dans le zip : on le retire pour que les fichiers arrivent à la racine du projet.
        val strip = if (firstSegments.size == 1 && real.all { it.contains('/') }) firstSegments.first() + "/" else ""
        val destCanon = dest.canonicalPath
        var count = 0
        (resolver.openInputStream(uri) ?: throw IllegalStateException("Fichier illisible.")).use { ins ->
            ZipInputStream(ins.buffered()).use { z ->
                var e = z.nextEntry
                while (e != null) {
                    var n = e.name.replace('\\', '/')
                    if (!n.startsWith("__MACOSX/")) {
                        if (strip.isNotEmpty()) n = n.removePrefix(strip)
                        if (n.isNotEmpty()) {
                            val out = File(dest, n)
                            val canon = out.canonicalPath
                            // protection contre les chemins « ../ » (zip slip)
                            if (canon == destCanon || canon.startsWith(destCanon + File.separator)) {
                                if (e.isDirectory) {
                                    out.mkdirs()
                                } else {
                                    out.parentFile?.mkdirs()
                                    FileOutputStream(out).use { o -> z.copyTo(o) }
                                    count++
                                }
                            }
                        }
                    }
                    z.closeEntry()
                    e = z.nextEntry
                }
            }
        }
        return count
    }

    /** Importe un dossier du téléphone (sélecteur Android) dans un nouveau projet. node_modules est ignoré. */
    fun importFolder(uri: Uri) {
        if (transferBusy) return
        transferBusy = true
        transferStatus = "Import du dossier en cours…"
        viewModelScope.launch(Dispatchers.IO) {
            var dest: File? = null
            val msg = try {
                val app = getApplication<Application>()
                val root = DocumentFile.fromTreeUri(app, uri) ?: throw IllegalStateException("Dossier inaccessible.")
                val name = uniqueProjectName(sanitizeProjectName(root.name ?: "dossier"))
                val d = File(projectsRoot(), name)
                dest = d
                d.mkdirs()
                val count = copyTree(root, d)
                withContext(Dispatchers.Main) { selectProject(name) }
                "Importé : $name ($count fichiers)"
            } catch (e: Exception) {
                dest?.deleteRecursively()
                "Erreur d'import : " + (e.localizedMessage ?: e.javaClass.simpleName)
            }
            withContext(Dispatchers.Main) {
                transferStatus = msg
                transferBusy = false
            }
        }
    }

    private fun copyTree(src: DocumentFile, dest: File): Int {
        val resolver = getApplication<Application>().contentResolver
        var count = 0
        for (child in src.listFiles()) {
            val childName = child.name ?: continue
            if (child.isDirectory) {
                if (childName == "node_modules") continue
                val sub = File(dest, childName)
                sub.mkdirs()
                count += copyTree(child, sub)
            } else if (child.isFile) {
                val out = File(dest, childName)
                resolver.openInputStream(child.uri)?.use { ins ->
                    FileOutputStream(out).use { o -> ins.copyTo(o) }
                    count++
                }
            }
        }
        return count
    }

    /** Exporte le projet [project] en zip vers [uri] (sélecteur Android). node_modules est ignoré. */
    fun exportProject(project: String, uri: Uri) {
        if (transferBusy) return
        transferBusy = true
        transferStatus = "Export de $project en cours…"
        viewModelScope.launch(Dispatchers.IO) {
            val msg = try {
                val dir = File(projectsRoot(), project)
                if (!dir.isDirectory) throw IllegalStateException("Projet introuvable.")
                var count = 0
                (getApplication<Application>().contentResolver.openOutputStream(uri)
                    ?: throw IllegalStateException("Destination inaccessible.")).use { os ->
                    ZipOutputStream(os.buffered()).use { z ->
                        count = zipDir(dir, project + "/", z)
                    }
                }
                "Exporté : $project.zip ($count fichiers)"
            } catch (e: Exception) {
                "Erreur d'export : " + (e.localizedMessage ?: e.javaClass.simpleName)
            }
            withContext(Dispatchers.Main) {
                transferStatus = msg
                transferBusy = false
            }
        }
    }

    private fun zipDir(dir: File, prefix: String, z: ZipOutputStream): Int {
        var n = 0
        for (f in (dir.listFiles() ?: emptyArray()).sortedBy { it.name }) {
            if (f.name == "node_modules") continue
            if (java.nio.file.Files.isSymbolicLink(f.toPath())) continue
            val entry = prefix + f.name
            if (f.isDirectory) {
                z.putNextEntry(ZipEntry("$entry/"))
                z.closeEntry()
                n += zipDir(f, "$entry/", z)
            } else if (f.isFile) {
                z.putNextEntry(ZipEntry(entry))
                FileInputStream(f).use { it.copyTo(z) }
                z.closeEntry()
                n++
            }
        }
        return n
    }

    override fun onCleared() {
        terminal.onDevServerChanged = null
        terminal.stopDevServer()
        aiRepository.cancelActive()
        super.onCleared()
    }

    // ── Projets (dossiers réels dans le home du terminal) ───────────────────
    var isProjectPickerOpen by mutableStateOf(false)

    /** Dossier qui contient tous les projets : ~/projects */
    private fun projectsRoot(): java.io.File =
        java.io.File(getApplication<Application>().filesDir, "home/projects")

    fun listProjects(): List<String> =
        (projectsRoot().listFiles() ?: emptyArray())
            .filter { it.isDirectory && !it.name.startsWith(".") }
            .map { it.name }
            .sortedBy { it.lowercase() }

    /** Crée le projet [name] et le sélectionne. Retourne un message d'erreur, ou null si OK. */
    fun createProject(name: String): String? {
        val n = name.trim()
        if (!Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,49}$").matches(n)) {
            return "Nom invalide : lettres, chiffres, point, tiret et underscore (50 max)."
        }
        val dir = java.io.File(projectsRoot(), n)
        if (dir.exists() && !dir.isDirectory) return "Un fichier porte déjà ce nom."
        if (!dir.isDirectory && !dir.mkdirs()) return "Impossible de créer le dossier."
        selectProject(n)
        return null
    }

    fun selectProject(name: String) {
        val dir = java.io.File(projectsRoot(), name)
        if (!dir.isDirectory) return
        setWorkingFolderPath(dir.absolutePath)
    }

    private fun setWorkingFolderPath(path: String) {
        workingFolderUriString = path
        settingsManager.workingFolderUri = path
        terminal.projectDir = path
        browserChildren.clear()
        expandedDirs.clear()
        editor.clear()
        isEditorOpen = false
        refreshFolderLabel()
    }

    fun clearConversation() {
        agentJob?.cancel()
        agentJob = null
        aiRepository.cancelActive()
        onWriteConfirmed = null
        pendingWrite = null
        onAskAnswered = null
        pendingAsk = null
        onDeleteConfirmed = null
        pendingDelete = null
        onCommandConfirmed = null
        pendingCommand = null
        liveCommand = null
        liveText = ""
        todos = emptyList()
        summaryFiles.clear()
        summaryCommands.clear()
        agentUndo.clear()
        isLoading = false
        _messages.value = emptyList()
        statusMessage = ""
        isCode1305Error = false
        showFallbackModelButton = false
        isKeyInvalid = false
        isNetworkError = false
        autoRetryCount = 0
        stepLimitReached = false
        chatModel = null
        promptTokens = 0
        completionTokens = 0
        tokensEstimated = false
    }

    fun switchToOtherModel() {
        val next = if (effectiveModel == "glm-4.5-flash") "glm-4.7-flash" else "glm-4.5-flash"
        if (chatModel != null) {
            chatModel = next
        } else {
            modelInput = next
            settingsManager.model = next
        }
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
        AgentService.start(getApplication())
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
                AgentService.stop(getApplication())
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

    // ── Terminal embarqué ───────────────────────────────────────────────────
    var isTerminalOpen by mutableStateOf(false)
    val terminal = com.zagent.app.terminal.TerminalController(application, viewModelScope)

    init {
        terminal.projectDir = workingFolderUriString
        terminal.onDevServerChanged = { running ->
            AgentService.devServerActive = running
            if (running) {
                AgentService.start(getApplication(), "Serveur de développement actif")
            } else if (!isLoading) {
                AgentService.stop(getApplication())
            }
        }
    }
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

        stepLimitReached = false
        agentUndo.clear()
        todos = emptyList()
        summaryFiles.clear()
        summaryCommands.clear()
        val newUserMessage = ChatMessage("user", userText)
        _messages.value = _messages.value + newUserMessage

        isLoading = true
        AgentService.start(getApplication())
        agentJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                runAgentLoop()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Throwable) {
                // Une erreur inattendue dans la boucle de l'agent ne doit jamais fermer l'appli.
                val msg = e.localizedMessage ?: e.javaClass.simpleName
                terminal.logAgent("Erreur interne : " + msg.take(160))
                withContext(NonCancellable + Dispatchers.Main) {
                    liveText = ""
                    liveCommand = null
                    liveCommandId = null
                    statusMessage = "Erreur interne de l'agent : " + msg.take(160)
                }
            } finally {
                AgentService.stop(getApplication())
                withContext(NonCancellable + Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    private suspend fun runAgentLoop() {
        isLoading = true
        statusMessage = "Envoi à l'IA…"
        isCode1305Error = false
        showFallbackModelButton = false

        val fileRepo = getFileRepo()
        _messages.value.lastOrNull { it.role == "user" }?.content?.let { ask ->
            terminal.logAgent("\n== Demande : " + ask.trim().lineSequence().first().take(120))
        }
        val basePrompt = "Tu es un agent de code qui travaille dans un dossier de projet. Utilise les outils pour explorer, lire, créer des dossiers, écrire et supprimer des fichiers (delete_file ; pour vider un fichier, write_file avec contenu vide). Si tu ne connais pas la structure, commence par list_dir ; utilise search_in_files pour retrouver du code. Lis un fichier avant de le modifier. Pour modifier un fichier existant, utilise edit_file (un passage exact, unique) ; si edit_file échoue, relis le fichier avec read_file. N'utilise write_file (contenu COMPLET) que pour créer un fichier ou le réécrire entièrement. Les chemins sont relatifs au dossier de travail. Tu peux exécuter n'importe quelle commande shell dans le dossier du projet avec run_command (git, npm, node, python, pip, clang, pkg install -y, etc.) : utilise-le pour installer, compiler, lancer des tests, vérifier ton travail et faire du git ; lis la sortie et corrige les erreurs. Les commandes ne peuvent pas être interactives (pas d'éditeur, pas de saisie) : ajoute les options non interactives (-y, --yes). Si la demande est floue ou qu'un choix important manque (sujet, public, style, durée, technologie…), appelle d'abord ask_user avec 1 à 4 questions courtes à choix (2 à 4 options chacune) au lieu de deviner, puis continue avec les réponses sans reposer les mêmes questions. Avant chaque appel d'outil, écris une seule phrase courte qui annonce ce que tu vas faire (ex : « Je lance git push »). Pour toute demande de 3 étapes ou plus, appelle d'abord update_todos avec la liste des tâches, puis remets-la à jour (liste complète) quand une tâche est terminée. Réponds en français, de façon courte."

        val rules = fileRepo.readFile("AGENT.md")
        val projectRules = if (rules.startsWith("Erreur") || rules.isBlank()) ""
            else "\n\nRègles du projet (fichier AGENT.md), à respecter :\n" + rules.take(6000)
        val custom = customInstructionsInput.trim().let {
            if (it.isEmpty()) "" else "\n\nInstructions personnalisées de l'utilisateur :\n" + it.take(4000)
        }
        val systemPrompt = basePrompt + projectRules + custom

        var stepCount = 0
        val maxSteps = maxStepsInput
        var finished = false

        while (stepCount < maxSteps) {
            stepCount++

            val jsonMessages = buildJsonArray {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                val history = _messages.value
                val compactBefore = history.size - 10
                for ((idx, msg) in history.withIndex()) {
                    val sentContent = if (idx < compactBefore && msg.role == "tool") compactToolOutput(msg.content) else msg.content
                    add(buildJsonObject {
                        put("role", msg.role)
                        if (sentContent != null) put("content", sentContent) else put("content", JsonNull)
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

            withContext(Dispatchers.Main) {
                liveText = ""
                statusMessage = "Envoi à l'IA…"
            }
            val liveBuf = StringBuilder()
            var lastPublish = 0L
            val result = aiRepository.callChatCompletions(
                baseUrl = baseUrlInput,
                apiKey = apiKeyInput,
                model = effectiveModel,
                messages = jsonMessages,
                onRetryUpdate = { retryMsg ->
                    liveBuf.setLength(0)
                    liveText = ""
                    statusMessage = retryMsg
                },
                onDelta = { piece ->
                    liveBuf.append(piece)
                    val nowMs = System.currentTimeMillis()
                    if (liveText.isEmpty() || nowMs - lastPublish >= 120) {
                        lastPublish = nowMs
                        if (liveText.isEmpty()) statusMessage = "L'IA écrit…"
                        liveText = liveBuf.toString()
                    }
                }
            )

            when (result) {
                is AiRepository.ApiResponse.Error -> {
                    alertUser("L'agent s'est arrêté", result.errorMessage.take(120))
                    liveText = ""
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
                    finished = true
                    break
                }
                is AiRepository.ApiResponse.Success -> {
                    val content = result.messageContent
                    val toolCalls = result.toolCalls

                    // Tokens : valeurs du fournisseur si présentes, sinon estimation (environ 4 caractères par token).
                    val estIn = (jsonMessages.toString().length + AgentTools.toolsJson.toString().length) / 4
                    val estOut = ((content?.length ?: 0) + (toolCalls?.sumOf { it.name.length + it.arguments.length } ?: 0)) / 4
                    val usedIn = result.promptTokens
                    val usedOut = result.completionTokens
                    promptTokens += usedIn ?: estIn
                    completionTokens += usedOut ?: estOut
                    if (usedIn == null || usedOut == null) tokensEstimated = true

                    if (toolCalls.isNullOrEmpty()) {
                        val assistantMsg = ChatMessage("assistant", content ?: "")
                        alertUser("L'agent a terminé", (content ?: "Tâche terminée.").trim().take(140))
                        withContext(Dispatchers.Main) {
                            _messages.value = _messages.value + assistantMsg
                            liveText = ""
                            isLoading = false
                            statusMessage = ""
                        }
                        finished = true
                        break
                    } else {
                        val assistantMsg = ChatMessage("assistant", content, toolCalls = toolCalls)
                        withContext(Dispatchers.Main) {
                            _messages.value = _messages.value + assistantMsg
                            liveText = ""
                        }

                        for (tc in toolCalls) {
                            val label = describeTool(tc.name, tc.arguments)
                            withContext(Dispatchers.Main) {
                                statusMessage = label
                            }

                            val logIt = tc.name != "run_command" && tc.name != "ask_user"
                            if (logIt) terminal.logAgent("> " + label.removeSuffix("…"))
                            val toolResult = executeToolWithGate(fileRepo, tc)
                            if (logIt) {
                                terminal.logAgent("  " + toolResult.lineSequence().firstOrNull().orEmpty().take(160))
                            }

                            val toolMsg = ChatMessage(
                                role = "tool",
                                content = toolResult,
                                toolCallId = tc.id,
                                name = tc.name,
                                toolLog = tc.name
                            )
                            withContext(Dispatchers.Main) {
                                _messages.value = _messages.value + toolMsg
                            }
                        }
                    }
                }
            }
        }

        if (!finished) {
            alertUser("L'agent s'est arrêté", "Limite de $maxSteps étapes atteinte.")
            withContext(Dispatchers.Main) {
                isLoading = false
                stepLimitReached = true
                statusMessage = "Limite de $maxSteps étapes atteinte pour cette demande."
            }
        }
    }

    /** Raccourcit une ancienne sortie d'outil avant de la renvoyer à l'IA (contexte plus léger, requêtes plus rapides). */
    private fun compactToolOutput(content: String?): String? {
        if (content == null || content.length <= 3000) return content
        return content.take(1500) + "\n[... sortie ancienne raccourcie ...]\n" + content.takeLast(1000)
    }

    /** Notification Android si l'appli n'est pas à l'écran. */
    private fun alertUser(title: String, text: String) {
        AgentNotifier.alert(getApplication(), title, text)
    }

    /** Renvoie à l'agent une commande en erreur pour qu'il la corrige. */
    fun fixCommandWithAi(command: String, result: String) {
        val out = result.substringAfter('\n', "").trim().takeLast(1500)
        sendMessage(
            "La commande `" + command + "` a échoué.\n\n```\n" + out +
                "\n```\nAnalyse l'erreur, corrige le problème, puis relance la commande."
        )
    }

    /** Arrête la commande en cours ; l'agent reçoit « arrêtée par l'utilisateur » et continue. */
    fun stopLiveCommand() {
        terminal.stopCurrentCommand()
    }

    /** Ouvre l'écran Terminal et place le shell dans le dossier du projet actif. */
    fun openTerminalInProject() {
        isTerminalOpen = true
        terminal.cdToProject()
    }

    /** Statut précis affiché pendant qu'un outil s'exécute. */
    private fun describeTool(name: String, argumentsJson: String): String {
        val o = try { Json.parseToJsonElement(argumentsJson) as? JsonObject } catch (e: Exception) { null }
        fun arg(k: String): String = (o?.get(k) as? JsonPrimitive)?.content?.trim().orEmpty()
        return when (name) {
            "list_dir" -> "Lecture du dossier " + arg("path").ifEmpty { "." } + "…"
            "read_file" -> "Lecture de " + arg("path") + "…"
            "make_dir" -> "Création du dossier " + arg("path") + "…"
            "write_file" -> "Écriture de " + arg("path") + "…"
            "edit_file" -> "Modification de " + arg("path") + "…"
            "delete_file" -> "Suppression de " + arg("path") + "…"
            "search_in_files" -> "Recherche de « " + arg("query").take(40) + " »…"
            "run_command" -> "Commande : " + arg("command").take(60) + "…"
            "update_todos" -> "Mise à jour des tâches…"
            "start_dev_server" -> "Démarrage du serveur de développement…"
            "stop_dev_server" -> "Arrêt du serveur de développement…"
            "ask_user" -> "En attente de ta réponse…"
            else -> "Exécution de $name…"
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
            return applyWrite(fileRepo, tc.id, path, fileContent, isEdit = false)
        }
        if (tc.name == "edit_file") {
            val args = try {
                Json.parseToJsonElement(tc.arguments).jsonObject
            } catch (e: Exception) {
                JsonObject(emptyMap())
            }
            val path = args["path"]?.jsonPrimitive?.content?.trim().orEmpty()
            val oldRaw = args["old_string"]?.jsonPrimitive?.content ?: ""
            val newRaw = args["new_string"]?.jsonPrimitive?.content ?: ""
            if (path.isEmpty()) return "Erreur: chemin manquant pour edit_file."
            if (oldRaw.isEmpty()) return "Erreur: old_string est vide."
            val cur = fileRepo.readFileForEdit(path).getOrElse { return "Erreur: " + (it.message ?: "lecture impossible") }
            val crlf = cur.contains("\r\n")
            val text = cur.replace("\r\n", "\n")
            val oldS = oldRaw.replace("\r\n", "\n")
            val newS = newRaw.replace("\r\n", "\n")
            var count = 0
            var first = -1
            var idx = text.indexOf(oldS)
            while (idx >= 0) {
                if (count == 0) first = idx
                count++
                idx = text.indexOf(oldS, idx + oldS.length)
            }
            if (count == 0) return "Erreur: old_string introuvable dans $path. Relis le fichier avec read_file et recopie le texte exactement."
            if (count > 1) return "Erreur: old_string apparaît $count fois dans $path. Ajoute du contexte autour pour le rendre unique."
            val updated = text.substring(0, first) + newS + text.substring(first + oldS.length)
            val out = if (crlf) updated.replace("\n", "\r\n") else updated
            return applyWrite(fileRepo, tc.id, path, out, isEdit = true)
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
                    summaryFiles.add("$path (supprimé)")
                    forgetPath(path)
                    refreshBrowser()
                }
            }
            return res
        }
        if (tc.name == "run_command") {
            val args = try {
                Json.parseToJsonElement(tc.arguments).jsonObject
            } catch (e: Exception) {
                JsonObject(emptyMap())
            }
            val command = args["command"]?.jsonPrimitive?.content?.trim().orEmpty()
            if (command.isEmpty()) return "Erreur: commande manquante pour run_command."
            val timeout = (args["timeout_seconds"]?.jsonPrimitive?.content?.toIntOrNull() ?: 120).coerceIn(1, 600)
            // Un git push est toujours confirmé (même en mode Auto), avec la branche et le dépôt visés.
            val isPush = pushRegex.containsMatchIn(command)
            val isCommit = commitRegex.containsMatchIn(command)
            // Une commande clairement destructrice est toujours confirmée, même en mode Auto.
            val isDangerous = destructiveRegexes.any { it.containsMatchIn(command) }
            val note = if (isPush) describePushTarget(command) else null
            // Un git commit est toujours confirmé, avec le diff de tous les changements.
            val changes = if (isCommit) readGitChanges() else null
            val footer = when {
                isCommit && !isPush -> "Un git commit est toujours confirmé, même en mode Auto : vérifie les changements ci-dessus."
                isDangerous -> "Cette commande peut détruire des données hors du projet : elle est toujours confirmée, même en mode Auto."
                else -> null
            }
            val allowed = (autoModeInput && !isPush && !isCommit && !isDangerous) ||
                waitForCommandConfirmation(command, tc.id, note, changes, footer)
            if (!allowed) return "Refusé par l'utilisateur: la commande a été annulée."
            withContext(Dispatchers.Main) {
                liveOutput = ""
                liveStartedAt = System.currentTimeMillis()
                summaryCommands.add(command.take(100))
                liveCommandId = tc.id
                liveCommand = command
                statusMessage = "Commande : " + command.take(60) + "…"
            }
            try {
                return terminal.runCommand(command, workingFolderUriString, timeout) { out -> liveOutput = out }
            } finally {
                withContext(NonCancellable + Dispatchers.Main) {
                    liveCommand = null
                    liveCommandId = null
                    refreshBrowser()
                }
            }
        }
        if (tc.name == "start_dev_server") {
            val args = try {
                Json.parseToJsonElement(tc.arguments).jsonObject
            } catch (e: Exception) {
                JsonObject(emptyMap())
            }
            val command = (args["command"] as? JsonPrimitive)?.content?.trim().orEmpty().ifEmpty { "npm run dev" }
            val allowed = autoModeInput || waitForCommandConfirmation(
                command, tc.id, null, null,
                "Le serveur reste actif en arrière-plan jusqu'à ce que tu l'arrêtes. Tu ouvres l'aperçu toi-même depuis le chat."
            )
            if (!allowed) return "Refusé par l'utilisateur: le lancement du serveur a été annulé."
            withContext(Dispatchers.Main) {
                summaryCommands.add(command.take(100))
                statusMessage = "Démarrage du serveur : " + command.take(50) + "…"
            }
            return terminal.startDevServer(command, workingFolderUriString)
        }
        if (tc.name == "stop_dev_server") {
            return if (terminal.stopDevServer()) "Serveur de développement arrêté." else "Aucun serveur de développement en cours."
        }
        if (tc.name == "update_todos") {
            val list = parseTodos(tc.arguments)
            withContext(Dispatchers.Main) { todos = list }
            return "Liste de tâches mise à jour (" + list.count { it.done } + "/" + list.size + " terminées)."
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
            alertUser("Confirmation requise", "Supprimer $path ?")
            pendingDelete = PendingDelete(path, toolCallId)
            onDeleteConfirmed = { allowed ->
                pendingDelete = null
                statusMessage = "Exécution en cours…"
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

    private val pushRegex = Regex("""(^|[;&|\s])git(\s+-C\s+\S+)?\s+push(\s|$)""")
    private val commitRegex = Regex("""(^|[;&|\s])git(\s+-C\s+\S+)?\s+commit(\s|$)""")

    /**
     * Dernier rempart contre les commandes catastrophiques : même en mode Auto, elles
     * passent par une confirmation manuelle. Liste volontairement non exhaustive, limitée
     * aux cibles évidentes (racine, home, formatage, périphériques, fork bomb) pour ne
     * pas casser les usages légitimes du projet.
     */
    private val destructiveRegexes = listOf(
        // rm récursif visant la racine (« / », « /* »), le home (« ~ », « $HOME ») seuls
        Regex("""\brm\b[^;&|]*(?:\s|\"|')?(?:/(?:\*|['"\s]|$)|~(?:['"\s]|$)|[$]HOME(?:['"\s]|$))"""),
        // formatage d'un système de fichiers
        Regex("""\bmkfs(?:\.\w+)?\b"""),
        // écriture brute sur un périphérique bloc
        Regex("""\bdd\b[^;&|]*\bof=/dev/"""),
        // chmod récursif ouvert sur la racine
        Regex("""\bchmod\s+-R\b[^;&|]*\s/\s*$"""),
        // fork bomb
        Regex(""":\s*\(\s*\)\s*\{\s*:\s*\|\s*:\s*&\s*\}\s*;\s*:""")
    )

    /** Branche courante et dépôt(s) distant(s) du projet, pour la confirmation d'un git push. */
    private suspend fun describePushTarget(command: String): String {
        val res = terminal.runCommand(
            "git rev-parse --abbrev-ref HEAD 2>&1; git remote -v 2>&1",
            workingFolderUriString, 15
        ) { }
        val lines = res.lines().drop(1).filter { it.isNotBlank() }
        val branch = lines.firstOrNull()?.trim().orEmpty()
        val remotes = lines.drop(1).filter { it.contains("(push)") }
            .map { it.replace("\t", "  ").replace(" (push)", "").trim() }
        val sb = StringBuilder("Branche courante : " + branch.ifEmpty { "inconnue" })
        if (remotes.isEmpty()) sb.append("\nDépôt : aucun remote trouvé")
        else sb.append("\nDépôt :\n").append(remotes.joinToString("\n"))
        val named = Regex("""\b(main|master)\b""").find(command.substringAfter("push"))?.value
        if (named != null && branch.isNotEmpty() && named != branch) {
            sb.append("\nAttention : la commande vise « $named », la branche courante est « $branch ».")
        }
        return sb.toString()
    }

    private suspend fun waitForCommandConfirmation(
        command: String,
        toolCallId: String,
        note: String? = null,
        changes: GitChanges? = null,
        footer: String? = null
    ): Boolean {
        return suspendCancellableCoroutine { continuation ->
            statusMessage = "En attente de ta confirmation…"
            alertUser("Confirmation requise", "Lancer : " + command.take(100))
            pendingCommand = PendingCommand(command, toolCallId, note, changes, footer)
            onCommandConfirmed = { allowed ->
                pendingCommand = null
                statusMessage = "Exécution en cours…"
                continuation.resume(allowed, null)
            }
            continuation.invokeOnCancellation {
                pendingCommand = null
                onCommandConfirmed = null
            }
        }
    }

    fun resolvePendingCommand(allowed: Boolean) {
        onCommandConfirmed?.invoke(allowed)
        onCommandConfirmed = null
        pendingCommand = null
    }

    private suspend fun waitForAsk(toolCallId: String, questions: List<AskQuestion>): String? {
        return suspendCancellableCoroutine { continuation ->
            statusMessage = "En attente de ta réponse…"
            alertUser("L'agent te pose une question", questions.first().question.take(120))
            pendingAsk = PendingAsk(toolCallId, questions)
            onAskAnswered = { answers ->
                pendingAsk = null
                statusMessage = "Exécution en cours…"
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

    /** Écriture avec porte de confirmation (Auto / Intelligent / fichier autorisé / manuel) et sauvegarde pour l'annulation. */
    private suspend fun applyWrite(
        fileRepo: FileRepository,
        toolCallId: String,
        path: String,
        content: String,
        isEdit: Boolean
    ): String {
        if (path.isBlank()) return "Erreur: chemin manquant."
        val raw = fileRepo.readFile(path)
        val exists = !raw.startsWith("Erreur: Fichier introuvable")
        val full = if (exists) fileRepo.readFileForEdit(path).getOrNull() else null
        val oldForDiff: String? = if (!exists) null else (full ?: raw)

        val auto = autoModeInput || path in settingsManager.alwaysAllowedPaths ||
            (smartModeInput && (!exists || isEdit))
        val allowed = if (auto) {
            if (autoModeInput) {
                withContext(Dispatchers.Main) { statusMessage = "Mode Auto : écriture de $path…" }
            }
            true
        } else {
            waitForWriteConfirmation(path, content, toolCallId, oldForDiff)
        }
        if (!allowed) return "Refusé par l'utilisateur: l'écriture de $path a été annulée."

        val res = fileRepo.writeFile(path, content)
        if (res.startsWith("Succès")) {
            withContext(Dispatchers.Main) {
                modifiedPaths[path] = true
                if (path !in summaryFiles) summaryFiles.add(path)
                if (!exists || full != null) agentUndo.add(UndoEntry(path, full))
                editor.onExternalWrite(path, content)
            }
        }
        return res
    }

    /** Annule tous les changements de fichiers faits par l'agent pour la dernière demande. */
    fun undoAgentChanges() {
        if (isLoading || agentUndo.isEmpty()) return
        val entries = agentUndo.toList().reversed()
        agentUndo.clear()
        viewModelScope.launch(Dispatchers.IO) {
            val repo = getFileRepo()
            for (e in entries) {
                val old = e.oldContent
                if (old == null) {
                    repo.deletePath(e.path)
                    withContext(Dispatchers.Main) { forgetPath(e.path) }
                } else {
                    repo.writeFile(e.path, old)
                    withContext(Dispatchers.Main) { editor.onExternalWrite(e.path, old) }
                }
            }
            withContext(Dispatchers.Main) { refreshBrowser() }
        }
    }

    private suspend fun waitForWriteConfirmation(
        path: String,
        content: String,
        toolCallId: String,
        oldContent: String?
    ): Boolean {
        val diff = DiffUtil.diff(oldContent, content)

        return suspendCancellableCoroutine { continuation ->
            alertUser("Confirmation requise", "Écrire $path ?")
            pendingWrite = PendingWrite(path, content, toolCallId, diff)
            onWriteConfirmed = { allowed ->
                pendingWrite = null
                continuation.resume(allowed, null)
            }
            // Cohérent avec les autres confirmations : si la boucle de l'agent est
            // annulée pendant l'attente, la carte ne doit plus rester à l'écran.
            continuation.invokeOnCancellation {
                pendingWrite = null
                onWriteConfirmed = null
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
