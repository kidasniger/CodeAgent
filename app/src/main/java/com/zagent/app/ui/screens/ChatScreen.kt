package com.zagent.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zagent.app.R
import com.zagent.app.data.AiRepository
import com.zagent.app.data.AskQuestion
import com.zagent.app.data.TodoItem
import com.zagent.app.data.parseAskQuestions
import com.zagent.app.ui.components.AnsiText
import com.zagent.app.ui.components.tailForDisplay
import com.zagent.app.ui.components.AskCard
import com.zagent.app.data.DiffUtil
import com.zagent.app.ui.components.GlassModalBottomSheet
import com.zagent.app.ui.components.EmptyState
import com.zagent.app.ui.components.GlassAlertDialog
import com.zagent.app.ui.components.GlassCard
import com.zagent.app.ui.components.GradientButton
import com.zagent.app.ui.components.IconBadge
import com.zagent.app.ui.components.MarkdownText
import com.zagent.app.ui.components.MonoChip
import com.zagent.app.ui.components.SecondaryButton
import com.zagent.app.ui.components.StatusBadge
import com.zagent.app.ui.components.ThinkingBlock
import com.zagent.app.ui.components.ToolGroupSummary
import com.zagent.app.ui.components.TypingPill
import com.zagent.app.ui.components.UsageLine
import com.zagent.app.ui.components.ZLogo
import com.zagent.app.ui.components.rememberZHaptics
import com.zagent.app.ui.components.DevServerBar
import com.zagent.app.ui.components.GitChangesSheet
import com.zagent.app.ui.components.GitDiffView
import com.zagent.app.ui.components.ModelPickerDialog
import com.zagent.app.ui.components.InputBarLot4
import com.zagent.app.ui.components.CodeBlock
import com.zagent.app.ui.components.DiffPanel
import com.zagent.app.ui.components.ToolChip
import com.zagent.app.ui.components.ToolState
import com.zagent.app.ui.components.ZMenu
import com.zagent.app.ui.components.ZMenuItem
import com.zagent.app.ui.components.ZMenuSeparator
import com.zagent.app.ui.components.buildInlineMarkdown
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.SpaceGrotesk
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.theme.LocalGlass
import com.zagent.app.ui.theme.LocalHazeState
import com.zagent.app.ui.theme.LocalReduceMotion
import com.zagent.app.ui.theme.ZMouvement
import com.zagent.app.ui.theme.glassSurface
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.zagent.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Écran principal : conversation avec l'agent (style minimaliste — LOT 3)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit
) {
    val c = Zcolors
    val haptique = rememberZHaptics() // MàJ 91
    val messages by viewModel.messages.collectAsStateWithLifecycle() // MàJ 99 : ne collecte que si l'écran est visible
    // MàJ 99 — clés stables : les lignes du fil (dates, bulles, outils, demandes)
    // sont dérivées des messages et mémorisées tant que la liste ne change pas.
    val chatRows = remember(messages) { buildChatRows(messages) }
    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var input by rememberSaveable { mutableStateOf("") }
    var editingIndex by rememberSaveable { mutableStateOf(-1) }
    var applyCode by remember { mutableStateOf<String?>(null) }

    // MA55 : restaure le brouillon enregistré pour cette conversation, et synchronise
    // chaque frappe vers le brouillon (sauvegarde différée côté MainViewModel).
    val setInput: (String) -> Unit = { t ->
        input = t
        viewModel.onInputChanged(t)
    }
    LaunchedEffect(viewModel.conversationId) {
        setInput(viewModel.currentDraft())
    }

    // MàJ 99 — derivedStateOf : la valeur « défilé » n'est recalculée que lorsque
    // le booléen change, au lieu de relire firstVisibleItemScrollOffset à chaque
    // frame de scroll dans la composition (recompositions inutiles de la barre).
    val barreDefilee by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 8
        }
    }
    val lastFailedCmdId = remember(messages) {
        val m = messages.lastOrNull { it.role == "tool" && it.name == "run_command" }
        val r = m?.content.orEmpty()
        if (m != null && (r.startsWith("Erreur: code") || r.startsWith("Erreur: délai"))) m.toolCallId else null
    }

    // MàJ 89 — apparition animée des nouveaux messages (fondu + glissement de
    // 8 dp en 200 ms). Les clés déjà vues sont mémorisées : l'historique chargé
    // (premier lot après un changement de conversation) n'est jamais animé.
    val clesVues = remember { mutableSetOf<String>() }
    LaunchedEffect(viewModel.conversationId) { clesVues.clear() }
    val estNouvelElement: (String) -> Boolean = { cle -> clesVues.isNotEmpty() && cle !in clesVues }
    SideEffect { clesVues.addAll(chatRows.map { it.key }) }

    // MA33 : streaming par frame (regroupe les mises à jour de texte pour éviter les saccades)
    var displayLive by remember { mutableStateOf("") }
    LaunchedEffect(viewModel.liveText) {
        withFrameNanos { }
        displayLive = viewModel.liveText
    }

    // MA33 : l'auto-scroll suit le flux seulement si l'utilisateur est déjà en bas
    var userAtBottom by remember { mutableStateOf(true) }
    LaunchedEffect(listState) {
        androidx.compose.runtime.snapshotFlow {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= info.totalItemsCount - 2
        }.collect { userAtBottom = it }
    }
    LaunchedEffect(chatRows.size, displayLive.length) {
        if (userAtBottom && chatRows.isNotEmpty()) {
            if (displayLive.isNotBlank()) listState.scrollToItem(chatRows.size)
            else listState.animateScrollToItem(chatRows.size - 1)
        }
    }

    // MA39 : les pastilles de date se fondent après 2 s d'inactivité de scroll
    var dateAlphaTarget by remember { mutableStateOf(1f) }
    var hideDatesJob by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(listState) {
        androidx.compose.runtime.snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            hideDatesJob?.cancel()
            if (scrolling) {
                dateAlphaTarget = 1f
            } else {
                hideDatesJob = launch { delay(2000); dateAlphaTarget = 0f }
            }
        }
    }
    val dateAlpha by animateFloatAsState(dateAlphaTarget, label = "dates")

    // Chronomètre de réflexion (MA35)
    var nowTick by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(viewModel.isLoading) {
        while (viewModel.isLoading) {
            nowTick = System.currentTimeMillis()
            delay(1000)
        }
    }

    // MàJ 91 — légère vibration à la fin d'une tâche (isLoading vrai → faux).
    var tachePrecedente by remember { mutableStateOf(false) }
    LaunchedEffect(viewModel.isLoading) {
        if (tachePrecedente && !viewModel.isLoading) haptique.tacheTerminee()
        tachePrecedente = viewModel.isLoading
    }

    val onFileTap: (String, Int?) -> Unit = { path, line -> viewModel.openFilePath(path, line) }
    val thinkingSeconds = ((nowTick - viewModel.thinkingStartedAt) / 1000).toInt().coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        AmbientTopBar(
            viewModel = viewModel,
            scrolled = barreDefilee,
            onNewConversation = { viewModel.clearConversation() },
            onOpenSettings = onOpenSettings
        )

        // MA43 : une seule notification discrète à la fois, en pilule de verre
        GlassToastPill(viewModel = viewModel, onOpenSettings = onOpenSettings)
        if (viewModel.isNetworkError) AutoRetryEffect(viewModel)

        // Liste de tâches de l'agent (MA40)
        TodoPanel(viewModel.todos, viewModel.isLoading)

        // Liste des messages (MA38 : sélection de texte partout)
        val selectionColors = TextSelectionColors(handleColor = c.cyan, backgroundColor = c.cyan.copy(alpha = 0.25f))
        Box(Modifier.weight(1f)) {
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center
                ) {
                    ChatEmptyState(
                        viewModel, onOpenSettings,
                        onPickFolder = { viewModel.isProjectPickerOpen = true },
                        onSuggestion = { setInput(it); editingIndex = -1 }
                    )
                }
            } else {
                CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
                    SelectionContainer {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 176.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            chatRows.forEachIndexed { index, row ->
                                when (row) {
                                    is ChatRow.Date -> stickyHeader(key = row.key, contentType = "date") {
                                        DatePill(row.label, Modifier.alpha(dateAlpha))
                                    }
                                    is ChatRow.User -> item(key = row.key, contentType = "user") {
                                        EntreeMessage(
                                            activee = estNouvelElement(row.key),
                                            modifier = Modifier.animateItem()
                                        ) {
                                            UserBubble(
                                                text = row.text,
                                                messageIndex = row.messageIndex,
                                                onEdit = { i, t -> setInput(t); editingIndex = i },
                                                onQuote = { t -> setInput("> " + t.replace("\n", "\n> ") + "\n\n") },
                                                onDelete = { viewModel.deleteFromMessage(it) }
                                            )
                                        }
                                    }
                                    is ChatRow.Agent -> item(key = row.key, contentType = "agent") {
                                        val isLastAgent = index == chatRows.lastIndex && displayLive.isBlank()
                                        EntreeMessage(
                                            activee = estNouvelElement(row.key),
                                            modifier = Modifier.animateItem()
                                        ) {
                                            AgentCard(
                                                content = row.content,
                                                tools = row.tools,
                                                showLogo = chatRows.getOrNull(index - 1) !is ChatRow.Agent,
                                                reasoning = row.reasoning,
                                                thinkingSeconds = -1,
                                                isLast = isLastAgent && !viewModel.isLoading,
                                                messageIndex = row.messageIndex,
                                                modeleLabel = row.modele,
                                                onFileTap = onFileTap,
                                                onApplyCode = { applyCode = it },
                                                onRegenerate = { viewModel.regenerateLast() },
                                                onQuote = { t -> setInput("> " + t.replace("\n", "\n> ") + "\n\n") },
                                                onDeleteMessage = { viewModel.deleteFromMessage(row.messageIndex) },
                                                fixId = if (viewModel.isLoading) null else lastFailedCmdId,
                                                onFix = { cmd, res -> viewModel.fixCommandWithAi(cmd, res) },
                                                liveId = viewModel.liveCommandId,
                                                liveOutput = { viewModel.liveOutput },
                                                liveStartedAt = viewModel.liveStartedAt,
                                                onStop = { viewModel.stopLiveCommand() }
                                            )
                                        }
                                    }
                                    is ChatRow.OrphanTool -> item(key = row.key, contentType = "orphan") {
                                        EntreeMessage(
                                            activee = estNouvelElement(row.key),
                                            modifier = Modifier.animateItem()
                                        ) {
                                            OrphanToolLine(row.text)
                                        }
                                    }
                                    is ChatRow.Ask -> item(key = row.key, contentType = "ask") {
                                        EntreeMessage(
                                            activee = estNouvelElement(row.key),
                                            modifier = Modifier.animateItem()
                                        ) {
                                            AskCard(
                                                questions = row.questions,
                                                result = row.result,
                                                active = row.result == null &&
                                                    viewModel.pendingAsk?.toolCallId == row.toolCallId,
                                                onSubmit = { answers -> viewModel.resolvePendingAsk(answers) }
                                            )
                                        }
                                    }
                                }
                            }
                            if (displayLive.isNotBlank()) {
                                item(key = "live_text", contentType = "agent") {
                                    AgentCard(
                                        content = displayLive,
                                        tools = emptyList(),
                                        isStreaming = true,
                                        reasoning = viewModel.liveReasoning,
                                        thinkingSeconds = thinkingSeconds,
                                        onFileTap = onFileTap,
                                        onApplyCode = { applyCode = it }
                                    )
                                }
                            }
                            // MA45 : usage tokens/coût sous le dernier message
                            val lastRow = chatRows.lastOrNull()
                            if (displayLive.isBlank() && lastRow is ChatRow.Agent) {
                                item(key = "usage_tail", contentType = "meta") {
                                    Column(Modifier.padding(horizontal = 16.dp)) {
                                        UsageLine(
                                            totalTokens = viewModel.totalTokens,
                                            estimated = viewModel.tokensEstimated,
                                            cost = viewModel.estimatedCost,
                                            promptTokens = viewModel.promptTokens,
                                            completionTokens = viewModel.completionTokens,
                                            showToggle = true,
                                            usageUnderMessages = viewModel.usageUnderMessages,
                                            onToggleUsageUnderMessages = { viewModel.toggleUsageUnderMessages() }
                                        )
                                    }
                                }
                            }
                            // MA47 : erreur humaine dans le fil, sous le dernier message
                            if (!viewModel.isLoading && (viewModel.hasBlockingError || viewModel.stepLimitReached || viewModel.statusMessage.isNotBlank())) {
                                item(key = "error_inline", contentType = "meta") {
                                    InlineErrorCard(viewModel = viewModel, onOpenSettings = onOpenSettings)
                                }
                            }
                        }
                    }
                }

                // MA33 : bouton flottant en verre quand l'utilisateur n'est pas en bas
                if (!userAtBottom) {
                    Row(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .glassSurface(LocalHazeState.current, LocalGlass.current, c.isDark, RoundedCornerShape(50.dp), LocalContext.current)
                            .background(c.surface2.copy(alpha = if (LocalGlass.current.enabled) 0.12f else 1f), RoundedCornerShape(50.dp))
                            .clickable {
                                scope.launch {
                                    if (displayLive.isNotBlank()) listState.scrollToItem(chatRows.size)
                                    else listState.animateScrollToItem(chatRows.size - 1)
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ArrowDownward, null, tint = c.text2, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.scroll_new_message), style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.5.sp), color = c.text)
                    }
                }
            }
        }

        // Pastille de statut de l'agent + barre de saisie flottante
        if (viewModel.isLoading) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                TypingPill(
                    viewModel.statusMessage.ifBlank { stringResource(R.string.typing_default) },
                    onStop = { viewModel.stopAgent() }
                )
            }
        }
        if (!viewModel.isLoading && (viewModel.summaryFiles.isNotEmpty() || viewModel.summaryCommands.isNotEmpty())) {
            TaskSummary(
                files = viewModel.summaryFiles.toList(),
                commands = viewModel.summaryCommands.toList(),
                stats = viewModel.fileDiffCounts.toMap(),
                hasUndo = viewModel.agentUndo.isNotEmpty(),
                onViewDiff = { viewModel.openGitChanges() },
                onUndo = { viewModel.undoAgentChanges() }
            )
        }
        if (!viewModel.isLoading && viewModel.agentUndo.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                val html = viewModel.lastAgentHtml
                if (html != null) {
                    Text(
                        stringResource(R.string.action_open_preview),
                        color = c.cyan,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(c.cyanBg)
                            .clickable { viewModel.openPreview(html) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Text(
                    stringResource(R.string.action_undo_changes, viewModel.agentUndo.size),
                    color = c.amber,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(c.amberBg)
                        .clickable { viewModel.undoAgentChanges() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        if (viewModel.terminal.devRunning) {
            DevServerBar(
                url = viewModel.terminal.devUrl,
                log = viewModel.terminal.devLog,
                onOpen = { viewModel.openDevPreview() },
                onStop = { viewModel.stopDevServerByUser() }
            )
        }
        if (editingIndex >= 0) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.editing_banner),
                    style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp),
                    color = c.amber,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Annuler",
                    color = c.cyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraSmall)
                        .clickable { editingIndex = -1; setInput("") }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        InputBarLot4(
            value = input,
            onValueChange = setInput,
            onSend = {
                val t = input
                if (t.isNotBlank()) haptique.envoi() // MàJ 91 — légère vibration à l'envoi
                viewModel.onInputSent()
                input = ""
                if (editingIndex >= 0) {
                    val idx = editingIndex
                    editingIndex = -1
                    viewModel.editAndResend(idx, t)
                } else {
                    viewModel.sendMessage(t)
                }
            },
            enabled = !viewModel.isLoading,
            isLoading = viewModel.isLoading,
            onStop = { viewModel.stopAgent() },
            viewModel = viewModel
        )
    }

    // MA32 : appliquer un bloc de code à un fichier (passe par la porte de confirmation)
    applyCode?.let { code ->
        ApplyCodeDialog(
            code = code,
            onApply = { path ->
                viewModel.requestCodeApply(path, code)
                applyCode = null
            },
            onDismiss = { applyCode = null }
        )
    }

    // Feuille de confirmation d'écriture
    viewModel.pendingWrite?.let { pw ->
        WriteConfirmSheet(
            pending = pw,
            onResolve = { allowed, always -> viewModel.resolvePendingWrite(allowed, always) }
        )
    }

    // Feuille de confirmation d'une commande (hors mode Auto)
    viewModel.pendingCommand?.let { pc ->
        CommandConfirmSheet(
            pending = pc,
            onResolve = { allowed -> viewModel.resolvePendingCommand(allowed) }
        )
    }

    // Feuille de confirmation de suppression
    viewModel.pendingDelete?.let { pd ->
        DeleteConfirmSheet(
            pending = pd,
            onResolve = { allowed -> viewModel.resolvePendingDelete(allowed) }
        )
    }

    // Changements Git (ouvert par l'utilisateur)
    if (viewModel.gitSheetOpen) {
        val g = viewModel.gitChanges
        GitChangesSheet(
            status = g?.status.orEmpty(),
            diff = g?.diff.orEmpty(),
            error = g?.error,
            loading = viewModel.gitLoading,
            onRefresh = { viewModel.refreshGitChanges() },
            onClose = { viewModel.gitSheetOpen = false },
            onOpenFull = {
                viewModel.gitSheetOpen = false
                viewModel.isGitScreenOpen = true
            }
        )
    }

    // Modèle de la conversation + Mode Duo (MàJ 102/104)
    if (viewModel.isModelDialogOpen) {
        ModelPickerDialog(
            globalModel = viewModel.modelInput,
            currentChatModel = viewModel.chatModel,
            onSelect = { m ->
                viewModel.selectChatModel(m)
                viewModel.isModelDialogOpen = false
            },
            onDismiss = { viewModel.isModelDialogOpen = false },
            favoris = viewModel.modelesFavoris,
            onToggleFavori = { viewModel.basculeFavori(it) },
            duoMode = viewModel.duoMode,
            duoModeleB = viewModel.duoModeleB,
            onDuoChange = { mode, modeleB -> viewModel.reglerDuo(mode, modeleB) }
        )
    }
}
// Écran d'accueil du chat minimaliste (MA30)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatEmptyState(
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit,
    onPickFolder: () -> Unit,
    onSuggestion: (String) -> Unit
) {
    val c = Zcolors
    val noFolder = viewModel.workingFolderUriString.isNullOrEmpty()
    val noKey = viewModel.apiKeyInput.isBlank()

    when {
        noFolder -> EmptyState(
            icon = Icons.Default.FolderOpen,
            tint = c.amber,
            title = "Aucun projet choisi",
            body = "Crée ou choisis un projet : ZAgent y lira et écrira les fichiers, et le terminal s'y ouvre au même endroit.",
            primaryText = "Choisir un projet",
            onPrimary = onPickFolder,
            secondaryText = "Ouvrir les réglages",
            onSecondary = onOpenSettings
        )
        noKey -> EmptyState(
            icon = Icons.Default.Key,
            tint = c.violet,
            title = "Branche ta première clé API",
            body = "Z.ai, NVIDIA NIM ou ta propre passerelle : colle la clé dans Réglages › Fournisseurs d'IA — une clé par API, détectée automatiquement. Stockage local chiffré.",
            primaryText = "Ouvrir les réglages",
            onPrimary = onOpenSettings
        )
        else -> {
            val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
            val greeting = when {
                hour < 12 -> stringResource(R.string.welcome_morning)
                hour < 18 -> stringResource(R.string.welcome_afternoon)
                else -> stringResource(R.string.welcome_evening)
            }
            val suggestions = listOf(
                stringResource(R.string.suggestion_fix_bug),
                stringResource(R.string.suggestion_create_screen),
                stringResource(R.string.suggestion_explain_file),
                stringResource(R.string.suggestion_write_tests)
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(56.dp))
                Text(
                    "$greeting, Ki",
                    style = TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, fontSize = 30.sp, fontWeight = FontWeight.Medium),
                    color = c.text
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.welcome_subtitle),
                    style = TextStyle(fontFamily = Inter, fontSize = 15.sp),
                    color = c.text3
                )
                Spacer(Modifier.height(32.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SuggestionChipView(suggestions[0], onSuggestion)
                        SuggestionChipView(suggestions[1], onSuggestion)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SuggestionChipView(suggestions[2], onSuggestion)
                        SuggestionChipView(suggestions[3], onSuggestion)
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionChipView(label: String, onSuggestion: (String) -> Unit) {
    val c = Zcolors
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(c.surface2)
            .clickable { onSuggestion(label) }
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(label, style = TextStyle(fontFamily = Inter, fontSize = 13.sp), color = c.text2)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Rendu des messages
// ─────────────────────────────────────────────────────────────────────────────

private sealed interface ChatRow {
    val key: String

    data class Date(val id: Int, val label: String) : ChatRow {
        override val key: String get() = "date_$id"
    }

    data class User(val id: Int, val text: String, val messageIndex: Int) : ChatRow {
        override val key: String get() = "u_$id"
    }

    data class Agent(
        val id: Int,
        val content: String?,
        val tools: List<Pair<AiRepository.ToolCallData, String?>>,
        val messageIndex: Int,
        val reasoning: String?,
        val modele: String? = null
    ) : ChatRow {
        override val key: String get() = "a_$id"
    }

    data class Ask(
        val id: Int,
        val toolCallId: String,
        val questions: List<AskQuestion>,
        val result: String?
    ) : ChatRow {
        override val key: String get() = "q_$id"
    }

    data class OrphanTool(val id: Int, val text: String) : ChatRow {
        override val key: String get() = "t_$id"
    }
}

private fun dayLabelOf(time: Long, dayFmt: SimpleDateFormat): String {
    val d = Calendar.getInstance().apply { timeInMillis = time }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        d.get(Calendar.YEAR) == today.get(Calendar.YEAR) && d.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Aujourd'hui"
        d.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) && d.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "Hier"
        else -> dayFmt.format(Date(time))
    }
}

private fun buildChatRows(messages: List<com.zagent.app.ui.viewmodel.ChatMessage>): List<ChatRow> {
    val rows = mutableListOf<ChatRow>()
    var currentTools: MutableList<Pair<AiRepository.ToolCallData, String?>>? = null
    var nextId = 0
    val askRowIndex = HashMap<String, Int>()
    val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dayFmt = SimpleDateFormat("d MMMM", Locale.getDefault())
    var lastDay = ""

    fun ensureDate(time: Long, last: String): String {
        val label = dayLabelOf(time, dayFmt)
        if (label != last) rows.add(ChatRow.Date(nextId++, label))
        return label
    }

    messages.forEachIndexed { msgIndex, m ->
        when (m.role) {
            "user" -> {
                currentTools = null
                lastDay = ensureDate(m.time, lastDay)
                rows.add(ChatRow.User(nextId++, m.content ?: "", msgIndex))
            }
            "assistant" -> {
                val list = mutableListOf<Pair<AiRepository.ToolCallData, String?>>()
                currentTools = list
                val calls = m.toolCalls.orEmpty()
                val visibleTools = calls.filter { it.name != "ask_user" && it.name != "update_todos" }
                if (!m.content.isNullOrBlank() || visibleTools.isNotEmpty() || calls.isEmpty()) {
                    lastDay = ensureDate(m.time, lastDay)
                    rows.add(ChatRow.Agent(nextId++, m.content, list, msgIndex, m.reasoning, m.modele))
                }
                visibleTools.forEach { tc -> list.add(tc to null) }
                calls.filter { it.name == "ask_user" }.forEach { tc ->
                    val qs = parseAskQuestions(tc.arguments)
                    if (qs.isNotEmpty()) {
                        askRowIndex[tc.id] = rows.size
                        rows.add(ChatRow.Ask(nextId++, tc.id, qs, null))
                    }
                }
            }
            "tool" -> {
                if (m.name == "ask_user") {
                    val idx = askRowIndex[m.toolCallId]
                    val old = if (idx != null) rows[idx] as? ChatRow.Ask else null
                    if (idx != null && old != null) rows[idx] = old.copy(result = m.content ?: "")
                } else if (m.name == "update_todos") {
                    // résultat masqué : la liste de tâches est affichée en haut du chat
                } else {
                    val tools = currentTools
                    if (tools != null) {
                        val idx = tools.indexOfFirst { it.first.id == m.toolCallId }
                        if (idx >= 0) tools[idx] = tools[idx].copy(second = m.content)
                        else rows.add(ChatRow.OrphanTool(nextId++, "${m.toolLog ?: "outil"} → ${(m.content ?: "").take(120)}"))
                    } else {
                        rows.add(ChatRow.OrphanTool(nextId++, "${m.toolLog ?: "outil"} → ${(m.content ?: "").take(120)}"))
                    }
                }
            }
        }
    }
    return rows
}
/**
 * MàJ 89 — entrée animée d'un nouvel élément du fil : fondu + glissement de
 * 8 dp en 200 ms. Les éléments de l'historique (activee = false) s'affichent
 * sans animation. Respecte « Réduire les animations » (MàJ 95 : fondu court).
 */
@Composable
private fun EntreeMessage(
    activee: Boolean,
    modifier: Modifier = Modifier,
    contenu: @Composable () -> Unit
) {
    val glissementPx = with(LocalDensity.current) {
        ZMouvement.GLISSEMENT_MESSAGE_DP.dp.roundToPx()
    }
    val etat = remember { MutableTransitionState(!activee).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = etat,
        enter = if (LocalReduceMotion.current) {
            fadeIn(tween(ZMouvement.FONDU_REDUIT_MS))
        } else {
            fadeIn(tween(ZMouvement.ENTREE_MESSAGE_MS, easing = LinearOutSlowInEasing)) +
                slideInVertically(
                    tween(ZMouvement.ENTREE_MESSAGE_MS, easing = LinearOutSlowInEasing)
                ) { glissementPx }
        },
        modifier = modifier
    ) {
        contenu()
    }
}
