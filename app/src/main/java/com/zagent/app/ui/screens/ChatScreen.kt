package com.zagent.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.data.AiRepository
import com.zagent.app.data.DiffUtil
import com.zagent.app.ui.components.CircleIconButton
import com.zagent.app.ui.components.EmptyState
import com.zagent.app.ui.components.GlassCard
import com.zagent.app.ui.components.GradientButton
import com.zagent.app.ui.components.IconBadge
import com.zagent.app.ui.components.MonoChip
import com.zagent.app.ui.components.SecondaryButton
import com.zagent.app.ui.components.StatusBadge
import com.zagent.app.ui.components.TypingPill
import com.zagent.app.ui.components.ZLogo
import com.zagent.app.ui.components.CodeBlock
import com.zagent.app.ui.components.DiffPanel
import com.zagent.app.ui.components.ToolChip
import com.zagent.app.ui.components.ToolState
import com.zagent.app.ui.components.buildInlineMarkdown
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.SpaceGrotesk
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Écran principal : conversation avec l'agent
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit
) {
    val c = Zcolors
    val messages by viewModel.messages.collectAsState()
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) viewModel.setWorkingFolder(uri)
    }

    val chatRows = remember(messages) { buildChatRows(messages) }

    LaunchedEffect(messages.size) {
        if (chatRows.isNotEmpty()) listState.animateScrollToItem(chatRows.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .imePadding()
    ) {
        AmbientTopBar(
            viewModel = viewModel,
            onOpenSettings = onOpenSettings,
            onOpenExplorer = { viewModel.isFileBrowserOpen = true },
            onNewConversation = { viewModel.clearConversation() },
            menuOpen = menuOpen,
            onMenuToggle = { menuOpen = !menuOpen },
            onMenuDismiss = { menuOpen = false }
        )

        // Bannières d'état (erreur réseau, clé invalide, surcharge…)
        StatusBanners(viewModel, onOpenSettings = onOpenSettings)

        // Liste des messages
        Box(Modifier.weight(1f)) {
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center
                ) {
                    ChatEmptyState(viewModel, onOpenSettings, onPickFolder = { folderLauncher.launch(null) })
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(chatRows, key = { it.key }) { row ->
                        when (row) {
                            is ChatRow.Date -> DatePill(row.label)
                            is ChatRow.User -> UserBubble(row.text)
                            is ChatRow.Agent -> AgentCard(row.content, row.tools)
                            is ChatRow.OrphanTool -> OrphanToolLine(row.text)
                        }
                    }
                }
            }
        }

        // Pastille « Réflexion de l'agent » + barre de saisie flottante
        if (viewModel.isLoading) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                TypingPill(viewModel.statusMessage.ifBlank { "Réflexion de l'agent" })
            }
        }
        InputBar(
            value = input,
            onValueChange = { input = it },
            onSend = {
                val t = input
                input = ""
                viewModel.sendMessage(t)
            },
            onOpenExplorer = { viewModel.isFileBrowserOpen = true },
            enabled = !viewModel.isLoading
        )
    }

    // Feuille de confirmation d'écriture
    viewModel.pendingWrite?.let { pw ->
        WriteConfirmSheet(
            pending = pw,
            onResolve = { allowed, always -> viewModel.resolvePendingWrite(allowed, always) }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Barre supérieure
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AmbientTopBar(
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit,
    onOpenExplorer: () -> Unit,
    onNewConversation: () -> Unit,
    menuOpen: Boolean,
    onMenuToggle: () -> Unit,
    onMenuDismiss: () -> Unit
) {
    val c = Zcolors
    Column(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                CircleIconButton(Icons.Default.Menu, "Menu", onClick = onMenuToggle)
                DropdownMenu(expanded = menuOpen, onDismissRequest = onMenuDismiss) {
                    DropdownMenuItem(
                        text = { Text("Nouvelle conversation") },
                        leadingIcon = { Icon(Icons.Default.Refresh, null, tint = c.text2) },
                        onClick = { onMenuDismiss(); onNewConversation() }
                    )
                    DropdownMenuItem(
                        text = { Text("Explorateur de fichiers") },
                        leadingIcon = { Icon(Icons.Default.FolderOpen, null, tint = c.text2) },
                        onClick = { onMenuDismiss(); onOpenExplorer() }
                    )
                    DropdownMenuItem(
                        text = { Text("Réglages") },
                        leadingIcon = { Icon(Icons.Default.Settings, null, tint = c.text2) },
                        onClick = { onMenuDismiss(); onOpenSettings() }
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            ZLogo(modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("ZAgent", style = MaterialTheme.typography.titleLarge, color = c.text)
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
                    MonoChip(viewModel.modelInput)
                }
            }
            Spacer(Modifier.width(10.dp))
            CircleIconButton(Icons.Default.FolderOpen, "Explorateur", onClick = onOpenExplorer)
            Spacer(Modifier.width(10.dp))
            CircleIconButton(Icons.Default.Settings, "Réglages", onClick = onOpenSettings)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bannières d'état
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatusBanners(viewModel: MainViewModel, onOpenSettings: () -> Unit) {
    val c = Zcolors
    val ctx = LocalContext.current

    when {
        viewModel.isKeyInvalid -> StateBanner(
            icon = Icons.Default.Key,
            tint = c.red,
            title = "Clé API invalide",
            body = "La clé configurée a été refusée par l'API Z.ai (erreur 401). Vérifie-la dans les réglages.",
            primary = "Ouvrir les réglages" to { onOpenSettings() },
            secondary = "console.z.ai" to {
                try {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://console.z.ai")))
                } catch (_: Exception) { }
            }
        )
        viewModel.isNetworkError -> OfflineBanner(viewModel)
        viewModel.showFallbackModelButton -> StateBanner(
            icon = Icons.Default.Bolt,
            tint = c.amber,
            title = "Modèle surchargé",
            body = "${viewModel.modelInput} est momentanément saturé. Bascule sur l'autre modèle ou réessaie dans un instant.",
            primary = "Essayer l'autre modèle" to { viewModel.switchToOtherModel() },
            secondary = "Réessayer" to { viewModel.retryLast() }
        )
        !viewModel.isLoading && viewModel.statusMessage.isNotEmpty() -> StateBanner(
            icon = Icons.Default.ErrorOutline,
            tint = c.red,
            title = "Une erreur est survenue",
            body = viewModel.statusMessage,
            primary = "Réessayer" to { viewModel.retryLast() }
        )
    }
}

@Composable
private fun OfflineBanner(viewModel: MainViewModel) {
    val c = Zcolors
    var countdown by remember { mutableStateOf(30) }

    LaunchedEffect(Unit) {
        while (viewModel.autoRetryCount < 3) {
            while (countdown > 0) {
                kotlinx.coroutines.delay(1000)
                countdown--
            }
            viewModel.retryLast(auto = true)
            countdown = 30
        }
    }

    StateBanner(
        icon = Icons.Default.WifiOff,
        tint = c.amber,
        title = "Pas de connexion",
        body = "ZAgent ne parvient pas à joindre l'API Z.ai. Tes fichiers locaux restent accessibles hors ligne.",
        primary = "Réessayer" to {
            countdown = 30
            viewModel.retryLast()
        },
        footer = if (viewModel.autoRetryCount < 3 && countdown > 0) {
            "Nouvelle tentative automatique dans $countdown s"
        } else null
    )
}

@Composable
private fun StateBanner(
    icon: ImageVector,
    tint: Color,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    primary: Pair<String, () -> Unit>? = null,
    secondary: Pair<String, () -> Unit>? = null,
    footer: String? = null
) {
    val c = Zcolors
    val bg = when (tint) {
        c.red -> c.redBg
        c.amber -> c.amberBg
        c.green -> c.greenBg
        c.cyan -> c.cyanBg
        else -> c.violetBg
    }
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        backgroundColor = bg,
        borderColor = tint.copy(alpha = 0.30f),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            IconBadge(icon, tint)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
                    color = c.text
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.text2
                )
                if (primary != null || secondary != null) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (primary != null) {
                            GradientButton(
                                primary.first,
                                onClick = primary.second,
                                modifier = Modifier.weight(1f, fill = false),
                                height = 38.dp
                            )
                        }
                        if (secondary != null) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable(onClick = secondary.second)
                                    .padding(horizontal = 12.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    secondary.first,
                                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                                    color = c.text2
                                )
                            }
                        }
                    }
                }
                if (footer != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        footer,
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp),
                        color = c.text3
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// États vides
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatEmptyState(
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit,
    onPickFolder: () -> Unit
) {
    val c = Zcolors
    val noFolder = viewModel.workingFolderUriString.isNullOrEmpty()
    val noKey = viewModel.apiKeyInput.isBlank()

    when {
        noFolder -> EmptyState(
            icon = Icons.Default.FolderOpen,
            tint = c.amber,
            title = "Aucun dossier choisi",
            body = "Sélectionne le dossier d'un projet : ZAgent y lira et écrira les fichiers, sans jamais toucher au reste du téléphone.",
            primaryText = "Choisir un dossier",
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
        else -> EmptyState(
            icon = Icons.Default.Terminal,
            tint = c.cyan,
            title = "Prêt à coder",
            body = "Décris une tâche : l'agent explore ton dossier, lit les fichiers et te propose les modifications avant d'écrire.",
            secondaryText = "Choisir un autre dossier",
            onSecondary = onPickFolder
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Rendu des messages
// ─────────────────────────────────────────────────────────────────────────────

private sealed interface ChatRow {
    val key: String

    data class Date(val label: String) : ChatRow {
        override val key: String get() = "date"
    }

    data class User(val id: Int, val text: String) : ChatRow {
        override val key: String get() = "u_$id"
    }

    data class Agent(
        val id: Int,
        val content: String?,
        val tools: List<Pair<AiRepository.ToolCallData, String?>>
    ) : ChatRow {
        override val key: String get() = "a_$id"
    }

    data class OrphanTool(val id: Int, val text: String) : ChatRow {
        override val key: String get() = "t_$id"
    }
}

private fun buildChatRows(messages: List<com.zagent.app.ui.viewmodel.ChatMessage>): List<ChatRow> {
    val rows = mutableListOf<ChatRow>()
    var currentTools: MutableList<Pair<AiRepository.ToolCallData, String?>>? = null
    var nextId = 0

    val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

    for (m in messages) {
        when (m.role) {
            "user" -> {
                currentTools = null
                if (rows.none { it is ChatRow.Date }) {
                    rows.add(ChatRow.Date("Aujourd'hui · ${timeFmt.format(Date(m.time))}"))
                }
                rows.add(ChatRow.User(nextId++, m.content ?: ""))
            }
            "assistant" -> {
                currentTools = mutableListOf()
                rows.add(ChatRow.Agent(nextId++, m.content, currentTools))
                m.toolCalls?.forEach { tc -> currentTools.add(tc to null) }
            }
            "tool" -> {
                val last = rows.lastOrNull()
                if (last is ChatRow.Agent && currentTools != null) {
                    val idx = currentTools.indexOfFirst { it.first.id == m.toolCallId }
                    if (idx >= 0) currentTools[idx] = currentTools[idx].copy(second = m.content)
                    else rows.add(ChatRow.OrphanTool(nextId++, "${m.toolLog ?: "outil"} → ${(m.content ?: "").take(120)}"))
                } else {
                    rows.add(ChatRow.OrphanTool(nextId++, "${m.toolLog ?: "outil"} → ${(m.content ?: "").take(120)}"))
                }
            }
        }
    }
    return rows
}

@Composable
private fun DatePill(label: String) {
    val c = Zcolors
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = TextStyle(fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.Medium),
            color = c.text2,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(c.surface2)
                .border(1.dp, c.border, RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun UserBubble(text: String) {
    val c = Zcolors
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 56.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp))
                .background(c.gradBtn)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                buildInlineMarkdown(
                    text,
                    baseColor = c.onGrad,
                    codeColor = c.onGrad,
                    codeBg = Color(0x30FFFFFF)
                ),
                style = TextStyle(fontFamily = Inter, fontSize = 15.sp, lineHeight = 22.sp),
                color = c.onGrad
            )
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

@Composable
private fun AgentCard(
    content: String?,
    tools: List<Pair<AiRepository.ToolCallData, String?>>
) {
    val c = Zcolors
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 40.dp),
        shape = RoundedCornerShape(22.dp, 22.dp, 22.dp, 6.dp),
        contentPadding = PaddingValues(14.dp)
    ) {
        if (!content.isNullOrBlank()) {
            splitContent(content).forEachIndexed { i, seg ->
                if (seg.isCode) {
                    CodeBlockFor(seg)
                } else if (seg.text.isNotBlank()) {
                    Text(
                        buildInlineMarkdown(
                            seg.text.trim(),
                            baseColor = c.text,
                            codeColor = c.cyan,
                            codeBg = c.cyanBg
                        ),
                        style = TextStyle(fontFamily = Inter, fontSize = 15.sp, lineHeight = 23.sp),
                        color = c.text
                    )
                }
                if (i < (tools.size + 1)) Spacer(Modifier.height(8.dp))
            }
        }
        tools.forEach { (tc, result) ->
            ToolChip(
                name = tc.name,
                arg = toolArgShort(tc.arguments),
                state = toolStateOf(result),
                resultText = result,
                expandKey = tc.id
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CodeBlockFor(seg: ContentSegment) {
    CodeBlock(
        path = null,
        language = seg.lang,
        code = seg.text.trimEnd()
    )
}

private fun toolArgShort(argumentsJson: String): String? = try {
    val obj = kotlinx.serialization.json.Json.parseToJsonElement(argumentsJson)
        .let { it as? kotlinx.serialization.json.JsonObject }
    obj?.get("path")?.let { p ->
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
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(c.surface2)
                .border(1.dp, c.border2, RoundedCornerShape(50))
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
                maxLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = c.cyan
                ),
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .then(if (enabled && value.isNotBlank()) Modifier.background(c.gradBtn) else Modifier.background(c.surface3))
                    .clickable(enabled = enabled && value.isNotBlank(), onClick = onSend),
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

    ModalBottomSheet(
        onDismissRequest = { onResolve(false, false) },
        sheetState = sheetState,
        containerColor = c.bg2,
        shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
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
