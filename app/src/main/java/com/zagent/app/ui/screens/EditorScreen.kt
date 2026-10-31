package com.zagent.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Terminal
import com.zagent.app.ui.editor.ConsolePanel
import com.zagent.app.ui.editor.PreviewPane
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.ui.components.CircleIconButton
import com.zagent.app.ui.editor.EditorTab
import com.zagent.app.ui.editor.Kind
import com.zagent.app.ui.editor.Lang
import com.zagent.app.ui.editor.SyntaxHighlighter
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Éditeur de code : onglets, coloration, annuler/refaire, recherche/remplacement
// ─────────────────────────────────────────────────────────────────────────────

private val EditorBg = Color(0xFF0F1526)
private val EditorText = Color(0xFFC9D4E8)
private val GutterText = Color(0xFF55627F)
private val MatchBg = Color(0x55FBBF24)
private val CurrentMatchBg = Color(0xAAFBBF24)

private fun styleFor(kind: Kind): SpanStyle = when (kind) {
    Kind.COMMENT -> SpanStyle(color = Color(0xFF6B7A99), fontStyle = FontStyle.Italic)
    Kind.STRING -> SpanStyle(color = Color(0xFF9ECE6A))
    Kind.NUMBER -> SpanStyle(color = Color(0xFFFF9E64))
    Kind.KEYWORD -> SpanStyle(color = Color(0xFFBB9AF7))
    Kind.TYPE -> SpanStyle(color = Color(0xFFE0AF68))
    Kind.FUNCTION -> SpanStyle(color = Color(0xFF7AA2F7))
    Kind.TAG -> SpanStyle(color = Color(0xFFF7768E))
    Kind.ATTR -> SpanStyle(color = Color(0xFF73DACA))
    Kind.PROP -> SpanStyle(color = Color(0xFF7DCFFF))
    Kind.LITERAL -> SpanStyle(color = Color(0xFFFF9E64))
    Kind.HEADING -> SpanStyle(color = Color(0xFF7AA2F7), fontWeight = FontWeight.SemiBold)
    Kind.EMPH -> SpanStyle(color = Color(0xFFE0AF68), fontWeight = FontWeight.SemiBold)
}

private class CodeTransformation(
    private val lang: Lang,
    private val matches: List<IntRange>,
    private val current: Int
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val s = text.text
        val b = AnnotatedString.Builder(s)
        for (sp in SyntaxHighlighter.spans(s, lang)) {
            if (sp.start >= 0 && sp.end <= s.length && sp.start < sp.end) {
                b.addStyle(styleFor(sp.kind), sp.start, sp.end)
            }
        }
        matches.forEachIndexed { i, r ->
            if (r.first >= 0 && r.last + 1 <= s.length) {
                b.addStyle(
                    SpanStyle(background = if (i == current) CurrentMatchBg else MatchBg),
                    r.first,
                    r.last + 1
                )
            }
        }
        return TransformedText(b.toAnnotatedString(), OffsetMapping.Identity)
    }
}

private fun findAll(text: String, query: String, caseSensitive: Boolean): List<IntRange> {
    if (query.isEmpty()) return emptyList()
    val out = ArrayList<IntRange>()
    var i = text.indexOf(query, 0, ignoreCase = !caseSensitive)
    while (i >= 0 && out.size < 5000) {
        out.add(i until i + query.length)
        i = text.indexOf(query, i + query.length, ignoreCase = !caseSensitive)
    }
    return out
}

@Composable
fun EditorScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val c = Zcolors
    val editor = viewModel.editor
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    BackHandler(onBack = onBack)

    val tab = editor.active

    var showSearch by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var replacement by remember { mutableStateOf("") }
    var caseSensitive by remember { mutableStateOf(false) }
    var currentIdx by remember { mutableIntStateOf(0) }
    var confirmClose by remember { mutableStateOf<Int?>(null) }
    var showPreview by remember { mutableStateOf(false) }
    var previewFull by remember { mutableStateOf(false) }
    var previewPath by remember { mutableStateOf<String?>(null) }
    var reloadTick by remember { mutableIntStateOf(0) }
    val console = viewModel.console
    val previewReq = viewModel.previewRequest
    LaunchedEffect(previewReq) {
        if (previewReq != null) {
            previewPath = previewReq
            showPreview = true
            viewModel.previewRequest = null
        }
    }
    var showConsole by remember { mutableStateOf(true) }

    // Plein écran : on masque aussi la barre d'état et de navigation (glisser depuis le bord pour les revoir)
    val fs = showPreview && previewFull
    val hostView = LocalView.current
    DisposableEffect(fs) {
        var ctx = hostView.context
        while (ctx is android.content.ContextWrapper && ctx !is android.app.Activity) ctx = ctx.baseContext
        val window = (ctx as? android.app.Activity)?.window
        val ctrl = window?.let { WindowCompat.getInsetsController(it, hostView) }
        if (fs) {
            ctrl?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            ctrl?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            ctrl?.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { ctrl?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    BackHandler(enabled = showPreview) {
        if (previewFull) previewFull = false else showPreview = false
    }
    LaunchedEffect(tab?.path) {
        if (showPreview && tab != null && tab.lang == Lang.HTML) previewPath = tab.path
    }

    val scrollStates = remember { HashMap<String, ScrollState>() }

    LaunchedEffect(editor.info) {
        if (editor.info != null) {
            delay(2000)
            editor.info = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .then(
                if (fs) Modifier.displayCutoutPadding()
                else Modifier.statusBarsPadding().navigationBarsPadding()
            )
            .imePadding()
    ) {
        // ── Barre supérieure ────────────────────────────────────────────────
        if (!fs) Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Retour", onClick = onBack, size = 38.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    tab?.name ?: "Éditeur",
                    style = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = c.text,
                    maxLines = 1
                )
                if (tab != null) {
                    Text(
                        tab.path,
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.5.sp),
                        color = c.text3,
                        maxLines = 1
                    )
                }
            }
            if (editor.loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = c.cyan)
                Spacer(Modifier.width(8.dp))
            }
            CircleIconButton(
                Icons.AutoMirrored.Filled.Undo, "Annuler",
                onClick = { tab?.undo() }, size = 36.dp,
                iconTint = if ((tab?.undoCount ?: 0) > 0) c.text else c.text3
            )
            Spacer(Modifier.width(6.dp))
            CircleIconButton(
                Icons.AutoMirrored.Filled.Redo, "Refaire",
                onClick = { tab?.redo() }, size = 36.dp,
                iconTint = if ((tab?.redoCount ?: 0) > 0) c.text else c.text3
            )
            Spacer(Modifier.width(6.dp))
            CircleIconButton(
                Icons.Default.Search, "Rechercher",
                onClick = { showSearch = !showSearch }, size = 36.dp,
                iconTint = if (showSearch) c.cyan else c.text
            )
            Spacer(Modifier.width(6.dp))
            CircleIconButton(
                Icons.Default.PlayArrow, "Aperçu",
                onClick = {
                    if (showPreview) {
                        showPreview = false
                        previewFull = false
                    } else {
                        if (tab != null && tab.lang == Lang.HTML) previewPath = tab.path
                        if (previewPath == null) editor.info = "Ouvre un fichier .html pour l'aperçu"
                        else showPreview = true
                    }
                },
                size = 36.dp,
                iconTint = if (showPreview) c.cyan else c.text
            )
            Spacer(Modifier.width(6.dp))
            CircleIconButton(
                Icons.Default.Save, "Enregistrer",
                onClick = { tab?.let { editor.save(it) } }, size = 36.dp,
                iconTint = if (tab?.dirty == true) c.green else c.text3
            )
        }

        // ── Onglets ─────────────────────────────────────────────────────────
        if (!fs && editor.tabs.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                editor.tabs.forEachIndexed { index, t ->
                    val selected = index == editor.activeIndex
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                            .background(if (selected) EditorBg else c.surface2)
                            .clickable { editor.select(index) }
                            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            t.name,
                            style = TextStyle(
                                fontFamily = JetBrainsMono,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (selected) EditorText else c.text2,
                            maxLines = 1
                        )
                        if (t.dirty) {
                            Spacer(Modifier.width(6.dp))
                            Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(c.amber))
                        }
                        Box(
                            Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    if (t.dirty) confirmClose = index else editor.close(index)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, "Fermer l'onglet", tint = c.text3, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // ── Recherche / remplacement ────────────────────────────────────────
        val text = tab?.value?.text ?: ""
        val matches = remember(text, query, caseSensitive) { findAll(text, query, caseSensitive) }
        val safeIdx = if (matches.isEmpty()) 0 else currentIdx.coerceIn(0, matches.lastIndex)

        val vScroll = tab?.let { scrollStates.getOrPut(it.path) { ScrollState(0) } }
        val lineHeightPx = with(density) { 20.sp.toPx() }

        fun goTo(idx: Int) {
            val t = tab ?: return
            if (matches.isEmpty()) return
            val i = ((idx % matches.size) + matches.size) % matches.size
            currentIdx = i
            val r = matches[i]
            t.setSelection(TextRange(r.first, r.last + 1))
            val line = text.substring(0, r.first).count { it == '\n' }
            val target = (line * lineHeightPx - lineHeightPx * 6).toInt().coerceAtLeast(0)
            scope.launch { vScroll?.animateScrollTo(target) }
        }

        if (!fs && showSearch && tab != null) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(c.bg2)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SearchField(
                        value = query,
                        onValueChange = { query = it; currentIdx = 0 },
                        placeholder = "Rechercher",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (query.isEmpty()) "" else if (matches.isEmpty()) "0" else "${safeIdx + 1}/${matches.size}",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp),
                        color = c.text2
                    )
                    Spacer(Modifier.width(4.dp))
                    SmallIcon(Icons.Default.KeyboardArrowUp, "Précédent") { goTo(safeIdx - 1) }
                    SmallIcon(Icons.Default.KeyboardArrowDown, "Suivant") { goTo(safeIdx + 1) }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (caseSensitive) c.cyanBg else c.surface2)
                            .clickable { caseSensitive = !caseSensitive }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "Aa",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                            color = if (caseSensitive) c.cyan else c.text2
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SearchField(
                        value = replacement,
                        onValueChange = { replacement = it },
                        placeholder = "Remplacer par",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        enabled = matches.isNotEmpty(),
                        onClick = {
                            val r = matches[safeIdx]
                            val newText = text.replaceRange(r.first, r.last + 1, replacement)
                            tab.applyEdit(TextFieldValue(newText, TextRange(r.first + replacement.length)))
                        }
                    ) { Text("Remplacer", color = if (matches.isNotEmpty()) c.cyan else c.text3, fontSize = 12.sp) }
                    TextButton(
                        enabled = matches.isNotEmpty(),
                        onClick = {
                            val sb = StringBuilder()
                            var last = 0
                            for (r in matches) {
                                sb.append(text, last, r.first)
                                sb.append(replacement)
                                last = r.last + 1
                            }
                            sb.append(text, last, text.length)
                            tab.applyEdit(TextFieldValue(sb.toString(), TextRange(0)))
                        }
                    ) { Text("Tout", color = if (matches.isNotEmpty()) c.cyan else c.text3, fontSize = 12.sp) }
                }
            }
        }

        // ── Bandeaux d'erreur / conflit ─────────────────────────────────────
        editor.error?.takeIf { !fs }?.let { err ->
            Text(
                err,
                color = c.red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { editor.error = null }
                    .background(c.redBg)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
        if (!fs && tab != null && tab.externalConflict) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(c.amberBg)
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "L'agent a modifié ce fichier.",
                    style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
                    color = c.amber,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { editor.reloadFromDisk(tab) }) { Text("Recharger", color = c.amber, fontSize = 12.sp) }
                TextButton(onClick = { tab.externalConflict = false }) { Text("Garder le mien", color = c.text2, fontSize = 12.sp) }
            }
        }

        // ── Zone d'édition ──────────────────────────────────────────────────
        if (!(showPreview && previewFull)) Box(Modifier.weight(1f).fillMaxWidth()) {
            if (tab == null || vScroll == null) {
                Column(
                    Modifier.fillMaxSize().background(EditorBg),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (editor.loading) "Ouverture…" else "Aucun fichier ouvert.",
                        style = TextStyle(fontFamily = Inter, fontSize = 14.sp),
                        color = c.text2
                    )
                    if (!editor.loading) {
                        TextButton(onClick = onBack) { Text("Ouvrir depuis l'explorateur", color = c.cyan) }
                    }
                }
            } else {
                CodeEditor(
                    tab = tab,
                    vScroll = vScroll,
                    matches = matches,
                    current = safeIdx,
                    onSave = { editor.save(tab) }
                )
            }
        }

        // ── Aperçu web (masqué pendant la saisie en mode partagé) ───────────
        if (showPreview && (previewFull || WindowInsets.ime.getBottom(density) == 0)) {
            val entry = previewPath ?: "index.html"
            Column(Modifier.weight(1f).fillMaxWidth()) {
                if (!fs) Row(
                    Modifier
                        .fillMaxWidth()
                        .background(c.bg2)
                        .padding(start = 12.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Aperçu · " + entry,
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp),
                        color = c.text2,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    SmallIcon(Icons.Default.Refresh, "Recharger") { reloadTick++ }
                    SmallIcon(Icons.Default.Terminal, "Console") { showConsole = !showConsole }
                    SmallIcon(
                        if (previewFull) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        "Plein écran"
                    ) { previewFull = !previewFull }
                    SmallIcon(Icons.Default.Close, "Fermer l'aperçu") {
                        showPreview = false
                        previewFull = false
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    PreviewPane(
                        path = entry,
                        reloadKey = editor.saveCount + reloadTick + viewModel.previewTick,
                        readResource = editor::readResource,
                        console = console,
                        dark = viewModel.themeIsDark,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (fs) {
                        Row(
                            Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0x99000000)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SmallIcon(Icons.Default.Refresh, "Recharger") { reloadTick++ }
                            SmallIcon(Icons.Default.FullscreenExit, "Quitter le plein écran") { previewFull = false }
                        }
                    }
                }
                if (showConsole && !fs) {
                    Row(
                        Modifier.fillMaxWidth().background(c.surface2).padding(start = 12.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val errs = console.errorCount
                        Text(
                            "Console" + if (errs > 0) " · $errs erreur" + (if (errs > 1) "s" else "") else "",
                            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp),
                            color = if (errs > 0) c.red else c.text2,
                            modifier = Modifier.weight(1f)
                        )
                        if (errs > 0) {
                            TextButton(onClick = {
                                val msg = "La page " + entry + " a des erreurs dans la console :\n" +
                                    console.errorsAsText() + "\nCorrige-les."
                                viewModel.isEditorOpen = false
                                viewModel.sendMessage(msg)
                            }) { Text("Corriger avec l'IA", color = c.cyan, fontSize = 12.sp) }
                        }
                        SmallIcon(Icons.Default.Delete, "Vider la console") { console.clear() }
                    }
                    ConsolePanel(console, Modifier.height(140.dp))
                }
            }
        }

        // ── Barre d'état ────────────────────────────────────────────────────
        if (!fs && tab != null) {
            val sel = tab.value.selection
            val pos = sel.start.coerceIn(0, text.length)
            val before = text.substring(0, pos)
            val line = before.count { it == '\n' } + 1
            val col = pos - (before.lastIndexOf('\n') + 1) + 1
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(c.bg2)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Ln $line, Col $col",
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp),
                    color = c.text2
                )
                Spacer(Modifier.weight(1f))
                val note = editor.info
                    ?: if (text.length > SyntaxHighlighter.MAX_CHARS) "Coloration désactivée (fichier long)" else null
                if (note != null) {
                    Text(note, style = TextStyle(fontFamily = Inter, fontSize = 11.sp), color = c.green)
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    SyntaxHighlighter.label(tab.lang) + if (tab.dirty) " · modifié" else "",
                    style = TextStyle(fontFamily = Inter, fontSize = 11.sp),
                    color = if (tab.dirty) c.amber else c.text2
                )
            }
        }

        // ── Barre de symboles (visible avec le clavier) ─────────────────────
        if (tab != null && WindowInsets.ime.getBottom(density) > 0) {
            SymbolBar(onInsert = { tab.insert(it) })
        }
    }

    // ── Confirmation de fermeture d'un onglet modifié ───────────────────────
    confirmClose?.let { index ->
        val t = editor.tabs.getOrNull(index)
        if (t == null) {
            confirmClose = null
        } else {
            AlertDialog(
                onDismissRequest = { confirmClose = null },
                containerColor = c.bg2,
                title = { Text("Modifications non enregistrées", color = c.text, fontFamily = Inter, fontSize = 17.sp) },
                text = { Text(t.name, color = c.text2, fontFamily = JetBrainsMono, fontSize = 13.sp) },
                confirmButton = {
                    TextButton(onClick = {
                        editor.save(t) { ok -> if (ok) editor.close(editor.tabs.indexOf(t)) }
                        confirmClose = null
                    }) { Text("Enregistrer", color = c.cyan) }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { confirmClose = null }) { Text("Annuler", color = c.text2) }
                        TextButton(onClick = {
                            editor.close(index)
                            confirmClose = null
                        }) { Text("Ignorer", color = c.red) }
                    }
                }
            )
        }
    }
}

@Composable
private fun SmallIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    val c = Zcolors
    Box(
        Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, desc, tint = c.text2, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val c = Zcolors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = c.text),
        cursorBrush = SolidColor(c.cyan),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false
        ),
        modifier = modifier,
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.surface2)
                    .border(1.dp, c.border, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, style = TextStyle(fontFamily = Inter, fontSize = 13.sp), color = c.text3)
                }
                inner()
            }
        }
    )
}

@Composable
private fun CodeEditor(
    tab: EditorTab,
    vScroll: ScrollState,
    matches: List<IntRange>,
    current: Int,
    onSave: () -> Unit
) {
    val c = Zcolors
    val lineStyle = TextStyle(
        fontFamily = JetBrainsMono,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        color = EditorText
    )
    val hScroll = rememberScrollState()
    val focus = remember { FocusRequester() }
    val text = tab.value.text
    val lineCount = remember(text) { text.count { it == '\n' } + 1 }
    val gutterText = remember(lineCount) { (1..lineCount).joinToString("\n") }
    val digits = lineCount.toString().length.coerceAtLeast(2)
    val gutterWidth = (digits * 8 + 22).dp
    val transformation = remember(tab.lang, matches, current) { CodeTransformation(tab.lang, matches, current) }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(EditorBg)
    ) {
        val viewportHeight = maxHeight
        val fieldMinWidth = (maxWidth - gutterWidth).coerceAtLeast(120.dp)

        Row(
            Modifier
                .fillMaxSize()
                .verticalScroll(vScroll)
        ) {
            // Gouttière des numéros de ligne
            Text(
                gutterText,
                style = lineStyle.copy(color = GutterText, textAlign = TextAlign.End),
                modifier = Modifier
                    .width(gutterWidth)
                    .heightIn(min = viewportHeight)
                    .background(Color(0xFF0B1120))
                    .padding(top = 10.dp, end = 10.dp, start = 4.dp)
            )
            // Champ de saisie, sans retour à la ligne (défilement horizontal)
            BasicTextField(
                value = tab.value,
                onValueChange = { tab.onChange(it) },
                textStyle = lineStyle,
                cursorBrush = SolidColor(c.cyan),
                visualTransformation = transformation,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false
                ),
                modifier = Modifier
                    .horizontalScroll(hScroll)
                    .widthIn(min = fieldMinWidth)
                    .heightIn(min = viewportHeight)
                    .padding(start = 10.dp, end = 40.dp, top = 10.dp, bottom = 120.dp)
                    .focusRequester(focus)
                    .onPreviewKeyEvent { e ->
                        if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when {
                            e.key == Key.Tab -> { tab.insert("  "); true }
                            e.isCtrlPressed && e.key == Key.Z -> { tab.undo(); true }
                            e.isCtrlPressed && e.key == Key.Y -> { tab.redo(); true }
                            e.isCtrlPressed && e.key == Key.S -> { onSave(); true }
                            else -> false
                        }
                    }
            )
        }
    }
}

private val SYMBOLS = listOf(
    "Tab" to "  ", "{" to "{", "}" to "}", "(" to "(", ")" to ")", "[" to "[", "]" to "]",
    ";" to ";", "<" to "<", ">" to ">", "\"" to "\"", "'" to "'", "=" to "=", "/" to "/"
)

@Composable
private fun SymbolBar(onInsert: (String) -> Unit) {
    val c = Zcolors
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.bg2)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SYMBOLS.forEach { (label, value) ->
            Box(
                Modifier
                    .height(36.dp)
                    .widthIn(min = if (label == "Tab") 52.dp else 38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(c.surface2)
                    .clickable { onInsert(value) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                    color = c.text
                )
            }
        }
    }
}
