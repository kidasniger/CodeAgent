package com.zagent.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.Key
// err-001 : nativeKeyCode est l'extension sur android.view.KeyEvent (Key.NumpadEnter
// n'existe pas dans ce BOM Compose) — Ctrl+Entrée accepte aussi le Entrée du pavé numérique.
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.onPreviewKeyEvent
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
import androidx.compose.material.icons.filled.PushPin
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.DisposableEffect
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
import com.zagent.app.ui.components.common.AnsiText
import com.zagent.app.ui.components.common.tailForDisplay
import com.zagent.app.ui.components.chat.AskCard
import com.zagent.app.data.DiffUtil
import com.zagent.app.ui.components.common.GlassModalBottomSheet
import com.zagent.app.ui.components.common.EmptyState
import com.zagent.app.ui.components.common.GlassAlertDialog
import com.zagent.app.ui.components.common.GlassCard
import com.zagent.app.ui.components.common.GradientButton
import com.zagent.app.ui.components.common.IconBadge
import com.zagent.app.ui.components.chat.MarkdownText
import com.zagent.app.ui.components.common.MonoChip
import com.zagent.app.ui.components.common.SecondaryButton
import com.zagent.app.ui.components.common.StatusBadge
import com.zagent.app.ui.components.chat.ThinkingBlock
import com.zagent.app.ui.components.chat.ToolGroupSummary
import com.zagent.app.ui.components.common.TypingPill
import com.zagent.app.ui.components.chat.UsageLine
import com.zagent.app.ui.components.common.ZLogo
import com.zagent.app.ui.components.common.rememberZHaptics
import com.zagent.app.ui.components.chat.DevServerBar
import com.zagent.app.ui.components.chat.GitChangesSheet
import com.zagent.app.ui.components.chat.GitDiffView
import com.zagent.app.ui.components.chat.ModelPickerDialog
import com.zagent.app.ui.components.chat.ChatInputBar
import com.zagent.app.ui.components.chat.CodeBlock
import com.zagent.app.ui.components.chat.DiffPanel
import com.zagent.app.ui.components.chat.ToolChip
import com.zagent.app.ui.components.chat.ToolState
import com.zagent.app.ui.components.common.ZMenu
import com.zagent.app.ui.components.common.ZMenuItem
import com.zagent.app.ui.components.common.ZMenuSeparator
import com.zagent.app.ui.components.chat.buildInlineMarkdown
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
    // feat-023 — moteur de synthèse vocale pour « Lire à voix haute ».
    val ctxTts = LocalContext.current
    val tts = remember {
        android.speech.tts.TextToSpeech(ctxTts) { }
    }
    var ttsActif by rememberSaveable { mutableStateOf(false) }
    DisposableEffect(Unit) {
        onDispose { runCatching { tts.stop(); tts.shutdown() } }
    }
    remember(tts) {
        tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(id: String?) { ttsActif = true }
            override fun onDone(id: String?) { ttsActif = false }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) { ttsActif = false }
        })
        0
    }
    fun lireAVoixHaute(texte: String) {
        if (ttsActif) {
            runCatching { tts.stop() }
            ttsActif = false
        } else {
            runCatching {
                tts.language = java.util.Locale.FRENCH
                tts.setSpeechRate(1.05f)
                tts.speak(texte.take(6000), android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "zagent-tts")
            }
        }
    }
    var applyCode by remember { mutableStateOf<String?>(null) }

    // feat-001 — recherche dans la conversation courante : barre, surlignage des
    // occurrences (bulles et rendu markdown) et navigation précédent/suivant qui
    // fait défiler jusqu'à la ligne du fil contenant l'occurrence courante.
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchIdx by rememberSaveable { mutableStateOf(0) }
    val searchMatches = remember(searchQuery, chatRows) {
        if (searchQuery.isBlank()) emptyList()
        else chatRows.mapIndexed { i, row ->
            val texte = when (row) {
                is ChatRow.User -> row.text
                is ChatRow.Agent -> row.content
                is ChatRow.OrphanTool -> row.text
                else -> ""
            }
            if (texte?.contains(searchQuery, ignoreCase = true) == true) i else -1
        }.filter { it >= 0 }
    }
    LaunchedEffect(searchQuery) { searchIdx = 0 }
    LaunchedEffect(searchIdx, searchMatches.size, searchOpen) {
        if (searchOpen && searchMatches.isNotEmpty()) {
            listState.animateScrollToItem(searchMatches[searchIdx.coerceIn(0, searchMatches.size - 1)])
        }
    }
    val naviguerRecherche: (Int) -> Unit = { delta ->
        val n = searchMatches.size
        if (n > 0) searchIdx = (((searchIdx + delta) % n) + n) % n
    }

    // feat-002 — messages épinglés : extraits de la conversation courante (en
    // mémoire), retrouvables dans un panneau qui défile jusqu'au message épinglé.
    val epingles = remember { mutableStateListOf<Pair<String, String>>() }
    var panneauEpingles by remember { mutableStateOf(false) }
    LaunchedEffect(viewModel.conversationId) { epingles.clear() }
    val epinglerRow: (String, String) -> Unit = { cle, texte ->
        if (epingles.none { it.first == cle }) {
            val apercu = texte.substringBefore(MainViewModel.CTX_MARKER).trim()
            epingles.add(cle to apercu.lineSequence().firstOrNull().orEmpty().take(160))
        }
    }

    // MA55 : restaure le brouillon enregistré pour cette conversation, et synchronise
    // chaque frappe vers le brouillon (sauvegarde différée côté MainViewModel).
    val setInput: (String) -> Unit = { t ->
        input = t
        viewModel.onInputChanged(t)
    }
    LaunchedEffect(viewModel.conversationId) {
        setInput(viewModel.currentDraft())
    }

    // feat-094 — texte reçu via « Partager vers ZAgent » : chargé une seule fois.
    if (viewModel.textePartage.isNotBlank()) {
        val partage = viewModel.consommerTextePartage()
        if (partage.isNotBlank() && input.isBlank()) setInput(partage)
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
        // feat-001 : pendant une recherche, l'auto-scroll ne vole le focus à personne.
        if (userAtBottom && chatRows.isNotEmpty() && !(searchOpen && searchMatches.isNotEmpty())) {
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
    // feat-004 — modèles alternatifs proposés dans le menu « Régénérer »
    val modelesRegeneration = remember(viewModel.effectiveModel) {
        viewModel.modelesConversationnels(null).map { it.id }
            .filter { it != viewModel.effectiveModel }
            .take(4)
    }
    val thinkingSeconds = ((nowTick - viewModel.thinkingStartedAt) / 1000).toInt().coerceAtLeast(0)

    // ux-094 — clavier physique : Ctrl+Entrée envoie le message courant.
    val envoyerAuClavier: () -> Unit = {
        val t = input
        if (t.isNotBlank()) {
            haptique.envoi()
            viewModel.onInputSent()
            input = ""
            if (editingIndex >= 0) {
                val idx = editingIndex
                editingIndex = -1
                viewModel.editAndResend(idx, t)
            } else {
                viewModel.sendMessage(t)
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .onPreviewKeyEvent { ev ->
                if (ev.type == KeyEventType.KeyDown && ev.isCtrlPressed &&
                    (ev.key == Key.Enter || ev.nativeKeyCode == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER)
                ) {
                    envoyerAuClavier()
                    true
                } else false
            }
    ) {
        AmbientTopBar(
            viewModel = viewModel,
            scrolled = barreDefilee,
            onNewConversation = { viewModel.clearConversation() },
            onOpenSettings = onOpenSettings,
            onSearchInConversation = { searchOpen = true }, // feat-001
            onOpenPinned = { panneauEpingles = true }, // feat-002
            nbEpingles = epingles.size // feat-002
        )

        // MA43 : une seule notification discrète à la fois, en pilule de verre
        GlassToastPill(viewModel = viewModel, onOpenSettings = onOpenSettings)
        if (viewModel.isNetworkError) AutoRetryEffect(viewModel)

        // Liste de tâches de l'agent (MA40)
        TodoPanel(viewModel.todos, viewModel.isLoading)

        // feat-001 — barre de recherche dans la conversation courante
        AnimatedVisibility(visible = searchOpen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Rechercher dans la conversation",
                            style = TextStyle(fontFamily = Inter, fontSize = 13.sp),
                            color = c.text3
                        )
                    },
                    singleLine = true,
                    shape = CircleShape,
                    textStyle = TextStyle(fontFamily = Inter, fontSize = 14.sp, color = c.text),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (searchMatches.isEmpty()) "0/0"
                    else "" + (searchIdx + 1) + "/" + searchMatches.size,
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp),
                    color = c.text3,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { naviguerRecherche(-1) },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.ExpandLess, "Occurrence précédente", tint = c.text2) }
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { naviguerRecherche(1) },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.ExpandMore, "Occurrence suivante", tint = c.text2) }
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable {
                            searchOpen = false
                            searchQuery = ""
                        },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Close, "Fermer la recherche", tint = c.text2) }
            }
        }

        // feat-002 — panneau des messages épinglés
        if (panneauEpingles) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            GlassModalBottomSheet(
                onDismissRequest = { panneauEpingles = false },
                sheetState = sheetState
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp)
                ) {
                    Text(
                        "Messages épinglés",
                        style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
                        color = c.text
                    )
                    Spacer(Modifier.height(10.dp))
                    if (epingles.isEmpty()) {
                        Text(
                            "Aucun message épinglé. Ouvre le menu d'un message puis « Épingler ».",
                            style = TextStyle(fontFamily = Inter, fontSize = 12.5.sp),
                            color = c.text2
                        )
                    }
                    epingles.toList().forEach { (cle, extrait) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(c.surface2)
                                .clickable {
                                    panneauEpingles = false
                                    val idx = chatRows.indexOfFirst { it.key == cle }
                                    if (idx >= 0) scope.launch { listState.animateScrollToItem(idx) }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PushPin, null, tint = c.cyan, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                extrait,
                                style = TextStyle(fontFamily = Inter, fontSize = 12.5.sp, lineHeight = 17.sp),
                                color = c.text,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

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
                                                onDelete = { viewModel.deleteFromMessage(it) },
                                                highlight = searchQuery, // feat-001
                                                onPin = { epinglerRow(row.key, row.text) } // feat-002
                                            )
                                        }
                                        // feat-003 — sélecteur de branche : basculer entre
                                        // la suite courante et les suites alternatives conservées.
                                        if (viewModel.hasBranchAt(row.messageIndex)) {
                                            val bInfo = viewModel.branchInfoAt(row.messageIndex)
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                Text(
                                                    "Branche " + (bInfo?.first ?: 1) + "/" + (bInfo?.second ?: 1) +
                                                        " — appuyer pour basculer",
                                                    style = TextStyle(fontFamily = Inter, fontSize = 11.sp),
                                                    color = c.cyan,
                                                    modifier = Modifier
                                                        .clip(MaterialTheme.shapes.extraSmall)
                                                        .clickable { viewModel.switchBranch(row.messageIndex) }
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
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
                                                onRegenerateWith = { m -> viewModel.regenerateLastWith(m) }, // feat-004
                                                modelesRegeneration = modelesRegeneration, // feat-004
                                                onQuote = { t -> setInput("> " + t.replace("\n", "\n> ") + "\n\n") },
                                                onDeleteMessage = { viewModel.deleteFromMessage(row.messageIndex) },
                                                fixId = if (viewModel.isLoading) null else lastFailedCmdId,
                                                onFix = { cmd, res -> viewModel.fixCommandWithAi(cmd, res) },
                                                liveId = viewModel.liveCommandId,
                                                liveOutput = { viewModel.liveOutput },
                                                liveStartedAt = viewModel.liveStartedAt,
                                                onStop = { viewModel.stopLiveCommand() },
                                                highlight = searchQuery, // feat-001
                                                onPin = { epinglerRow(row.key, row.content.orEmpty()) }, // feat-002
                                                eval = viewModel.evaluationDe(row.messageIndex), // feat-017
                                                onEval = { viewModel.evaluer(row.messageIndex, it) }, // feat-017
                                                onSpeak = { lireAVoixHaute(row.content.orEmpty()) } // feat-023
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

                // feat-096 — bandeau hors ligne (file d'attente active)
                if (viewModel.isOffline) {
                    Row(
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(c.amberBg)
                            .border(1.dp, c.amber, RoundedCornerShape(50))
                            .clickable { viewModel.reessayerHorsLigne() }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Hors ligne — le chat attend le réseau (tap pour réessayer). Éditeur et terminal restent utilisables.",
                            style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp),
                            color = c.amber,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }

                // feat-098 — bandeau budget dépassé
                if (viewModel.budgetDepasse && !viewModel.isOffline) {
                    Row(
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(c.violetBg)
                            .border(1.dp, c.violet, RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Budget du jour dépassé — alerte (modifiable dans Réglages).",
                            style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp),
                            color = c.violet
                        )
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
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    // ux-093 — l'état dynamique de l'agent (progression, erreur)
                    // est annoncé automatiquement aux lecteurs d'écran.
                    .semantics { liveRegion = LiveRegionMode.Polite },
                horizontalArrangement = Arrangement.Center
            ) {
                TypingPill(
                    viewModel.statusMessage.ifBlank { stringResource(R.string.typing_default) },
                    // ux-015 — tant que l'agent n'a rien produit, « Stop » annule
                    // aussi le message tout juste envoyé (il est retiré du fil).
                    onStop = {
                        if (viewModel.envoiAnnulable) viewModel.annulerDernierEnvoi()
                        else viewModel.stopAgent()
                    }
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
        // feat-016 — file de messages en attente d'envoi (l'agent travaille).
        if (viewModel.messageQueue.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "File : " + viewModel.messageQueue.size + " message(s) en attente — envoi à la fin du tour",
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
                        .clickable { viewModel.annulerFile() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        // feat-013 — rappel discret que le mode Plan est actif.
        if (viewModel.planMode) {
            Text(
                "Mode Plan : l'agent propose un plan et attend ta validation avant d'écrire (/plan pour désactiver)",
                style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp),
                color = c.cyan,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
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
        ChatInputBar(
            value = input,
            onValueChange = setInput,
            onSend = envoyerAuClavier,
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
            onResolve = { allowed -> viewModel.resolvePendingCommand(allowed) },
            onAllowAllTurn = { viewModel.autoriserToutCeTour() } // ux-019
        )
    }

    // Feuille de confirmation de suppression
    viewModel.pendingDelete?.let { pd ->
        DeleteConfirmSheet(
            pending = pd,
            onResolve = { allowed -> viewModel.resolvePendingDelete(allowed) }
        )
    }

    // Changements Git (ouvert par l'utilisateur) — corr-008 : état regroupé
    if (viewModel.gitUi.sheetOpen) {
        val g = viewModel.gitUi.changes
        GitChangesSheet(
            status = g?.status.orEmpty(),
            diff = g?.diff.orEmpty(),
            error = g?.error,
            loading = viewModel.gitUi.loading,
            onRefresh = { viewModel.refreshGitChanges() },
            onClose = { viewModel.gitUi = viewModel.gitUi.copy(sheetOpen = false) },
            onOpenFull = {
                viewModel.gitUi = viewModel.gitUi.copy(sheetOpen = false)
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


// ─────────────────────────────────────────────────────────────────────────────

