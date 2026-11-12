package com.zagent.app.ui.screens

// corr-010 — Partie « liste du fil » de ChatScreen, extraite sans changer le
// rendu : écran d'accueil (ChatEmptyState), modèle de lignes (ChatRow),
// construction des lignes (buildChatRows) et animation d'entrée (EntreeMessage).
// Visibilité private → internal pour les symboles utilisés par ChatScreen.kt.

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

// Écran d'accueil du chat minimaliste (MA30)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun ChatEmptyState(
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
            title = stringResource(R.string.ui_aucun_projet_choisi),
            body = "Crée ou choisis un projet : ZAgent y lira et écrira les fichiers, et le terminal s'y ouvre au même endroit.",
            primaryText = "Choisir un projet",
            onPrimary = onPickFolder,
            secondaryText = "Ouvrir les réglages",
            onSecondary = onOpenSettings
        )
        noKey -> EmptyState(
            icon = Icons.Default.Key,
            tint = c.violet,
            title = stringResource(R.string.onboarding_cle_titre),
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


// Rendu des messages
// ─────────────────────────────────────────────────────────────────────────────

internal sealed interface ChatRow {
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

internal fun buildChatRows(messages: List<com.zagent.app.ui.viewmodel.ChatMessage>): List<ChatRow> {
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
internal fun EntreeMessage(
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
