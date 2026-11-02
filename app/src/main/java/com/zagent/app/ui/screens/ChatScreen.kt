package com.zagent.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
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
import androidx.compose.material.icons.filled.MoreHoriz
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.zagent.app.ui.components.DevServerBar
import com.zagent.app.ui.components.GitChangesSheet
import com.zagent.app.ui.components.GitDiffView
import com.zagent.app.ui.components.ModelPickerDialog
import com.zagent.app.ui.components.CodeBlock
import com.zagent.app.ui.components.DiffPanel
import com.zagent.app.ui.components.ToolChip
import com.zagent.app.ui.components.ToolState
import com.zagent.app.ui.components.buildInlineMarkdown
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.SpaceGrotesk
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.theme.LocalGlass
import com.zagent.app.ui.theme.LocalHazeState
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
    val messages by viewModel.messages.collectAsState()
    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var input by rememberSaveable { mutableStateOf("") }
    var editingIndex by rememberSaveable { mutableStateOf(-1) }
    var applyCode by remember { mutableStateOf<String?>(null) }

    val chatRows = remember(messages) { buildChatRows(messages) }
    val lastFailedCmdId = remember(messages) {
        val m = messages.lastOrNull { it.role == "tool" && it.name == "run_command" }
        val r = m?.content.orEmpty()
        if (m != null && (r.startsWith("Erreur: code") || r.startsWith("Erreur: délai"))) m.toolCallId else null
    }

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

    val onFileTap: (String, Int?) -> Unit = { path, line -> viewModel.openFilePath(path, line) }
    val thinkingSeconds = ((nowTick - viewModel.thinkingStartedAt) / 1000).toInt().coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        AmbientTopBar(
            viewModel = viewModel,
            scrolled = listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 8,
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
                        onSuggestion = { input = it; editingIndex = -1 }
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
                                        UserBubble(
                                            text = row.text,
                                            messageIndex = row.messageIndex,
                                            onEdit = { i, t -> input = t; editingIndex = i },
                                            onQuote = { t -> input = "> " + t.replace("\n", "\n> ") + "\n\n" },
                                            onDelete = { viewModel.deleteFromMessage(it) }
                                        )
                                    }
                                    is ChatRow.Agent -> item(key = row.key, contentType = "agent") {
                                        val isLastAgent = index == chatRows.lastIndex && displayLive.isBlank()
                                        AgentCard(
                                            content = row.content,
                                            tools = row.tools,
                                            showLogo = chatRows.getOrNull(index - 1) !is ChatRow.Agent,
                                            reasoning = row.reasoning,
                                            thinkingSeconds = -1,
                                            isLast = isLastAgent && !viewModel.isLoading,
                                            messageIndex = row.messageIndex,
                                            onFileTap = onFileTap,
                                            onApplyCode = { applyCode = it },
                                            onRegenerate = { viewModel.regenerateLast() },
                                            onQuote = { t -> input = "> " + t.replace("\n", "\n> ") + "\n\n" },
                                            onDeleteMessage = { viewModel.deleteFromMessage(row.messageIndex) },
                                            fixId = if (viewModel.isLoading) null else lastFailedCmdId,
                                            onFix = { cmd, res -> viewModel.fixCommandWithAi(cmd, res) },
                                            liveId = viewModel.liveCommandId,
                                            liveOutput = { viewModel.liveOutput },
                                            liveStartedAt = viewModel.liveStartedAt,
                                            onStop = { viewModel.stopLiveCommand() }
                                        )
                                    }
                                    is ChatRow.OrphanTool -> item(key = row.key, contentType = "orphan") {
                                        OrphanToolLine(row.text)
                                    }
                                    is ChatRow.Ask -> item(key = row.key, contentType = "ask") {
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
                        Text("Nouveau message", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.5.sp), color = c.text)
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
                    viewModel.statusMessage.ifBlank { "L'agent travaille" },
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
                        "Ouvrir l'aperçu",
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
                    "Annuler " + viewModel.agentUndo.size + " changement(s)",
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
                    "Modification d'un message — l'envoi remplacera la suite",
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
                        .clickable { editingIndex = -1; input = "" }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        InputBar(
            value = input,
            onValueChange = { input = it },
            onSend = {
                val t = input
                input = ""
                if (editingIndex >= 0) {
                    val idx = editingIndex
                    editingIndex = -1
                    viewModel.editAndResend(idx, t)
                } else {
                    viewModel.sendMessage(t)
                }
            },
            onOpenExplorer = { viewModel.navigateTo(com.zagent.app.ui.viewmodel.AppScreen.FILE_BROWSER) },
            enabled = !viewModel.isLoading
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
            onClose = { viewModel.gitSheetOpen = false }
        )
    }

    // Modèle de la conversation
    if (viewModel.isModelDialogOpen) {
        ModelPickerDialog(
            globalModel = viewModel.modelInput,
            currentChatModel = viewModel.chatModel,
            onSelect = { m ->
                viewModel.selectChatModel(m)
                viewModel.isModelDialogOpen = false
            },
            onDismiss = { viewModel.isModelDialogOpen = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Barre supérieure
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AmbientTopBar(
    viewModel: MainViewModel,
    scrolled: Boolean,
    onNewConversation: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val c = Zcolors
    Column(
        Modifier
            .fillMaxWidth()
            .then(
                if (scrolled) Modifier.glassSurface(
                    LocalHazeState.current,
                    LocalGlass.current,
                    c.isDark,
                    RoundedCornerShape(24.dp),
                    LocalContext.current
                ) else Modifier
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ZLogo(modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "ZAgent",
                    style = TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, fontSize = 20.sp, fontWeight = FontWeight.Medium),
                    color = c.text,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (dot, label) = when {
                        viewModel.isLoading -> c.cyan to "Agent actif"
                        viewModel.hasBlockingError -> c.red to "Erreur"
                        else -> c.green to "En ligne"
                    }
                    Box(
                        Modifier
                            .size(7.dp)
                            .background(dot, CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(label, style = TextStyle(fontFamily = Inter, fontSize = 11.sp, fontWeight = FontWeight.Medium), color = dot)
                    Spacer(Modifier.width(8.dp))
                    MonoChip(
                        viewModel.effectiveModel,
                        modifier = Modifier
                            .widthIn(max = 160.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable { viewModel.isModelDialogOpen = true }
                    )
                }
                UsageLine(viewModel.totalTokens, viewModel.tokensEstimated, viewModel.estimatedCost)
            }
            Spacer(Modifier.width(10.dp))
            // Groupe de 3 actions (40 dp) dans une pilule de verre.
            Row(
                Modifier
                    .glassSurface(LocalHazeState.current, LocalGlass.current, c.isDark, RoundedCornerShape(50.dp), LocalContext.current)
                    .background(c.surface2.copy(alpha = if (LocalGlass.current.enabled) 0.12f else 1f), RoundedCornerShape(50.dp))
                    .padding(horizontal = 5.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TopBarPillIcon(Icons.Default.Refresh, "Réinitialiser les discussions", onClick = onNewConversation)
                TopBarPillIcon(Icons.Default.FolderOpen, "Explorateur de fichiers", onClick = { viewModel.navigateTo(com.zagent.app.ui.viewmodel.AppScreen.FILE_BROWSER) })
                TopBarPillIcon(Icons.Default.Settings, "Réglages", onClick = onOpenSettings)
            }
        }
    }
}

/** Icône ronde de 40 dp du groupe d'actions de la barre du haut. */
@Composable
private fun TopBarPillIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = Zcolors.text, modifier = Modifier.size(20.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Toast discret en pilule de verre (MA43)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GlassToastPill(viewModel: MainViewModel, onOpenSettings: () -> Unit) {
    val c = Zcolors
    val blocking = viewModel.hasBlockingError || viewModel.stepLimitReached
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(viewModel.statusMessage, blocking) {
        visible = true
        if (!blocking && viewModel.statusMessage.isNotBlank()) {
            delay(4000)
            visible = false
        }
    }
    val data: ToastData? = when {
        viewModel.isKeyInvalid -> ToastData(Icons.Default.Key, c.red, "Clé API manquante", "Réglages" to onOpenSettings)
        viewModel.isNetworkError -> ToastData(Icons.Default.WifiOff, c.amber, "Hors ligne", "Réessayer" to { viewModel.retryLast() })
        viewModel.showFallbackModelButton || viewModel.isCode1305Error ->
            ToastData(Icons.Default.Bolt, c.amber, "Modèle surchargé", "Autre modèle" to { viewModel.switchToOtherModel() })
        viewModel.stepLimitReached -> ToastData(Icons.Default.Bolt, c.amber, "Limite d'étapes atteinte", "Continuer" to { viewModel.continueAgent() })
        !viewModel.isLoading && viewModel.statusMessage.isNotBlank() ->
            ToastData(Icons.Default.ErrorOutline, c.amber, viewModel.statusMessage.take(60), "Réessayer" to { viewModel.retryLast() })
        else -> null
    }
    AnimatedVisibility(visible = visible && data != null) {
        data?.let { t ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .glassSurface(LocalHazeState.current, LocalGlass.current, c.isDark, RoundedCornerShape(50.dp), LocalContext.current)
                    .background(c.surface2.copy(alpha = if (LocalGlass.current.enabled) 0.12f else 1f), RoundedCornerShape(50.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(t.icon, null, tint = t.tint, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    t.text,
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.5.sp),
                    color = c.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                t.action?.let { (label, onClick) ->
                    Text(
                        label,
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                        color = c.cyan,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraSmall)
                            .clickable(onClick = onClick)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/** Notification courte affichée dans la pilule de verre (MA43). */
private data class ToastData(val icon: ImageVector, val tint: Color, val text: String, val action: Pair<String, () -> Unit>?)

/** Relances automatiques en cas de perte de réseau (logique conservée de l'ancienne bannière). */
@Composable
private fun AutoRetryEffect(viewModel: MainViewModel) {
    var countdown by remember { mutableStateOf(30) }
    LaunchedEffect(Unit) {
        while (viewModel.autoRetryCount < 3) {
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
            viewModel.retryLast(auto = true)
            countdown = 30
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Erreur humaine dans le fil, avec actions (MA47)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InlineErrorCard(viewModel: MainViewModel, onOpenSettings: () -> Unit) {
    val c = Zcolors
    val title = when {
        viewModel.isKeyInvalid -> "La clé API a été refusée"
        viewModel.isNetworkError -> "Connexion indisponible"
        viewModel.showFallbackModelButton || viewModel.isCode1305Error -> "Le modèle est surchargé"
        viewModel.stepLimitReached -> "Limite d'étapes atteinte"
        else -> "Une erreur est survenue"
    }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        backgroundColor = c.redBg,
        borderColor = c.red.copy(alpha = 0.30f),
        shape = MaterialTheme.shapes.large,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            IconBadge(Icons.Default.ErrorOutline, c.red)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
                    color = c.text
                )
                if (viewModel.statusMessage.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        viewModel.statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.text2
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GradientButton(
                        "Réessayer",
                        onClick = { viewModel.retryLast() },
                        modifier = Modifier.weight(1f, fill = false),
                        height = 38.dp
                    )
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onOpenSettings)
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Text(
                            "Ouvrir les Réglages",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                            color = c.text2
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
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
            title = "Branche ta clé API Z.ai",
            body = "Colle ta clé Z.ai dans les réglages pour activer l'agent. Elle est stockée localement, chiffrée sur l'appareil.",
            primaryText = "Ouvrir les réglages",
            onPrimary = onOpenSettings
        )
        else -> {
            val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
            val greeting = when {
                hour < 12 -> "Bonjour"
                hour < 18 -> "Bon après-midi"
                else -> "Bonsoir"
            }
            val suggestions = listOf("Corriger un bug", "Créer un écran", "Expliquer ce fichier", "Écrire des tests")
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
                    "Sur quoi travaillons-nous ?",
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
        val reasoning: String?
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
                    rows.add(ChatRow.Agent(nextId++, m.content, list, msgIndex, m.reasoning))
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

/** En-tête de date collant, petite pastille de verre (MA39). */
@Composable
private fun DatePill(label: String, modifier: Modifier = Modifier) {
    val c = Zcolors
    Box(
        modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = TextStyle(fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.Medium),
            color = c.text2,
            modifier = Modifier
                .glassSurface(LocalHazeState.current, LocalGlass.current, c.isDark, RoundedCornerShape(50.dp), LocalContext.current)
                .background(c.surface2.copy(alpha = if (LocalGlass.current.enabled) 0.12f else 1f), RoundedCornerShape(50.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

/** Bulle utilisateur discrète : fond surface, rayon 20 dp, 85 % de largeur max (MA29/MA36/MA37). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UserBubble(
    text: String,
    messageIndex: Int,
    onEdit: (Int, String) -> Unit,
    onQuote: (String) -> Unit,
    onDelete: (Int) -> Unit
) {
    val c = Zcolors
    val clipboard = LocalClipboardManager.current
    val ctx = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Box(
            Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(20.dp))
                .background(c.surface2)
                .combinedClickable(onClick = {}, onLongClick = { menuOpen = true })
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    buildInlineMarkdown(text, baseColor = c.text, codeColor = c.cyan, codeBg = c.cyanBg),
                    style = TextStyle(fontFamily = Inter, fontSize = 15.sp, lineHeight = 22.sp),
                    color = c.text
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Modifier le message",
                        tint = c.text3,
                        modifier = Modifier
                            .size(13.dp)
                            .clickable { onEdit(messageIndex, text) }
                    )
                }
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Copier le texte", color = c.text) },
                    onClick = { menuOpen = false; clipboard.setText(AnnotatedString(text)) }
                )
                DropdownMenuItem(
                    text = { Text("Sélectionner", color = c.text) },
                    onClick = {
                        menuOpen = false
                        Toast.makeText(ctx, "Maintiens ton doigt sur le texte pour le sélectionner", Toast.LENGTH_SHORT).show()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Citer", color = c.text) },
                    onClick = { menuOpen = false; onQuote(text) }
                )
                DropdownMenuItem(
                    text = { Text("Supprimer", color = c.red) },
                    onClick = { menuOpen = false; onDelete(messageIndex) }
                )
            }
        }
    }
}

@Composable
private fun OrphanToolLine(text: String) {
    val c = Zcolors
    Text(
        text,
        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, lineHeight = 15.sp),
        color = c.text3,
        modifier = Modifier.padding(start = 8.dp)
    )
}

/** Découpe le contenu en segments texte / blocs de code. */
private data class ContentSegment(val isCode: Boolean, val lang: String?, val text: String)

private fun splitContent(content: String): List<ContentSegment> {
    val segments = mutableListOf<ContentSegment>()
    val fence = Regex("```(\\w+)?\\n?([\\s\\S]*?)(?:```|$)")
    var last = 0
    for (m in fence.findAll(content)) {
        if (m.range.first > last) {
            segments.add(ContentSegment(false, null, content.substring(last, m.range.first)))
        }
        segments.add(ContentSegment(true, m.groupValues[1].ifBlank { null }, m.groupValues[2]))
        last = m.range.last + 1
    }
    if (last < content.length) segments.add(ContentSegment(false, null, content.substring(last)))
    return segments
}

/** Curseur pulsé en fin de génération (MA33). */
@Composable
private fun StreamingCursor() {
    val c = Zcolors
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "curseur")
    val alpha by transition.animateFloat(
        initialValue = 0.15f, targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(600),
            androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulse"
    )
    Text("●", color = c.cyan.copy(alpha = alpha), fontSize = 15.sp, modifier = Modifier.padding(start = 2.dp))
}

/** Réponse de l'agent : texte pleine largeur sur le fond, sans bulle (MA29). */
@Composable
private fun AgentCard(
    content: String?,
    tools: List<Pair<AiRepository.ToolCallData, String?>>,
    modifier: Modifier = Modifier,
    showLogo: Boolean = false,
    isStreaming: Boolean = false,
    reasoning: String? = null,
    thinkingSeconds: Int = 0,
    isLast: Boolean = false,
    messageIndex: Int = -1,
    onFileTap: (String, Int?) -> Unit = { _, _ -> },
    onApplyCode: ((String) -> Unit)? = null,
    onRegenerate: () -> Unit = {},
    onQuote: (String) -> Unit = {},
    onDeleteMessage: () -> Unit = {},
    fixId: String? = null,
    onFix: (String, String) -> Unit = { _, _ -> },
    liveId: String? = null,
    liveOutput: () -> String = { "" },
    liveStartedAt: Long = 0L,
    onStop: () -> Unit = {}
) {
    val c = Zcolors
    var actionsRevealed by remember(messageIndex) { mutableStateOf(false) }
    var toolsOpen by remember(messageIndex) { mutableStateOf(tools.size < 4) }
    val grouped = tools.size >= 4

    Column(modifier = modifier.fillMaxWidth().widthIn(max = 680.dp)) {
        // Petit logo au-dessus du premier message d'une série (MA29)
        if (showLogo) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                ZLogo(modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "ZAgent",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp),
                    color = c.text3
                )
            }
        }

        // MA35 : bloc « Réflexion »
        ThinkingBlock(
            reasoning = reasoning ?: "",
            running = isStreaming && !reasoning.isNullOrBlank(),
            seconds = thinkingSeconds
        )

        if (!content.isNullOrBlank()) {
            splitContent(content).forEachIndexed { i, seg ->
                if (seg.isCode) {
                    CodeBlockFor(seg, onApply = onApplyCode)
                } else if (seg.text.isNotBlank()) {
                    MarkdownText(
                        seg.text.trim(),
                        fontSize = 16.sp,
                        lineHeight = 26.sp,
                        onFileTap = onFileTap
                    )
                }
                if (i < (tools.size + 1)) Spacer(Modifier.height(10.dp))
            }
            if (isStreaming) StreamingCursor()
        }

        // MA34 : outils compacts, regroupés au-delà de 4 appels
        if (grouped && !toolsOpen) {
            ToolGroupSummary(tools.size, expanded = false, onToggle = { toolsOpen = true })
        } else {
            tools.forEachIndexed { tIdx, (tc, result) ->
                if (tc.name == "run_command") {
                    CommandBlock(
                        tc, result,
                        canFix = tc.id == fixId,
                        onFix = onFix,
                        isLive = result == null && tc.id == liveId,
                        liveOutput = liveOutput,
                        liveStartedAt = liveStartedAt,
                        onStop = onStop
                    )
                } else {
                    ToolChip(
                        name = tc.name,
                        arg = toolArgShort(tc.arguments),
                        state = toolStateOf(result),
                        resultText = result,
                        expandKey = tc.id
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (grouped && tIdx == tools.lastIndex) {
                    ToolGroupSummary(tools.size, expanded = true, onToggle = { toolsOpen = false })
                }
            }
        }

        // MA36 : barre d'actions sous la réponse
        if (!isStreaming && !content.isNullOrBlank()) {
            MessageActionsBar(
                messageKey = "msg_$messageIndex",
                messageText = content,
                full = isLast || actionsRevealed,
                canRegenerate = isLast,
                onToggle = { actionsRevealed = true },
                onRegenerate = onRegenerate,
                onQuote = onQuote,
                onDelete = onDeleteMessage
            )
        }
    }
}

/** Icône d'action discrète (MA36). */
@Composable
private fun ActionIcon(icon: ImageVector, label: String, tint: Color = Zcolors.text3, onClick: () -> Unit) {
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(15.dp))
    }
}

/** Copier · Régénérer · 👍 · 👎 · Partager + menu contextuel (MA36). */
@Composable
private fun MessageActionsBar(
    messageKey: String,
    messageText: String,
    full: Boolean,
    canRegenerate: Boolean,
    onToggle: () -> Unit,
    onRegenerate: () -> Unit,
    onQuote: (String) -> Unit,
    onDelete: () -> Unit
) {
    val c = Zcolors
    val clipboard = LocalClipboardManager.current
    val ctx = LocalContext.current
    var feedback by rememberSaveable(messageKey) { mutableStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (!full) {
            ActionIcon(Icons.Default.MoreHoriz, "Actions") { onToggle() }
        } else {
            ActionIcon(Icons.Default.ContentCopy, "Copier") { clipboard.setText(AnnotatedString(messageText)) }
            if (canRegenerate) ActionIcon(Icons.Default.Refresh, "Régénérer") { onRegenerate() }
            ActionIcon(
                Icons.Default.ThumbUp, "Utile",
                tint = if (feedback == 1) c.green else c.text3
            ) { feedback = if (feedback == 1) 0 else 1 }
            ActionIcon(
                Icons.Default.ThumbDown, "Pas utile",
                tint = if (feedback == -1) c.red else c.text3
            ) { feedback = if (feedback == -1) 0 else -1 }
            ActionIcon(Icons.Default.Share, "Partager") {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, messageText)
                }
                runCatching { ctx.startActivity(Intent.createChooser(send, "Partager le message")) }
            }
            ActionIcon(Icons.Default.MoreHoriz, "Plus") { menuOpen = !menuOpen }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Citer", color = c.text) }, onClick = { menuOpen = false; onQuote(messageText) })
                DropdownMenuItem(
                    text = { Text("Sélectionner", color = c.text) },
                    onClick = {
                        menuOpen = false
                        Toast.makeText(ctx, "Maintiens ton doigt sur le texte pour le sélectionner", Toast.LENGTH_SHORT).show()
                    }
                )
                DropdownMenuItem(text = { Text("Supprimer", color = c.red) }, onClick = { menuOpen = false; onDelete() })
            }
        }
        if (feedback != 0) {
            Spacer(Modifier.width(6.dp))
            Text(
                if (feedback == 1) "Merci pour ton retour" else "Noté",
                style = TextStyle(fontFamily = Inter, fontSize = 11.sp),
                color = c.text3
            )
        }
    }
}

/** Commande de l'agent : reste visible dans le message, repliable, avec sa sortie complète. */
@Composable
private fun CommandBlock(
    tc: AiRepository.ToolCallData,
    result: String?,
    canFix: Boolean,
    onFix: (String, String) -> Unit,
    isLive: Boolean = false,
    liveOutput: () -> String = { "" },
    liveStartedAt: Long = 0L,
    onStop: () -> Unit = {}
) {
    val c = Zcolors
    val command = try {
        (kotlinx.serialization.json.Json.parseToJsonElement(tc.arguments) as? kotlinx.serialization.json.JsonObject)
            ?.get("command")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
    } catch (_: Exception) {
        null
    } ?: ""
    val state = toolStateOf(result)
    var expanded by rememberSaveable(tc.id) { mutableStateOf(true) }
    val head = result?.substringBefore('\n').orEmpty()
    val output = remember(result) {
        tailForDisplay(result?.substringAfter('\n', "")?.trimEnd().orEmpty(), 12000)
    }
    val statusColor = when (state) {
        ToolState.OK -> c.green
        ToolState.ERROR -> c.red
        ToolState.REFUSED -> c.amber
        ToolState.RUNNING -> c.cyan
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(14.dp))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = result != null) { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$ $command",
                style = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
                color = c.cyan,
                maxLines = if (expanded) 6 else 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            when (state) {
                ToolState.RUNNING -> CircularProgressIndicator(
                    modifier = Modifier.size(13.dp), strokeWidth = 2.dp, color = c.cyan
                )
                ToolState.OK -> Icon(Icons.Default.Check, null, tint = c.green, modifier = Modifier.size(14.dp))
                ToolState.ERROR -> Icon(Icons.Default.Close, null, tint = c.red, modifier = Modifier.size(14.dp))
                ToolState.REFUSED -> Text("refusé", color = c.amber, fontSize = 12.sp)
            }
            if (result != null) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Détail",
                    tint = c.text3,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        if (isLive) {
            var now by remember { mutableStateOf(System.currentTimeMillis()) }
            LaunchedEffect(liveStartedAt) {
                while (true) {
                    now = System.currentTimeMillis()
                    kotlinx.coroutines.delay(500)
                }
            }
            val secs = ((now - liveStartedAt) / 1000).coerceAtLeast(0)
            val liveScroll = rememberScrollState()
            val shownLive = remember(liveOutput()) { tailForDisplay(liveOutput(), 6000) }
            LaunchedEffect(shownLive) { liveScroll.scrollTo(liveScroll.maxValue) }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "En cours : " + secs + " s",
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp),
                    color = c.text3,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Arrêter",
                    color = c.red,
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(c.redBg)
                        .clickable(onClick = onStop)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .background(c.surface2)
                    .verticalScroll(liveScroll)
                    .padding(10.dp)
            ) {
                AnsiText(
                    text = shownLive.ifEmpty { "..." },
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, lineHeight = 15.sp),
                    baseColor = c.text2,
                    linkColor = c.cyan
                )
            }
        }
        if (expanded && result != null) {
            if (head.isNotBlank()) {
                Text(
                    head,
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp),
                    color = statusColor,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 6.dp)
                )
            }
            if (output.isNotEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .background(c.surface2)
                        .verticalScroll(rememberScrollState())
                        .padding(10.dp)
                ) {
                    AnsiText(
                        text = output,
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, lineHeight = 15.sp),
                        baseColor = c.text2,
                        linkColor = c.cyan
                    )
                }
            }
        }
        if (canFix && result != null) {
            Text(
                "Corriger avec l'IA",
                color = c.cyan,
                fontSize = 12.sp,
                modifier = Modifier
                    .padding(10.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(c.cyanBg)
                    .clickable { onFix(command, result) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

/** Liste de tâches : anneau de progression repliable (MA40). */
@Composable
private fun TodoPanel(todos: List<TodoItem>, isLoading: Boolean) {
    if (todos.isEmpty()) return
    val c = Zcolors
    var open by rememberSaveable { mutableStateOf(false) }
    val done = todos.count { it.done }
    val progress = done / todos.size.toFloat()
    val animatedProgress by animateFloatAsState(progress, label = "todos")
    val currentIdx = todos.indexOfFirst { !it.done }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(14.dp))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TodoRing(animatedProgress)
            Spacer(Modifier.width(10.dp))
            Text(
                "Tâches  $done/${todos.size}",
                style = TextStyle(fontFamily = Inter, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                color = c.text,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = c.text3,
                modifier = Modifier.size(18.dp)
            )
        }
        AnimatedVisibility(visible = open) {
            Column(
                Modifier
                    .heightIn(max = 180.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                todos.forEachIndexed { idx, t ->
                    val checkProgress by animateFloatAsState(if (t.done) 1f else 0f, label = "check")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(16.dp)
                                .scale(0.7f + 0.3f * checkProgress)
                                .clip(RoundedCornerShape(5.dp))
                                .background(c.green.copy(alpha = checkProgress))
                                .border(1.5.dp, if (checkProgress > 0.5f) c.green else c.border, RoundedCornerShape(5.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (checkProgress > 0.5f) {
                                Icon(Icons.Default.Check, null, tint = c.bg, modifier = Modifier.size(12.dp).alpha(checkProgress))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            t.text,
                            style = TextStyle(
                                fontFamily = Inter,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                textDecoration = if (t.done) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = if (t.done) c.text3 else c.text,
                            modifier = Modifier.weight(1f)
                        )
                        if (isLoading && idx == currentIdx) {
                            val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "encours")
                            val pulse by transition.animateFloat(
                                initialValue = 0.25f, targetValue = 1f,
                                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                    androidx.compose.animation.core.tween(700),
                                    androidx.compose.animation.core.RepeatMode.Reverse
                                ),
                                label = "point"
                            )
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .background(c.cyan.copy(alpha = pulse), CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Anneau de progression animé (MA40). */
@Composable
private fun TodoRing(progress: Float, ringSize: Dp = 24.dp) {
    val c = Zcolors
    Canvas(Modifier.size(ringSize)) {
        val stroke = 3.dp.toPx()
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(
            color = c.border2, startAngle = -90f, sweepAngle = 360f, useCenter = false,
            style = Stroke(stroke, cap = StrokeCap.Round),
            topLeft = Offset(stroke / 2, stroke / 2), size = arcSize
        )
        drawArc(
            color = c.cyan, startAngle = -90f, sweepAngle = 360f * progress.coerceIn(0f, 1f), useCenter = false,
            style = Stroke(stroke, cap = StrokeCap.Round),
            topLeft = Offset(stroke / 2, stroke / 2), size = arcSize
        )
    }
}

/** Récapitulatif de fin de tâche avec compteurs de diff (MA41). */
@Composable
private fun TaskSummary(
    files: List<String>,
    commands: List<String>,
    stats: Map<String, Pair<Int, Int>>,
    hasUndo: Boolean,
    onViewDiff: () -> Unit,
    onUndo: () -> Unit
) {
    val c = Zcolors
    var open by rememberSaveable { mutableStateOf(false) }
    val totalAdded = stats.values.sumOf { it.first }
    val totalRemoved = stats.values.sumOf { it.second }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(14.dp))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Récapitulatif · " + files.size + " fichier(s) · +" + totalAdded + " −" + totalRemoved,
                style = TextStyle(fontFamily = Inter, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                color = c.text,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = c.text3,
                modifier = Modifier.size(18.dp)
            )
        }
        AnimatedVisibility(visible = open) {
            Column(
                Modifier
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                files.forEach { f ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "fichier  $f",
                            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, lineHeight = 15.sp),
                            color = c.text2,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("+${stats[f]?.first ?: 0}", style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp), color = c.green)
                        Spacer(Modifier.width(6.dp))
                        Text("−${stats[f]?.second ?: 0}", style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp), color = c.red)
                    }
                }
                commands.forEach {
                    Text(
                        "$ $it",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, lineHeight = 15.sp),
                        color = c.cyan
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Voir le diff",
                        color = c.cyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(c.cyanBg)
                            .clickable(onClick = onViewDiff)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                    if (hasUndo) {
                        Text(
                            "Annuler les modifications",
                            color = c.amber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(c.amberBg)
                                .clickable(onClick = onUndo)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

/** MA32 : appliquer un bloc de code à un fichier du projet. */
@Composable
private fun ApplyCodeDialog(code: String, onApply: (String) -> Unit, onDismiss: () -> Unit) {
    val c = Zcolors
    var path by remember { mutableStateOf("") }
    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Appliquer au fichier", fontFamily = SpaceGrotesk, color = c.text) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    "Le contenu du bloc remplacera tout le fichier (le fichier est créé s'il n'existe pas). Chemin relatif au projet.",
                    style = TextStyle(fontFamily = Inter, fontSize = 12.5.sp),
                    color = c.text2
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    placeholder = { Text("chemin/du/fichier.kt", fontFamily = JetBrainsMono, color = c.text3) },
                    textStyle = TextStyle(fontFamily = JetBrainsMono, color = c.text, fontSize = 13.sp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    code.lines().size.toString() + " lignes · " + code.length + " caractères",
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp),
                    color = c.text3
                )
            }
        },
        confirmButton = {
            TextButton(enabled = path.isNotBlank(), onClick = { onApply(path.trim()) }) {
                Text("Appliquer", color = if (path.isNotBlank()) c.cyan else c.text3)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = c.text2) }
        }
    )
}

@Composable
private fun CodeBlockFor(seg: ContentSegment, onApply: ((String) -> Unit)? = null) {
    CodeBlock(
        path = null,
        language = seg.lang,
        code = seg.text.trimEnd(),
        onApply = onApply
    )
}

private fun toolArgShort(argumentsJson: String): String? = try {
    val obj = kotlinx.serialization.json.Json.parseToJsonElement(argumentsJson)
        .let { it as? kotlinx.serialization.json.JsonObject }
    (obj?.get("path") ?: obj?.get("command"))?.let { p ->
        (p as? kotlinx.serialization.json.JsonPrimitive)?.content
    }
} catch (_: Exception) {
    null
}

private fun toolStateOf(result: String?): ToolState = when {
    result == null -> ToolState.RUNNING
    result.startsWith("Refusé") -> ToolState.REFUSED
    result.startsWith("Erreur") || result.startsWith("Erreur d'écriture") -> ToolState.ERROR
    else -> ToolState.OK
}

// ─────────────────────────────────────────────────────────────────────────────
// Barre de saisie flottante
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onOpenExplorer: () -> Unit,
    enabled: Boolean
) {
    val c = Zcolors
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 88.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(c.surface2.copy(alpha = if (LocalGlass.current.enabled) 0.10f else 1f))
                .glassSurface(LocalHazeState.current, LocalGlass.current, c.isDark, RoundedCornerShape(50.dp), context)
                .border(1.dp, c.border2.copy(alpha = 0.5f), RoundedCornerShape(50))
                .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenExplorer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, "Explorateur", tint = c.text2, modifier = Modifier.size(20.dp))
            }
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        "Décris ta tâche…",
                        style = TextStyle(fontFamily = Inter, fontSize = 15.sp),
                        color = c.text3
                    )
                },
                textStyle = TextStyle(fontFamily = Inter, fontSize = 15.sp, color = c.text),
                maxLines = 6,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Send
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSend = {
                        if (enabled && value.isNotBlank()) {
                            onSend()
                            keyboardController?.hide()
                        }
                    }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = c.cyan
                ),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp, max = 116.dp)
            )
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .then(if (enabled && value.isNotBlank()) Modifier.background(c.gradBtn) else Modifier.background(c.surface3))
                    .clickable(enabled = enabled && value.isNotBlank()) {
                        onSend()
                        keyboardController?.hide()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ArrowUpward,
                    "Envoyer",
                    tint = if (enabled && value.isNotBlank()) c.onGrad else c.text3,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom sheet de confirmation d'une commande
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommandConfirmSheet(
    pending: MainViewModel.PendingCommand,
    onResolve: (Boolean) -> Unit
) {
    val c = Zcolors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    GlassModalBottomSheet(
        onDismissRequest = { onResolve(false) },
        sheetState = sheetState,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .glassSurface(LocalHazeState.current, LocalGlass.current, c.isDark, RoundedCornerShape(28.dp), LocalContext.current)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Default.Terminal, c.cyan, size = 46.dp, iconSize = 22.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "Commande de l'agent",
                        style = TextStyle(fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                        color = c.text2
                    )
                    Text(
                        "Exécuter cette commande ?",
                        style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
                        color = c.text
                    )
                }
            }

            if (pending.command.contains("sudo", ignoreCase = true) || pending.command.contains("rm -rf", ignoreCase = true)) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "COMMANDE SENSIBLE",
                    color = c.red,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(c.red.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.surface2)
                    .border(1.dp, c.border, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    pending.command,
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp),
                    color = c.text,
                    maxLines = 8
                )
            }

            if (pending.note != null) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.amberBg)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        pending.note,
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, lineHeight = 17.sp),
                        color = c.amber
                    )
                }
            }
            pending.changes?.let { ch ->
                Spacer(Modifier.height(10.dp))
                GitDiffView(status = ch.status, diff = ch.diff, error = ch.error, maxHeight = 260.dp)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                pending.footer ?: if (pending.note != null) "Un git push est toujours confirmé, même en mode Auto."
                else "Elle s'exécute dans le dossier du projet, avec le terminal de l'appli. Active le mode Auto dans les réglages pour ne plus avoir à confirmer.",
                style = MaterialTheme.typography.bodySmall,
                color = c.text2
            )

            Spacer(Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(
                    "Refuser",
                    onClick = { onResolve(false) },
                    modifier = Modifier.weight(1f),
                    height = 50.dp
                )
                GradientButton(
                    "Exécuter",
                    onClick = { onResolve(true) },
                    modifier = Modifier.weight(1f),
                    height = 50.dp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom sheet de confirmation de suppression
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteConfirmSheet(
    pending: MainViewModel.PendingDelete,
    onResolve: (Boolean) -> Unit
) {
    val c = Zcolors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    GlassModalBottomSheet(
        onDismissRequest = { onResolve(false) },
        sheetState = sheetState,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .glassSurface(LocalHazeState.current, LocalGlass.current, c.isDark, RoundedCornerShape(28.dp), LocalContext.current)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Default.Shield, c.red, size = 46.dp, iconSize = 22.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "Confirmation de suppression",
                        style = TextStyle(fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                        color = c.text2
                    )
                    Text(
                        "Supprimer cet élément ?",
                        style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
                        color = c.text
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(c.surface2)
                    .border(1.dp, c.border, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    pending.path,
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp),
                    color = c.text,
                    maxLines = 1
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "L'agent demande à supprimer cet élément du projet. Cette action est définitive.",
                style = MaterialTheme.typography.bodySmall,
                color = c.text2
            )

            Spacer(Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(
                    "Refuser",
                    onClick = { onResolve(false) },
                    modifier = Modifier.weight(1f),
                    height = 50.dp
                )
                GradientButton(
                    "Supprimer",
                    onClick = { onResolve(true) },
                    modifier = Modifier.weight(1f),
                    height = 50.dp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom sheet de confirmation d'écriture (avec diff)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WriteConfirmSheet(
    pending: MainViewModel.PendingWrite,
    onResolve: (Boolean, Boolean) -> Unit
) {
    val c = Zcolors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var alwaysAllow by remember(pending.toolCallId) { mutableStateOf(false) }

    GlassModalBottomSheet(
        onDismissRequest = { onResolve(false, false) },
        sheetState = sheetState,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .glassSurface(LocalHazeState.current, LocalGlass.current, c.isDark, RoundedCornerShape(28.dp), LocalContext.current)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // En-tête
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Default.Shield, c.violet, size = 46.dp, iconSize = 22.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "Confirmation d'écriture",
                        style = TextStyle(fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                        color = c.text2
                    )
                    Text(
                        "Autoriser cette écriture ?",
                        style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
                        color = c.text
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Chemin + compteur de lignes
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(c.surface2)
                        .border(1.dp, c.border, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .weight(1f, fill = false)
                ) {
                    Text(
                        pending.path,
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp),
                        color = c.text,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.width(8.dp))
                if (pending.diff.added > 0) StatusBadge("+${pending.diff.added} ligne${if (pending.diff.added > 1) "s" else ""}", c.green)
                if (pending.diff.removed > 0) {
                    Spacer(Modifier.width(6.dp))
                    StatusBadge("-${pending.diff.removed}", c.red)
                }
            }

            Spacer(Modifier.height(12.dp))
            DiffPanel(pending.diff.lines)

            Spacer(Modifier.height(14.dp))

            // Toujours autoriser
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Toujours autoriser ce fichier",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                        color = c.text
                    )
                    Text(
                        "Les prochaines écritures de l'agent sur ce fichier seront automatiques.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.text2
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = alwaysAllow,
                    onCheckedChange = { alwaysAllow = it },
                    colors = androidx.compose.material3.SwitchDefaults.colors(
                        checkedTrackColor = c.green,
                        checkedThumbColor = Color.White,
                        uncheckedTrackColor = c.surface3,
                        uncheckedThumbColor = c.bg2,
                        uncheckedBorderColor = c.border2
                    )
                )
            }

            Spacer(Modifier.height(18.dp))

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(
                    "Refuser",
                    onClick = { onResolve(false, false) },
                    modifier = Modifier.weight(1f),
                    height = 50.dp
                )
                GradientButton(
                    "Autoriser",
                    onClick = { onResolve(true, alwaysAllow) },
                    modifier = Modifier.weight(1f),
                    height = 50.dp,
                    leading = {
                        Icon(Icons.Default.Check, null, tint = c.onGrad, modifier = Modifier.size(17.dp))
                    }
                )
            }
        }
    }
}
