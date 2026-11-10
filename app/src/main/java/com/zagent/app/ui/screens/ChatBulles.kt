package com.zagent.app.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.data.AiRepository
import com.zagent.app.ui.components.common.AnsiText
import com.zagent.app.ui.components.chat.CodeBlock
import com.zagent.app.ui.components.chat.MarkdownText
import com.zagent.app.ui.components.chat.ThinkingBlock
import com.zagent.app.ui.components.chat.ToolChip
import com.zagent.app.ui.components.chat.ToolGroupSummary
import com.zagent.app.ui.components.chat.ToolState
import com.zagent.app.ui.components.common.ZLogo
import com.zagent.app.ui.components.chat.buildInlineMarkdown
import com.zagent.app.ui.components.common.rememberZHaptics
import com.zagent.app.ui.components.common.tailForDisplay
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.LocalGlass
import com.zagent.app.ui.theme.LocalHazeState
import com.zagent.app.ui.theme.LocalReduceMotion
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.theme.glassSurface
import com.zagent.app.ui.viewmodel.MainViewModel

// ─────────────────────────────────────────────────────────────────────────────
// LOT 9 · MàJ 99 — découpe de ChatScreen : rendu des messages (bulles, carte
// agent, blocs de commande). Anciennement fonctions privées de ChatScreen.kt,
// mêmes signatures (visibilité internal, même paquet).
// MàJ 96 — zones tactiles ≥ 48 dp et rôle Button sur les icônes d'action.
// ─────────────────────────────────────────────────────────────────────────────

/** En-tête de date collant, petite pastille de verre (MA39). */
@Composable
internal fun DatePill(label: String, modifier: Modifier = Modifier) {
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
internal fun UserBubble(
    text: String,
    messageIndex: Int,
    onEdit: (Int, String) -> Unit,
    onQuote: (String) -> Unit,
    onDelete: (Int) -> Unit,
    /** feat-001 — requête de recherche à surligner ("" = aucun). */
    highlight: String = "",
    /** feat-002 — épingle le message dans le panneau des épinglés (null = masqué). */
    onPin: (() -> Unit)? = null
) {
    val c = Zcolors
    val clipboard = LocalClipboardManager.current
    val ctx = LocalContext.current
    val haptique = rememberZHaptics() // MàJ 91
    var menuOpen by remember { mutableStateOf(false) }
    // MA49/MA53 : le message stocké peut contenir un bloc de contexte joint (pièces
    // jointes et mentions @fichier) ; on n'affiche que le texte visible + un résumé.
    val visibleText = remember(text) { text.substringBefore(MainViewModel.CTX_MARKER) }
    val attachedCount = remember(text) {
        if (!text.contains(MainViewModel.CTX_MARKER)) 0
        else text.substringAfter(MainViewModel.CTX_MARKER).lines().count { it.trimStart().startsWith("[") }
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Box(
            Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(20.dp))
                .background(c.surface2)
                .combinedClickable(onClick = {}, onLongClick = { haptique.menu(); menuOpen = true })
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    buildInlineMarkdown(
                        visibleText,
                        baseColor = c.text,
                        codeColor = c.cyan,
                        codeBg = c.cyanBg,
                        highlight = highlight,
                        highlightBg = if (highlight.isBlank()) null else c.amber.copy(alpha = 0.4f)
                    ),
                    style = TextStyle(fontFamily = Inter, fontSize = 15.sp, lineHeight = 22.sp),
                    color = c.text
                )
                if (attachedCount > 0) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AttachFile,
                            null,
                            tint = c.cyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            if (attachedCount == 1) "1 élément joint au contexte" else "" + attachedCount + " éléments joints au contexte",
                            style = TextStyle(fontFamily = Inter, fontSize = 11.sp),
                            color = c.cyan
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    // MàJ 96 — zone tactile 48 dp autour de l'icône discrète.
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(onClick = { onEdit(messageIndex, visibleText) }, role = Role.Button),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Modifier le message",
                            tint = c.text3,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Copier le texte", color = c.text) },
                    onClick = { menuOpen = false; clipboard.setText(AnnotatedString(visibleText)) }
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
                    onClick = { menuOpen = false; onQuote(visibleText) }
                )
                if (onPin != null) {
                    DropdownMenuItem(
                        text = { Text("Épingler", color = c.text) },
                        onClick = { menuOpen = false; onPin() } // feat-002
                    )
                }
                DropdownMenuItem(
                    text = { Text("Supprimer", color = c.red) },
                    onClick = { menuOpen = false; onDelete(messageIndex) }
                )
            }
        }
    }
}

@Composable
internal fun OrphanToolLine(text: String) {
    val c = Zcolors
    Text(
        text,
        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, lineHeight = 15.sp),
        color = c.text3,
        modifier = Modifier.padding(start = 8.dp)
    )
}

/** Découpe le contenu en segments texte / blocs de code. */
internal data class ContentSegment(val isCode: Boolean, val lang: String?, val text: String)

internal fun splitContent(content: String): List<ContentSegment> {
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
internal fun StreamingCursor() {
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
internal fun AgentCard(
    content: String?,
    tools: List<Pair<AiRepository.ToolCallData, String?>>,
    modifier: Modifier = Modifier,
    showLogo: Boolean = false,
    isStreaming: Boolean = false,
    reasoning: String? = null,
    thinkingSeconds: Int = 0,
    isLast: Boolean = false,
    messageIndex: Int = -1,
    /** MàJ 104 — nom du modèle émetteur (mode Duo) ; null = modèle principal. */
    modeleLabel: String? = null,
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
    onStop: () -> Unit = {},
    /** feat-001 — requête de recherche à surligner ("" = aucun). */
    highlight: String = "",
    /** feat-004 — modèles alternatifs pour la régénération. */
    modelesRegeneration: List<String> = emptyList(),
    /** feat-004 — régénère après bascule sur le modèle donné. */
    onRegenerateWith: (String) -> Unit = {},
    /** feat-002 — épingle le message dans le panneau des épinglés. */
    onPin: () -> Unit = {}
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

        // MàJ 104 — pastille du modèle émetteur (mode Duo)
        if (!modeleLabel.isNullOrBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(c.violetBg)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        modeleLabel,
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                        color = c.violet
                    )
                }
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
                        onFileTap = onFileTap,
                        highlight = highlight
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
                onDelete = onDeleteMessage,
                onRegenerateWith = onRegenerateWith, // feat-004
                modelesRegeneration = modelesRegeneration, // feat-004
                onPin = onPin // feat-002
            )
        }
    }
}

/** Icône d'action discrète (MA36) — zone tactile 48 dp (MàJ 96). */
@Composable
internal fun ActionIcon(icon: ImageVector, label: String, tint: Color = Zcolors.text3, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(15.dp))
    }
}

/** Copier · Régénérer · 👍 · 👎 · Partager + menu contextuel (MA36). */
@Composable
internal fun MessageActionsBar(
    messageKey: String,
    messageText: String,
    full: Boolean,
    canRegenerate: Boolean,
    onToggle: () -> Unit,
    onRegenerate: () -> Unit,
    onQuote: (String) -> Unit,
    onDelete: () -> Unit,
    /** feat-004 — régénère la réponse en basculant d'abord sur le modèle donné. */
    onRegenerateWith: (String) -> Unit = {},
    /** feat-004 — modèles alternatifs proposés dans le menu de régénération. */
    modelesRegeneration: List<String> = emptyList(),
    /** feat-002 — épingle le message dans le panneau des épinglés. */
    onPin: () -> Unit = {}
) {
    val c = Zcolors
    val clipboard = LocalClipboardManager.current
    val ctx = LocalContext.current
    val haptique = rememberZHaptics() // MàJ 91
    var feedback by rememberSaveable(messageKey) { mutableStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    // feat-004 — menu de régénération avec choix d'un autre modèle
    var regenMenuOpen by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (!full) {
            ActionIcon(Icons.Default.MoreHoriz, "Actions") { onToggle() }
        } else {
            ActionIcon(Icons.Default.ContentCopy, "Copier") { clipboard.setText(AnnotatedString(messageText)) }
            if (canRegenerate) {
                // feat-004 — « Régénérer la réponse » avec choix d'un autre modèle :
                // s'il existe des modèles alternatifs, le bouton ouvre un menu
                // (modèle actuel + alternatives) ; sinon régénération directe.
                Box {
                    ActionIcon(
                        Icons.Default.Refresh,
                        if (modelesRegeneration.isEmpty()) "Régénérer" else "Régénérer (choix du modèle)"
                    ) {
                        if (modelesRegeneration.isEmpty()) onRegenerate()
                        else { haptique.menu(); regenMenuOpen = !regenMenuOpen }
                    }
                    DropdownMenu(expanded = regenMenuOpen, onDismissRequest = { regenMenuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Régénérer avec le modèle actuel", color = c.text) },
                            onClick = { regenMenuOpen = false; onRegenerate() }
                        )
                        modelesRegeneration.forEach { m ->
                            DropdownMenuItem(
                                text = { Text("Régénérer avec " + m, color = c.text) },
                                onClick = { regenMenuOpen = false; onRegenerateWith(m) }
                            )
                        }
                    }
                }
            }
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
            ActionIcon(Icons.Default.MoreHoriz, "Plus") { haptique.menu(); menuOpen = !menuOpen }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Citer", color = c.text) }, onClick = { menuOpen = false; onQuote(messageText) })
                DropdownMenuItem(
                    text = { Text("Sélectionner", color = c.text) },
                    onClick = {
                        menuOpen = false
                        Toast.makeText(ctx, "Maintiens ton doigt sur le texte pour le sélectionner", Toast.LENGTH_SHORT).show()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Épingler", color = c.text) },
                    onClick = { menuOpen = false; onPin() } // feat-002
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
internal fun CommandBlock(
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
                        .clickable(onClick = onStop, role = Role.Button)
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
                    .clickable(role = Role.Button) { onFix(command, result) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
internal fun CodeBlockFor(seg: ContentSegment, onApply: ((String) -> Unit)? = null) {
    CodeBlock(
        path = null,
        language = seg.lang,
        code = seg.text.trimEnd(),
        onApply = onApply
    )
}

internal fun toolArgShort(argumentsJson: String): String? = try {
    val obj = kotlinx.serialization.json.Json.parseToJsonElement(argumentsJson)
        .let { it as? kotlinx.serialization.json.JsonObject }
    (obj?.get("path") ?: obj?.get("command"))?.let { p ->
        (p as? kotlinx.serialization.json.JsonPrimitive)?.content
    }
} catch (_: Exception) {
    null
}

internal fun toolStateOf(result: String?): ToolState = when {
    result == null -> ToolState.RUNNING
    result.startsWith("Refusé") -> ToolState.REFUSED
    result.startsWith("Erreur") || result.startsWith("Erreur d'écriture") -> ToolState.ERROR
    else -> ToolState.OK
}
