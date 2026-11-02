package com.zagent.app.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.zagent.app.data.ActivityEvent
import com.zagent.app.data.ActivityRepository
import com.zagent.app.data.ActivityStatus
import com.zagent.app.data.ConversationMeta
import com.zagent.app.data.ConversationRepository
import com.zagent.app.data.StoredMessage
import com.zagent.app.data.StoredToolCall
import com.zagent.app.data.UsageEntry
import com.zagent.app.data.UsageRepository
import com.zagent.app.data.FileRepository
import com.zagent.app.data.ProjectSearch
import com.zagent.app.data.SettingsManager
import com.zagent.app.ui.editor.ConsoleState
import com.zagent.app.ui.editor.EditorController
import com.zagent.app.ui.theme.ThemeMode
import com.zagent.app.ui.theme.GlassSettings
import com.zagent.app.ui.navigation.ZRoutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
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
import java.util.Locale
import java.util.UUID
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
    val time: Long = System.currentTimeMillis(),
    val reasoning: String? = null
)

enum class AppScreen { CHAT, SETTINGS, TERMINAL, EDITOR, FILE_BROWSER }

class MainViewModel(application: Application) : AndroidViewModel(application) {
    // ── Navigation (MA71) ──────────────────────────────────────────────────
    // Le NavHost de ZNavHost.kt est la source de vérité du back stack. Les écrans
    // continuent d'appeler navigateTo / navigateBack (API inchangée) ; ces demandes
    // sont traduites en requêtes de route typées que ZNavHost exécute sur le
    // NavHost Compose. currentScreen est ensuite resynchronisé depuis la route
    // réellement affichée, ce qui remplace les anciens booléens
    // isSettingsOpen / isTerminalOpen / isEditorOpen / isFileBrowserOpen.

    /** Route demandée (ex. "terminal", "editor?path=...") ; consommée par ZNavHost. */
    var navRouteRequest by mutableStateOf<String?>(null)
        private set

    /** Compteur de demandes de retour ; ZNavHost effectue le pop du back stack. */
    var navBackRequest by mutableIntStateOf(0)
        private set

    private val navigationHistory = mutableStateListOf<AppScreen>()
    var currentScreen by mutableStateOf(AppScreen.CHAT)
        private set

    fun navigateTo(screen: AppScreen, editorPath: String? = null) {
        if (currentScreen == screen) return
        navigationHistory.add(currentScreen)
        navRouteRequest = when (screen) {
            AppScreen.EDITOR -> ZRoutes.editor(editorPath ?: editor.active?.path)
            else -> ZRoutes.of(screen)
        }
    }

    fun navigationPreviousScreen(): AppScreen =
        navigationHistory.lastOrNull() ?: AppScreen.CHAT

    fun navigateBack(): Boolean {
        navBackRequest++
        return true
    }

    /** Consomme la requête de route après son exécution par le NavHost. */
    fun consumeNavRouteRequest() {
        navRouteRequest = null
    }

    /**
     * Resynchronise l'état depuis la route réellement affichée par le NavHost
     * (y compris après un retour système, un deep link ou un raccourci).
     */
    fun syncScreenFromRoute(route: String?) {
        val screen = when {
            route == null -> AppScreen.CHAT
            route.startsWith("editor") -> AppScreen.EDITOR
            route.startsWith("files") -> AppScreen.FILE_BROWSER
            route.startsWith("terminal") -> AppScreen.TERMINAL
            route.startsWith("settings") -> AppScreen.SETTINGS
            else -> AppScreen.CHAT
        }
        if (currentScreen != screen) {
            currentScreen = screen
            navigationHistory.clear()
        }
    }
    val settingsManager = SettingsManager(application)
    private val aiRepository = AiRepository()

    // ── Dépôts de données du LOT 5 (MA56/MA60/MA65) ─────────────────────
    private val conversationRepo = ConversationRepository(application)
    private val usageRepo = UsageRepository(application)
    private val activityRepo = ActivityRepository(application)

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

    // ── Apparence & flux de démarrage ─────────────────────────────────────────
    var themeMode by mutableStateOf(settingsManager.themeMode)
    var oledBlack by mutableStateOf(settingsManager.oledBlack)
        private set
    var dynamicSystemColors by mutableStateOf(settingsManager.dynamicSystemColors)
        private set
    var cornerRadius by mutableStateOf(settingsManager.cornerRadius)
        private set
    var scheduledDarkTime by mutableStateOf(settingsManager.scheduledDarkTime)
        private set
    var scheduledLightTime by mutableStateOf(settingsManager.scheduledLightTime)
        private set
    var fontFamilyId by mutableStateOf(settingsManager.fontFamilyId)
        private set
    var fontScale by mutableStateOf(settingsManager.fontScale)
        private set
    var densityScale by mutableStateOf(settingsManager.densityScale)
        private set

    fun updateFontFamily(value: String) { fontFamilyId = value; settingsManager.fontFamilyId = value }
    fun updateFontScale(value: Float) { fontScale = value.coerceIn(0.85f, 1.25f); settingsManager.fontScale = fontScale }
    fun updateDensityScale(value: Float) { densityScale = value.coerceIn(0.88f, 1.12f); settingsManager.densityScale = densityScale }

    fun updateOledBlack(value: Boolean) { oledBlack = value; settingsManager.oledBlack = value }
    fun updateDynamicSystemColors(value: Boolean) { dynamicSystemColors = value; settingsManager.dynamicSystemColors = value }
    fun updateCornerRadius(value: Int) { cornerRadius = value; settingsManager.cornerRadius = value }
    fun updateScheduledDarkTime(value: String) { scheduledDarkTime = value; settingsManager.scheduledDarkTime = value }
    fun updateScheduledLightTime(value: String) { scheduledLightTime = value; settingsManager.scheduledLightTime = value }
    var glassSettings by mutableStateOf(settingsManager.loadGlassSettings(false))

    /** MA72 : coupe les transitions de navigation (fondu/glissement) quand il est actif. */
    var reduceAnimations by mutableStateOf(settingsManager.reduceAnimations)
        private set

    fun updateReduceAnimations(value: Boolean) {
        reduceAnimations = value
        settingsManager.reduceAnimations = value
    }
    fun updateGlassSettings(dark: Boolean, value: GlassSettings) { glassSettings = value; settingsManager.saveGlassSettings(dark, value) }
    fun reloadGlassSettings(dark: Boolean) { glassSettings = settingsManager.loadGlassSettings(dark) }

    /** Usage tokens/coût sous les messages (MA45). */
    var usageUnderMessages by mutableStateOf(settingsManager.usageUnderMessages)
    fun toggleUsageUnderMessages() {
        usageUnderMessages = !usageUnderMessages
        settingsManager.usageUnderMessages = usageUnderMessages
    }

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

    // ── Brouillons, fichiers du projet et pièces jointes (MA49/MA52/MA53/MA55) ──
    /** Identifiant de la conversation courante : sert de clé au brouillon sauvegardé (MA55). */
    var conversationId by mutableStateOf("")
        private set

    private var draftSaveJob: Job? = null

    init {
        conversationId = settingsManager.lastConversationId.ifBlank { UUID.randomUUID().toString() }
        settingsManager.lastConversationId = conversationId
    }

    /** Appelé à chaque frappe : enregistre le brouillon avec un léger délai pour ne pas surcharger le stockage. */
    fun onInputChanged(text: String) {
        draftSaveJob?.cancel()
        val id = conversationId
        draftSaveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(600)
            settingsManager.saveDraft(id, text)
        }
    }

    /** Appelé après un envoi : le brouillon de la conversation repart de zéro. */
    fun onInputSent() {
        draftSaveJob?.cancel()
        val id = conversationId
        viewModelScope.launch(Dispatchers.IO) { settingsManager.saveDraft(id, "") }
    }

    /** Brouillon sauvegardé de la conversation courante (MA55). */
    fun currentDraft(): String = settingsManager.draftFor(conversationId)

    /** Fichiers du projet (chemins relatifs) pour l'autocomplétion des mentions « @ » (MA53). */
    var projectFiles by mutableStateOf<List<String>>(emptyList())
        private set

    fun refreshProjectFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val files = ProjectSearch.listProjectFiles(getFileRepo())
            withContext(Dispatchers.Main) { projectFiles = files }
        }
    }

    /** Pièce jointe ajoutée depuis la feuille « + » (MA49) : fichier du téléphone, arborescence du projet… */
    data class ChatAttachment(
        val id: Long,
        val name: String,
        val uri: Uri? = null,
        val projectPath: String? = null,
        val isImage: Boolean = false
    )

    val attachments = mutableStateListOf<ChatAttachment>()
    private var nextAttachmentId = 0L

    /** Joint un fichier (ou une image) choisi dans le sélecteur Android. */
    fun addAttachmentUri(uri: Uri) {
        val name = displayNameOf(uri) ?: "fichier"
        val mime = getApplication<Application>().contentResolver.getType(uri).orEmpty()
        attachments.add(ChatAttachment(nextAttachmentId++, name, uri = uri, isImage = mime.startsWith("image/")))
    }

    /** Joint l'arborescence du projet courant au contexte de l'agent. */
    fun addProjectTreeAttachment() {
        attachments.add(ChatAttachment(nextAttachmentId++, folderLabel + " — arborescence", projectPath = "."))
    }

    fun removeAttachment(id: Long) {
        attachments.removeAll { it.id == id }
    }

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

    /** Réflexion du modèle en cours (champ reasoning_content du flux SSE) et heure de départ — MA35. */
    var liveReasoning by mutableStateOf("")
    var thinkingStartedAt by mutableStateOf(0L)

    /** Liste de tâches de l'agent (outil update_todos), affichée en haut du chat. */
    var todos by mutableStateOf<List<TodoItem>>(emptyList())

    /** Résumé de la demande en cours : fichiers touchés et commandes lancées. */
    val summaryFiles = mutableStateListOf<String>()
    val summaryCommands = mutableStateListOf<String>()

    /** Compteurs de diff par fichier touché pendant la demande : chemin → (ajoutées, supprimées) — MA41. */
    val fileDiffCounts = mutableStateMapOf<String, Pair<Int, Int>>()

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
        editor.open(path) { navigateTo(AppScreen.EDITOR) }
        navigateTo(AppScreen.EDITOR)
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
        navigateTo(AppScreen.EDITOR)
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
        if (currentScreen == AppScreen.EDITOR) navigateBack()
        refreshFolderLabel()
        // MA53 : recharge la liste des fichiers pour l'autocomplétion « @ ».
        refreshProjectFiles()
    }

    fun clearConversation() {
        persistCurrentConversation()
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
        liveReasoning = ""
        todos = emptyList()
        summaryFiles.clear()
        summaryCommands.clear()
        fileDiffCounts.clear()
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
        // MA55 : nouvelle conversation → nouvel identifiant, brouillon reparti à zéro.
        draftSaveJob?.cancel()
        val oldId = conversationId
        conversationId = UUID.randomUUID().toString()
        settingsManager.lastConversationId = conversationId
        settingsManager.clearDraft(oldId)
        attachments.clear()
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

    // ── Explorateur de fichiers ─────────────────────────────────────────────

    // ── Terminal embarqué ───────────────────────────────────────────────────
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
            val result = getFileRepo().listEntriesResult(path)
            withContext(Dispatchers.Main) {
                result.onSuccess {
                    browserChildren[path] = it
                    if (path == ".") browserError = null
                }.onFailure {
                    browserError = it.localizedMessage ?: "Impossible de lire le dossier."
                    if (path == ".") browserChildren[path] = emptyList()
                }
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
            val loaded = dirs.map { it to repo.listEntriesResult(it) }
            withContext(Dispatchers.Main) {
                val rootResult = loaded.firstOrNull { it.first == "." }?.second
                rootResult?.onSuccess { browserError = null }?.onFailure {
                    browserError = it.localizedMessage ?: "Impossible de lire le dossier du projet."
                }
                for ((p, result) in loaded) {
                    result.onSuccess { browserChildren[p] = it }
                    if (result.isFailure && p == ".") browserChildren[p] = emptyList()
                }
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
                    editor.open(path) { navigateTo(AppScreen.EDITOR) }
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
                        editor.open(path) { navigateTo(AppScreen.EDITOR) }
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
            navigateTo(AppScreen.SETTINGS)
            statusMessage = "Veuillez entrer une clé API dans les réglages."
            return
        }

        stepLimitReached = false
        agentUndo.clear()
        todos = emptyList()
        summaryFiles.clear()
        summaryCommands.clear()
        fileDiffCounts.clear()

        isLoading = true
        AgentService.start(getApplication())
        agentJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                // MA49/MA53 : le contexte des pièces jointes et des mentions @fichier
                // est assemblé hors du thread principal, puis stocké avec le message.
                val ctx = buildContextBlock(userText)
                val stored = if (ctx.isBlank()) userText else userText + CTX_MARKER + ctx
                withContext(Dispatchers.Main) {
                    _messages.value = _messages.value + ChatMessage("user", stored)
                    attachments.clear()
                }
                persistCurrentConversation()
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

    // ── Contexte des pièces jointes et mentions @fichier (MA49/MA53) ─────────
    /** Marqueur séparant le texte visible de l'utilisateur du bloc de contexte joint au message. */
    companion object {
        const val CTX_MARKER = "\n\n--- Pièces jointes au contexte ---\n\n"

        /** Outils de lecture seule : refusés si « Toujours autoriser la lecture » est désactivé (MA64). */
        val READ_TOOLS = setOf("read_file", "list_dir", "search_in_files")

        // MA77 : actions des raccourcis d'application (appui long sur l'icône).
        const val ACTION_SHORTCUT_NEW_CHAT = "com.zagent.app.action.NEW_CHAT"
        const val ACTION_SHORTCUT_OPEN_TERMINAL = "com.zagent.app.action.OPEN_TERMINAL"
        const val ACTION_SHORTCUT_LAST_PROJECT = "com.zagent.app.action.LAST_PROJECT"
    }

    /**
     * MA73 : nombre de confirmations en attente dans la conversation
     * (écriture, question, suppression, commande) ; alimente le badge de l'onglet Chat.
     */
    val pendingConfirmationCount: Int
        get() = listOfNotNull(pendingWrite, pendingAsk, pendingDelete, pendingCommand).size

    /**
     * MA77 : applique l'action d'un raccourci d'application (Nouvelle conversation,
     * Ouvrir le terminal, Dernier projet). Retourne true si l'action est reconnue.
     */
    fun handleAppShortcut(action: String?): Boolean {
        return when (action) {
            ACTION_SHORTCUT_NEW_CHAT -> {
                clearConversation()
                navRouteRequest = ZRoutes.CHAT
                true
            }
            ACTION_SHORTCUT_OPEN_TERMINAL -> {
                navRouteRequest = ZRoutes.TERMINAL
                true
            }
            ACTION_SHORTCUT_LAST_PROJECT -> {
                // Dernier projet connu : on affiche directement l'explorateur ;
                // sinon l'écran de sélection de projet s'ouvre par-dessus.
                if (workingFolderUriString.isNullOrBlank()) isProjectPickerOpen = true
                navRouteRequest = ZRoutes.FILES
                true
            }
            else -> false
        }
    }

    /**
     * Construit le bloc de contexte envoyé à l'agent : contenu des pièces jointes de la
     * feuille « + » (MA49) puis contenu des fichiers mentionnés avec « @ » (MA53).
     * À appeler sur un thread d'arrière-plan (lectures disque).
     */
    private fun buildContextBlock(userText: String): String {
        val parts = ArrayList<String>()
        for (a in attachments.toList()) {
            if (a.projectPath != null) {
                val tree = getFileRepo().listDir(a.projectPath)
                if (!tree.startsWith("Erreur")) parts.add("[dossier : " + a.name + "]\n" + tree.take(12000))
            } else if (a.uri != null) {
                parts.add(readUriAttachment(a))
            }
        }
        for (path in collectMentionFiles(userText)) {
            val repo = getFileRepo()
            if (repo.isTextFile(path)) {
                val content = repo.readFile(path)
                if (!content.startsWith("Erreur")) {
                    parts.add("[fichier : " + path + "]\n" + content.take(20000))
                }
            }
        }
        if (parts.isEmpty()) return ""
        return parts.joinToString("\n\n")
    }

    /** Lit une pièce jointe du téléphone : texte si possible, sinon simple mention du nom. */
    private fun readUriAttachment(a: ChatAttachment): String {
        val uri = a.uri ?: return ""
        return try {
            val resolver = getApplication<Application>().contentResolver
            val mime = resolver.getType(uri).orEmpty()
            val ext = a.name.substringAfterLast('.', "").lowercase()
            val textual = mime.startsWith("text/") || mime.contains("json") || mime.contains("xml") ||
                mime.contains("javascript") || ext in setOf(
                    "txt", "md", "kt", "kts", "java", "py", "js", "ts", "json", "xml",
                    "html", "css", "csv", "log", "yaml", "yml", "sh", "toml", "gradle"
                )
            if (!textual) "[pièce jointe : " + a.name + " (" + mime.ifEmpty { "type inconnu" } + ")]"
            else resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { r ->
                val sb = StringBuilder()
                val buf = CharArray(1024)
                var n: Int
                var total = 0
                while (r.read(buf).also { n = it } != -1 && total < 20000) {
                    sb.append(buf, 0, n)
                    total += n
                }
                "[fichier : " + a.name + "]\n" + sb.toString()
            } ?: "[pièce jointe : " + a.name + " (illisible)]"
        } catch (e: Exception) {
            "[pièce jointe : " + a.name + " (lecture impossible)]"
        }
    }

    /** Retrouve les @chemins du texte qui correspondent à de vrais fichiers du projet (MA53). */
    private fun collectMentionFiles(userText: String): List<String> {
        val tokens = Regex("@([A-Za-z0-9._/-]+)").findAll(userText)
            .map { it.groupValues[1] }
            .distinct()
            .toList()
        return tokens.filter { projectFiles.contains(it) }
    }

    // ── Commandes slash « / » (MA52) ────────────────────────────────────────
    /** Action demandée à l'écran après l'exécution d'une commande slash. */
    enum class SlashAction { NONE, OPEN_MODEL_PICKER }

    /**
     * Exécute une commande slash tapée dans le champ de saisie.
     * /modèle retourne OPEN_MODEL_PICKER : c'est l'UI qui ouvre le menu ancré sur le chip.
     */
    fun executeSlashCommand(raw: String): SlashAction {
        val cmd = raw.trim().substringBefore(' ').lowercase()
        return when (cmd) {
            "/nouveau" -> { clearConversation(); SlashAction.NONE }
            "/modèle" -> SlashAction.OPEN_MODEL_PICKER
            "/diff" -> { openGitChanges(); SlashAction.NONE }
            "/tester" -> {
                sendMessage(
                    "Lance les tests du projet (choisis l'outil adapté au projet), montre le résultat, puis corrige les échecs éventuels."
                )
                SlashAction.NONE
            }
            "/effacer" -> { clearMessagesOnly(); SlashAction.NONE }
            "/aide" -> { showHelpMessage(); SlashAction.NONE }
            else -> SlashAction.NONE
        }
    }

    /** Efface le fil de messages sans réinitialiser le reste de la conversation (MA52). */
    fun clearMessagesOnly() {
        if (isLoading) return
        _messages.value = emptyList()
        statusMessage = ""
    }

    /** Ajoute localement un message d'aide listant les commandes slash (MA52). */
    fun showHelpMessage() {
        val help = buildString {
            appendLine("Commandes disponibles dans le champ de saisie :")
            appendLine()
            appendLine("• /nouveau — démarrer une nouvelle conversation")
            appendLine("• /modèle — choisir le modèle de la conversation")
            appendLine("• /diff — voir les changements Git du projet")
            appendLine("• /tester — lancer les tests du projet")
            appendLine("• /effacer — effacer les messages du fil")
            appendLine("• /aide — afficher cette aide")
            appendLine()
            append("Astuce : tape « @ » pour joindre un fichier du projet au contexte.")
        }
        _messages.value = _messages.value + ChatMessage("assistant", help)
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
        val customInstrSource = settingsManager.projectInstructionsFor(folderLabel).ifBlank { customInstructionsInput }
        val custom = customInstrSource.trim().let {
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
                liveReasoning = ""
                thinkingStartedAt = System.currentTimeMillis()
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
                },
                onReasoningDelta = { piece ->
                    val nowMs = System.currentTimeMillis()
                    if (nowMs - lastPublish >= 120) {
                        lastPublish = nowMs
                        statusMessage = "Réflexion en cours…"
                        liveReasoning += piece
                    } else {
                        liveReasoning += piece
                    }
                }
            )

            when (result) {
                is AiRepository.ApiResponse.Error -> {
                    alertUser("L'agent s'est arrêté", result.errorMessage.take(120))
                    liveText = ""
                    liveReasoning = ""
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
                    // MA60 : journal d'usage pour l'écran Usage & coûts.
                    recordUsage(usedIn ?: estIn, usedOut ?: estOut, effectiveModel)

                    if (toolCalls.isNullOrEmpty()) {
                        val reasoning = liveReasoning.ifBlank { result.reasoningContent ?: "" }
                        val assistantMsg = ChatMessage("assistant", content ?: "", reasoning = reasoning.ifBlank { null })
                        alertUser("L'agent a terminé", (content ?: "Tâche terminée.").trim().take(140))
                        withContext(Dispatchers.Main) {
                            _messages.value = _messages.value + assistantMsg
                            liveText = ""
                            liveReasoning = ""
                            isLoading = false
                            statusMessage = ""
                        }
                        persistCurrentConversation()
                        finished = true
                        break
                    } else {
                        val reasoning = liveReasoning.ifBlank { result.reasoningContent ?: "" }
                        val assistantMsg = ChatMessage("assistant", content, toolCalls = toolCalls, reasoning = reasoning.ifBlank { null })
                        withContext(Dispatchers.Main) {
                            _messages.value = _messages.value + assistantMsg
                            liveText = ""
                            liveReasoning = ""
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

    /** Stop demandé par l'utilisateur pendant la génération : coupe l'appel réseau et la boucle (MA44). */
    fun stopAgent() {
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
        liveCommandId = null
        liveText = ""
        liveReasoning = ""
        isLoading = false
        statusMessage = "Génération interrompue."
        persistCurrentConversation()
    }

    /** Remplace un message utilisateur et supprime toute la suite, puis relance l'agent (MA37). */
    fun editAndResend(messageIndex: Int, newText: String) {
        if (isLoading || newText.isBlank()) return
        val list = _messages.value
        if (messageIndex !in list.indices || list[messageIndex].role != "user") return
        val kept = list.subList(0, messageIndex)
        _messages.value = kept + ChatMessage("user", newText.trim())
        agentUndo.clear()
        fileDiffCounts.clear()
        summaryFiles.clear()
        summaryCommands.clear()
        todos = emptyList()
        retryLast()
    }

    /** Supprime un message (et tout ce qui suit s'il vient de l'utilisateur) — menu contextuel MA36. */
    fun deleteFromMessage(messageIndex: Int) {
        if (isLoading) return
        val list = _messages.value
        if (messageIndex !in list.indices) return
        _messages.value = if (list[messageIndex].role == "user") {
            list.subList(0, messageIndex)
        } else {
            list.filterIndexed { i, _ -> i != messageIndex }
        }
    }

    /** Régénère la réponse : supprime la fin (assistant + outils) jusqu'au dernier message utilisateur, puis relance (MA36). */
    fun regenerateLast() {
        if (isLoading) return
        val list = _messages.value
        var last = list.lastIndex
        while (last >= 0 && list[last].role != "user") last--
        if (last < 0) return
        _messages.value = list.subList(0, last + 1)
        agentUndo.clear()
        fileDiffCounts.clear()
        retryLast()
    }

    /** Ouvre un chemin de fichier repéré dans le chat, à la ligne si fournie (MA46). */
    fun openFilePath(path: String, line: Int? = null) {
        if (path.isBlank()) return
        editor.openAtLine(path, line) { navigateTo(AppScreen.EDITOR) }
    }

    /** Applique un bloc de code du chat à un fichier (passe par la porte de confirmation habituelle — MA32). */
    fun requestCodeApply(path: String, content: String) {
        if (path.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            applyWrite(getFileRepo(), "user-apply", path.trim().trimStart('/'), content, isEdit = false)
        }
    }

    /** Ouvre l'écran Terminal et place le shell dans le dossier du projet actif. */
    fun openTerminalInProject() {
        navigateTo(AppScreen.TERMINAL)
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
        // MA64 : interrupteur global « Toujours autoriser la lecture ».
        if (!settingsManager.allowRead && tc.name in READ_TOOLS) {
            return "Refusé par réglage: la lecture n'est pas autorisée (Permissions & sécurité)."
        }
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
            val allowed = autoModeInput || settingsManager.allowDelete || waitForDeleteConfirmation(path, tc.id)
            if (!allowed) return "Refusé par l'utilisateur: la suppression de $path a été annulée."
            val t0Del = System.currentTimeMillis()
            val res = fileRepo.deletePath(path)
            logActivity(
                "suppression", path, res.take(140),
                if (res.startsWith("Succès")) ActivityStatus.OK else ActivityStatus.ERREUR,
                System.currentTimeMillis() - t0Del
            )
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
                (settingsManager.allowExecute && !isPush && !isCommit && !isDangerous) ||
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
            val t0Cmd = System.currentTimeMillis()
            try {
                val out = terminal.runCommand(command, workingFolderUriString, timeout) { out -> liveOutput = out }
                // MA64/MA65 : la commande est consignée dans l'activité (et dans l'historique des commandes approuvées).
                logActivity(
                    "commande", command,
                    out.lineSequence().firstOrNull().orEmpty().take(160),
                    if (out.startsWith("Erreur")) ActivityStatus.ERREUR else ActivityStatus.OK,
                    System.currentTimeMillis() - t0Cmd
                )
                return out
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
        val t0Tool = System.currentTimeMillis()
        val toolRes = AgentTools.executeTool(fileRepo, tc.name, tc.arguments)
        logActivity(
            tc.name,
            describeTool(tc.name, tc.arguments).removeSuffix("…"),
            toolRes.lineSequence().firstOrNull().orEmpty().take(160),
            if (toolRes.startsWith("Erreur")) ActivityStatus.ERREUR else ActivityStatus.OK,
            System.currentTimeMillis() - t0Tool
        )
        return toolRes
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

        val auto = autoModeInput || settingsManager.allowWrite ||
            path in settingsManager.alwaysAllowedPaths ||
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

        val t0Write = System.currentTimeMillis()
        val res = fileRepo.writeFile(path, content)
        if (res.startsWith("Succès")) {
            withContext(Dispatchers.Main) {
                modifiedPaths[path] = true
                if (path !in summaryFiles) summaryFiles.add(path)
                if (!exists || full != null) agentUndo.add(UndoEntry(path, full))
                editor.onExternalWrite(path, content)
                // Compteurs de diff pour le récapitulatif de fin de tâche (MA41).
                val d = DiffUtil.diff(oldForDiff, content)
                val prev = fileDiffCounts[path] ?: (0 to 0)
                fileDiffCounts[path] = (prev.first + d.added) to (prev.second + d.removed)
            }
        }
        // MA65 : journal d'activité avec snapshot d'avant écriture (pour « Annuler »).
        logActivity(
            "écriture", path,
            (if (exists) "modification" else "création") + " · " + content.length + " caractères",
            if (res.startsWith("Succès")) ActivityStatus.OK else ActivityStatus.ERREUR,
            System.currentTimeMillis() - t0Write,
            undoPath = path,
            undoContent = oldForDiff?.take(80_000)
        )
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

    // ═════════════════════════════════════════════════════════════════════════
    // LOT 5 (MA56 → MA70) — nouveaux écrans, journaux et feuilles de partage.
    // ═════════════════════════════════════════════════════════════════════════

    // ── MA56 : historique des conversations ──────────────────────────────────
    var conversationList by mutableStateOf<List<ConversationMeta>>(emptyList())
        private set

    fun refreshConversations() {
        conversationList = conversationRepo.loadIndex()
    }

    /** Archive la conversation courante (index + messages) sur le thread IO. */
    fun persistCurrentConversation() {
        val msgs = _messages.value
        val id = conversationId
        if (msgs.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val firstUser = msgs.firstOrNull { it.role == "user" }?.content?.let { it.substringBefore(CTX_MARKER) }
            val lastVisible = msgs.lastOrNull { it.role == "assistant" }?.content
                ?: msgs.lastOrNull()?.content
            val index = conversationRepo.loadIndex().toMutableList()
            val existing = index.firstOrNull { it.id == id }
            val meta = ConversationMeta(
                id = id,
                title = existing?.title ?: conversationRepo.autoTitle(firstUser),
                preview = conversationRepo.autoPreview(lastVisible),
                updatedAt = System.currentTimeMillis(),
                messageCount = msgs.size,
                pinned = existing?.pinned ?: false
            )
            index.removeAll { it.id == id }
            index.add(meta)
            conversationRepo.saveIndex(index)
            conversationRepo.saveMessages(id, msgs.map { m ->
                StoredMessage(
                    role = m.role,
                    content = m.content?.take(200_000),
                    time = m.time,
                    reasoning = m.reasoning,
                    toolCallId = m.toolCallId,
                    name = m.name,
                    toolLog = m.toolLog,
                    toolCalls = m.toolCalls?.map { StoredToolCall(it.id, it.name, it.arguments) }
                )
            })
            val fresh = conversationRepo.loadIndex()
            withContext(Dispatchers.Main) { conversationList = fresh }
        }
    }

    /** Rouvre une conversation sauvegardée : archive la courante puis charge les messages. */
    fun switchToConversation(id: String) {
        if (id == conversationId) return
        persistCurrentConversation()
        viewModelScope.launch(Dispatchers.IO) {
            val stored = conversationRepo.loadMessages(id)
            val restored = stored.map { m ->
                ChatMessage(
                    role = m.role,
                    content = m.content,
                    toolCalls = m.toolCalls?.map { AiRepository.ToolCallData(it.id, it.name, it.arguments) },
                    toolCallId = m.toolCallId,
                    name = m.name,
                    toolLog = m.toolLog,
                    time = m.time,
                    reasoning = m.reasoning
                )
            }
            withContext(Dispatchers.Main) {
                if (_messages.value.isNotEmpty()) persistCurrentConversation()
                _messages.value = restored
                conversationId = id
                settingsManager.lastConversationId = id
                chatModel = null
                todos = emptyList()
                summaryFiles.clear()
                summaryCommands.clear()
                fileDiffCounts.clear()
                agentUndo.clear()
                isLoading = false
                statusMessage = ""
                liveText = ""
                liveReasoning = ""
                liveCommand = null
                liveCommandId = null
                draftSaveJob?.cancel()
            }
        }
    }

    /** Supprime une conversation de l'historique (MA56). */
    fun deleteConversation(id: String) {
        if (id == conversationId) {
            clearConversation()
        }
        viewModelScope.launch(Dispatchers.IO) {
            conversationRepo.deleteMessages(id)
            val index = conversationRepo.loadIndex().filter { it.id != id }
            conversationRepo.saveIndex(index)
            val fresh = conversationRepo.loadIndex()
            withContext(Dispatchers.Main) { conversationList = fresh }
        }
    }

    /** Épingle / désépingle une conversation (glisser vers la droite, MA56). */
    fun toggleConversationPin(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val index = conversationRepo.loadIndex().toMutableList()
            val i = index.indexOfFirst { it.id == id }
            if (i >= 0) {
                index[i] = index[i].copy(pinned = !index[i].pinned)
                conversationRepo.saveIndex(index)
            }
            val fresh = conversationRepo.loadIndex()
            withContext(Dispatchers.Main) { conversationList = fresh }
        }
    }

    // ── MA57 : projets favoris + racine publique ─────────────────────────────
    fun projectsRootPublic(): java.io.File = projectsRoot()

    fun toggleProjectFavorite(name: String) {
        val favs = settingsManager.favoriteProjects
        settingsManager.favoriteProjects = if (name in favs) favs - name else favs + name
    }

    // ── MA58 : recherche globale ─────────────────────────────────────────────
    var searchHistory by mutableStateOf<List<String>>(emptyList())
        private set

    fun refreshSearchHistory() {
        searchHistory = settingsManager.searchHistory
    }

    fun addSearchQuery(query: String) {
        settingsManager.addSearchQuery(query)
        searchHistory = settingsManager.searchHistory
    }

    /** Recherche globale : conversations sauvegardées + code du projet, en IO. */
    fun runGlobalSearch(
        query: String,
        onDone: (List<com.zagent.app.data.ConversationHit>, List<ProjectSearch.CodeHit>) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val convs = conversationRepo.searchContent(query)
            val files = if (workingFolderUriString != null) {
                ProjectSearch.codeSearch(getFileRepo(), query)
            } else emptyList()
            withContext(Dispatchers.Main) { onDone(convs, files) }
        }
    }

    // ── MA59 : modèle global et URL personnalisée ────────────────────────────
    fun selectGlobalModel(id: String) {
        val v = id.trim()
        if (v.isEmpty()) return
        modelInput = v
        settingsManager.model = v
    }

    fun saveBaseUrl(url: String) {
        val v = url.trim()
        if (v.isNotEmpty()) {
            baseUrlInput = v
            settingsManager.baseUrl = v
        }
    }

    // ── MA60 : usage & coûts ─────────────────────────────────────────────────
    fun usageRepoSnapshot(): List<UsageEntry> = usageRepo.load()

    fun clearUsage() {
        usageRepo.clear()
    }

    private fun recordUsage(inTok: Int, outTok: Int, model: String) {
        val cost = if (priceInInput <= 0f && priceOutInput <= 0f) 0.0
        else inTok * priceInInput / 1_000_000.0 + outTok * priceOutInput / 1_000_000.0
        usageRepo.add(UsageEntry(System.currentTimeMillis(), model, folderLabel, inTok, outTok, cost))
    }

    // ── MA61 : Git plein écran ───────────────────────────────────────────────
    data class GitCommit(val hash: String, val author: String, val date: Long, val subject: String)
    data class GitBranch(val name: String, val current: Boolean)

    var gitLog by mutableStateOf<List<GitCommit>>(emptyList())
    var gitBranches by mutableStateOf<List<GitBranch>>(emptyList())
    var gitBranchBusy by mutableStateOf(false)
    var gitCommitDiff by mutableStateOf<String?>(null)
        private set
    var gitCommitDiffHash by mutableStateOf<String?>(null)
        private set
    var gitCommitMessage by mutableStateOf("")
    var isGeneratingCommitMsg by mutableStateOf(false)

    fun refreshGitLog() {
        viewModelScope.launch(Dispatchers.IO) {
            val res = terminal.runCommand(
                "git log --pretty=format:%h|%an|%at|%s -30 2>&1",
                workingFolderUriString, 20, silent = true
            ) { }
            val list = res.lines().filter { it.contains('|') }.mapNotNull {
                val p = it.split("|", limit = 4)
                if (p.size < 4) null
                else GitCommit(p[0].trim(), p[1].trim(), (p[2].toLongOrNull() ?: 0L) * 1000L, p[3].trim())
            }
            withContext(Dispatchers.Main) {
                gitLog = if (res.startsWith("Erreur") || res.contains("fatal") || res.contains("NOREPO")) emptyList() else list
            }
        }
    }

    fun refreshGitBranches() {
        viewModelScope.launch(Dispatchers.IO) {
            val res = terminal.runCommand(
                "git branch --format=%(HEAD)|%(refname:short) 2>&1",
                workingFolderUriString, 15, silent = true
            ) { }
            val list = res.lines().filter { it.contains('|') }.map {
                val p = it.split("|", limit = 2)
                GitBranch(p.getOrNull(1)?.trim().orEmpty(), p.getOrNull(0)?.trim() == "*")
            }.filter { it.name.isNotEmpty() }
            withContext(Dispatchers.Main) { gitBranches = list }
        }
    }

    fun showCommitDiff(hash: String) {
        if (gitCommitDiffHash == hash) {
            gitCommitDiff = null
            gitCommitDiffHash = null
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val res = terminal.runCommand(
                "git show " + hash + " --no-color --no-ext-diff --stat --patch 2>&1 | head -c 30000",
                workingFolderUriString, 20, silent = true, outLimit = 32000
            ) { }
            withContext(Dispatchers.Main) {
                gitCommitDiff = res
                gitCommitDiffHash = hash
            }
        }
    }

    /** Indexe ou désindexe un fichier (case à cocher, MA61). */
    fun gitToggleStage(path: String, currentlyStaged: Boolean) {
        val cmd = if (currentlyStaged) "git reset -q -- \"" + path + "\"" else "git add -q -- \"" + path + "\""
        viewModelScope.launch(Dispatchers.IO) {
            terminal.runCommand(cmd, workingFolderUriString, 15, silent = true) { }
            val r = readGitChanges()
            withContext(Dispatchers.Main) { gitChanges = r }
        }
    }

    fun gitCommit(message: String, onDone: () -> Unit) {
        val msg = message.trim()
        if (msg.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val quoted = msg.replace("\"", "'")
            val res = terminal.runCommand(
                "git commit -m \"" + quoted + "\" 2>&1",
                workingFolderUriString, 60, silent = true
            ) { }
            val r = readGitChanges()
            withContext(Dispatchers.Main) {
                gitChanges = r
                statusMessage = if (res.startsWith("Erreur") || res.contains("nothing to commit")) {
                    res.lineSequence().firstOrNull()?.take(140) ?: "Commit impossible."
                } else {
                    "Commit créé : " + res.lineSequence().firstOrNull()?.take(80).orEmpty()
                }
                onDone()
            }
        }
    }

    /** Génère un message de commit avec l'IA à partir du diff courant (MA61). */
    fun generateCommitMessage() {
        if (isGeneratingCommitMsg) return
        val diffText = gitChanges?.diff.orEmpty()
        if (diffText.isBlank()) {
            statusMessage = "Aucun changement à commenter."
            return
        }
        isGeneratingCommitMsg = true
        gitCommitMessage = ""
        viewModelScope.launch(Dispatchers.IO) {
            val prompt = buildJsonArray {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", "Tu écris des messages de commit Git en français, style conventionnel « type: résumé au présent » (72 caractères max). Réponds UNIQUEMENT avec le message, sans guillemets ni explication.")
                })
                add(buildJsonObject {
                    put("role", "user")
                    put("content", "Voici le diff des changements en cours :\n\n" + diffText.take(12000))
                })
            }
            val buf = StringBuilder()
            runCatching {
                aiRepository.callChatCompletions(
                    baseUrl = baseUrlInput,
                    apiKey = apiKeyInput,
                    model = effectiveModel,
                    messages = prompt,
                    onRetryUpdate = { },
                    onDelta = { piece ->
                        buf.append(piece)
                        gitCommitMessage = buf.toString().trim().take(200)
                    }
                )
            }
            withContext(Dispatchers.Main) {
                isGeneratingCommitMsg = false
                if (gitCommitMessage.isBlank()) gitCommitMessage = "Mise à jour du projet"
            }
        }
    }

    fun gitCheckout(branch: String) {
        if (gitBranchBusy || branch.isBlank()) return
        gitBranchBusy = true
        viewModelScope.launch(Dispatchers.IO) {
            val res = terminal.runCommand(
                "git checkout \"" + branch.replace("\"", "'") + "\" 2>&1",
                workingFolderUriString, 30, silent = true
            ) { }
            val r = readGitChanges()
            withContext(Dispatchers.Main) {
                statusMessage = res.lineSequence().firstOrNull()?.take(120) ?: ""
                gitChanges = r
                refreshGitBranches()
                refreshProjectFiles()
                gitBranchBusy = false
            }
        }
    }

    // ── MA62 : aperçu plein écran ────────────────────────────────────────────
    var isPreviewScreenOpen by mutableStateOf(false)
    var previewScreenPath by mutableStateOf("")

    fun openPreviewScreen(path: String) {
        if (path.isBlank()) return
        previewScreenPath = path
        previewTick++
        isPreviewScreenOpen = true
    }

    // ── MA63 : instructions par projet ───────────────────────────────────────
    fun projectInstructionsText(): String = settingsManager.projectInstructionsFor(folderLabel)

    fun setProjectInstructionsForCurrent(value: String) {
        settingsManager.setProjectInstructions(folderLabel, value)
    }

    // ── MA64 : permissions ───────────────────────────────────────────────────
    fun removeAlwaysAllowedPath(path: String) {
        settingsManager.alwaysAllowedPaths = settingsManager.alwaysAllowedPaths - path
    }

    fun activityRepoSnapshot(): List<ActivityEvent> = activityRepo.load()

    /** Consigne un événement d'activité (jamais sur le thread principal). */
    private fun logActivity(
        kind: String,
        label: String,
        detail: String,
        status: String,
        durationMs: Long,
        undoPath: String? = null,
        undoContent: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            activityRepo.add(
                ActivityEvent(
                    id = System.nanoTime(),
                    ts = System.currentTimeMillis(),
                    kind = kind,
                    label = label.take(160),
                    detail = detail.take(300),
                    status = status,
                    durationMs = durationMs,
                    conversationId = conversationId,
                    undoPath = undoPath,
                    undoContent = undoContent
                )
            )
        }
    }

    // ── MA65 : annulation d'une écriture depuis le journal d'activité ────────
    fun undoActivityEntry(event: ActivityEvent, onDone: () -> Unit) {
        val path = event.undoPath
        if (path == null || event.status != ActivityStatus.OK) {
            onDone()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val repo = getFileRepo()
            val old = event.undoContent
            val res = if (old == null) repo.deletePath(path) else repo.writeFile(path, old)
            if (res.startsWith("Succès")) activityRepo.markUndone(event.id)
            withContext(Dispatchers.Main) {
                if (res.startsWith("Succès")) {
                    if (old == null) forgetPath(path) else editor.onExternalWrite(path, old)
                    refreshBrowser()
                } else {
                    statusMessage = res.take(140)
                }
                onDone()
            }
        }
    }

    // ── MA68 : templates de projet ───────────────────────────────────────────
    fun applyTemplate(
        templateLabel: String,
        files: Map<String, String>,
        firstMessage: String,
        onDone: (Boolean) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                transferStatus = "Création du projet « " + templateLabel + " »…"
            }
            val base = sanitizeProjectName(templateLabel.replace(" ", "-").lowercase(Locale.ROOT))
            val name = uniqueProjectName(base)
            var ok = false
            withContext(Dispatchers.Main) {
                ok = createProject(name) == null
            }
            ok = ok && runCatching {
                val repo = getFileRepo()
                for ((path, content) in files) {
                    val res = repo.writeFile(path, content)
                    if (!res.startsWith("Succès")) return@runCatching false
                }
                true
            }.getOrDefault(false)
            if (ok) refreshProjectFiles()
            val success = ok
            withContext(Dispatchers.Main) {
                transferStatus = if (success) "Projet créé : $name" else "Erreur lors de la création du projet"
                if (success) {
                    isTemplatesOpen = false
                    sendMessage(firstMessage)
                }
                onDone(success)
            }
        }
    }

    // ── MA70 : export / partage de la conversation ───────────────────────────
    fun buildConversationMarkdown(includeTools: Boolean, hidePaths: Boolean): String {
        val df = java.text.SimpleDateFormat("d MMM yyyy · HH:mm", java.util.Locale.getDefault())
        val sb = StringBuilder()
        sb.append("# Conversation ZAgent\n\n")
        sb.append("_Projet : ").append(folderLabel).append(" · ").append(df.format(java.util.Date())).append("_\n\n")
        for (m in _messages.value) {
            val raw = m.content.orEmpty()
            val content = if (m.role == "user") raw.substringBefore(CTX_MARKER) else raw
            if (m.role == "tool" && !includeTools) continue
            if (content.isBlank() && !(includeTools && !m.toolCalls.isNullOrEmpty())) continue
            when (m.role) {
                "user" -> sb.append("## Toi\n\n")
                "assistant" -> sb.append("## ZAgent\n\n")
                "tool" -> sb.append("### Outil : ").append(m.name.orEmpty()).append("\n\n")
                else -> { }
            }
            var body = content
            if (hidePaths) body = hideLocalPaths(body)
            if (body.isNotBlank()) sb.append(body.take(20_000)).append("\n\n")
            if (includeTools && !m.toolCalls.isNullOrEmpty()) {
                for (tc in m.toolCalls) {
                    sb.append("- Appel `").append(tc.name).append("` : `").append(tc.arguments.take(200)).append("`\n")
                }
                sb.append("\n")
            }
        }
        return sb.toString()
    }

    private fun hideLocalPaths(text: String): String {
        val root = workingFolderUriString
        var out = text
        if (!root.isNullOrBlank()) out = out.replace(root, "…")
        val home = java.io.File(getApplication<Application>().filesDir, "home").absolutePath
        return out.replace(home, "…")
    }

    fun exportConversationMarkdown(uri: android.net.Uri, includeTools: Boolean, hidePaths: Boolean, onDone: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val md = buildConversationMarkdown(includeTools, hidePaths)
            val ok = runCatching {
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(md.toByteArray(Charsets.UTF_8)) } != null
            }.getOrDefault(false)
            withContext(Dispatchers.Main) { onDone(ok) }
        }
    }

    fun exportConversationPdf(uri: android.net.Uri, includeTools: Boolean, hidePaths: Boolean, onDone: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val md = buildConversationMarkdown(includeTools, hidePaths)
            val ok = runCatching { writeConversationPdf(md, uri) }.getOrDefault(false)
            withContext(Dispatchers.Main) { onDone(ok) }
        }
    }

    /** Écrit un PDF paginé A4 (texte simple, repli propre API 26+). */
    private fun writeConversationPdf(text: String, uri: android.net.Uri): Boolean {
        val doc = android.graphics.pdf.PdfDocument()
        try {
            val bodyPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(20, 20, 19)
                textSize = 11f
                typeface = android.graphics.Typeface.SERIF
            }
            val titlePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(201, 100, 66)
                textSize = 15f
                typeface = android.graphics.Typeface.SERIF
                isFakeBoldText = true
            }
            val pageW = 595
            val pageH = 842
            val margin = 48f
            val lineHeight = 15f
            val maxChars = 92
            val lines = ArrayList<Pair<Boolean, String>>()
            for (raw in text.lines()) {
                var line = raw
                if (line.length <= maxChars) {
                    lines.add(line.startsWith("#") to line)
                } else {
                    while (line.length > maxChars) {
                        lines.add(line.startsWith("#") to line.take(maxChars))
                        line = line.drop(maxChars)
                    }
                    lines.add(line.startsWith("#") to line)
                }
            }
            var pageIdx = 1
            var page = doc.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(pageW, pageH, pageIdx).create())
            var canvas = page.canvas
            var y = margin + 12f
            canvas.drawText("Conversation ZAgent", margin, y, titlePaint)
            y += lineHeight * 1.6f
            for ((isTitle, line) in lines) {
                if (y > pageH - margin) {
                    doc.finishPage(page)
                    pageIdx++
                    page = doc.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(pageW, pageH, pageIdx).create())
                    canvas = page.canvas
                    y = margin + 12f
                }
                if (line.isBlank()) {
                    y += lineHeight * 0.6f
                    continue
                }
                canvas.drawText(line.replace("\t", "    "), margin, y, if (isTitle) titlePaint else bodyPaint)
                y += if (isTitle) lineHeight * 1.3f else lineHeight
            }
            doc.finishPage(page)
            val ok = runCatching {
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) } != null
            }.getOrDefault(false)
            return ok
        } finally {
            doc.close()
        }
    }

    // ── Overlays LOT 5 (rendus par AppRoot) ──────────────────────────────────
    var isHistoryOpen by mutableStateOf(false)
    var isSearchOpen by mutableStateOf(false)
    var isGitScreenOpen by mutableStateOf(false)
    var isTemplatesOpen by mutableStateOf(false)
    var isShareSheetOpen by mutableStateOf(false)

    val isLot5OverlayOpen: Boolean
        get() = isHistoryOpen || isSearchOpen || isGitScreenOpen || isTemplatesOpen ||
            isPreviewScreenOpen || isShareSheetOpen || isProjectPickerOpen

    /** Ouvre la recherche globale : rafraîchit historique + fichiers du projet. */
    fun openSearchScreen() {
        refreshSearchHistory()
        refreshProjectFiles()
        isSearchOpen = true
    }

    /** Essai du verre depuis l'onboarding (MA69) : l'interrupteur global du verre. */
    fun setGlassTrialEnabled(enabled: Boolean) {
        glassSettings = glassSettings.copy(enabled = enabled)
        settingsManager.glassEnabled = enabled
    }
}
