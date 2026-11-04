package com.zagent.app.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.ui.editor.EditorTab
import com.zagent.app.ui.editor.Kind
import com.zagent.app.ui.editor.Lang
import com.zagent.app.ui.editor.SyntaxHighlighter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.LocalGlass
import com.zagent.app.ui.theme.LocalHazeState
import com.zagent.app.ui.theme.ZText3Dark
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.theme.glassSurface
import kotlin.math.abs
import kotlin.math.roundToInt

// Vue de code : coloration claire/sombre (MA81), éditeur avec gouttière, ligne
// courante et guides d'indentation (MA81), zoom par pincement (MA83), barre de
// symboles complète avec annuler/refaire (MA80). Découpé de EditorScreen.kt (MA79).

private val MatchBg = Color(0x55FBBF24)
private val CurrentMatchBg = Color(0xAAFBBF24)

// ─────────────────────────────────────────────────────────────────────────────
// MA81 — Deux thèmes de coloration : sombre (rendu habituel, fond nuit) et
// clair (tons profonds, lisibles sur un fond de code clair).
// ─────────────────────────────────────────────────────────────────────────────

/** Palette « nuit » : couleurs actuelles de l'éditeur, inchangées. */
private val DarkSyntax: Map<Kind, SpanStyle> = mapOf(
    Kind.COMMENT to SpanStyle(color = Color(0xFF6B7A99), fontStyle = FontStyle.Italic),
    Kind.STRING to SpanStyle(color = Color(0xFF9ECE6A)),
    Kind.NUMBER to SpanStyle(color = Color(0xFFFF9E64)),
    Kind.KEYWORD to SpanStyle(color = Color(0xFFBB9AF7)),
    Kind.TYPE to SpanStyle(color = Color(0xFFE0AF68)),
    Kind.FUNCTION to SpanStyle(color = Color(0xFF7AA2F7)),
    Kind.TAG to SpanStyle(color = Color(0xFFF7768E)),
    Kind.ATTR to SpanStyle(color = Color(0xFF73DACA)),
    Kind.PROP to SpanStyle(color = Color(0xFF7DCFFF)),
    Kind.LITERAL to SpanStyle(color = Color(0xFFFF9E64)),
    Kind.HEADING to SpanStyle(color = Color(0xFF7AA2F7), fontWeight = FontWeight.SemiBold),
    Kind.EMPH to SpanStyle(color = Color(0xFFE0AF68), fontWeight = FontWeight.SemiBold)
)

/** Palette « jour » : tons plus profonds pour rester lisibles sur fond clair. */
private val LightSyntax: Map<Kind, SpanStyle> = mapOf(
    Kind.COMMENT to SpanStyle(color = Color(0xFF8B98AD), fontStyle = FontStyle.Italic),
    Kind.STRING to SpanStyle(color = Color(0xFF0E9F6E)),
    Kind.NUMBER to SpanStyle(color = Color(0xFFC2410C)),
    Kind.KEYWORD to SpanStyle(color = Color(0xFF7C3AED)),
    Kind.TYPE to SpanStyle(color = Color(0xFFB45309)),
    Kind.FUNCTION to SpanStyle(color = Color(0xFF1D4ED8)),
    Kind.TAG to SpanStyle(color = Color(0xFFBE123C)),
    Kind.ATTR to SpanStyle(color = Color(0xFF0F766E)),
    Kind.PROP to SpanStyle(color = Color(0xFF0369A1)),
    Kind.LITERAL to SpanStyle(color = Color(0xFFC2410C)),
    Kind.HEADING to SpanStyle(color = Color(0xFF1D4ED8), fontWeight = FontWeight.SemiBold),
    Kind.EMPH to SpanStyle(color = Color(0xFFB45309), fontWeight = FontWeight.SemiBold)
)

internal fun styleFor(kind: Kind, dark: Boolean): SpanStyle =
    (if (dark) DarkSyntax else LightSyntax).getValue(kind)

internal class CodeTransformation(
    private val lang: Lang,
    private val matches: List<IntRange>,
    private val current: Int,
    private val dark: Boolean
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val s = text.text
        val b = AnnotatedString.Builder(s)
        for (sp in SyntaxHighlighter.spans(s, lang)) {
            if (sp.start >= 0 && sp.end <= s.length && sp.start < sp.end) {
                b.addStyle(styleFor(sp.kind, dark), sp.start, sp.end)
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

/** MA82 : occurrences de [query] ; [wholeWord] exige un mot entier (bordures lettre/chiffre/_). */
internal fun findAll(
    text: String,
    query: String,
    caseSensitive: Boolean,
    wholeWord: Boolean = false
): List<IntRange> {
    if (query.isEmpty()) return emptyList()
    fun isWordChar(ch: Char) = ch.isLetterOrDigit() || ch == '_'
    val out = ArrayList<IntRange>()
    var i = text.indexOf(query, 0, ignoreCase = !caseSensitive)
    while (i >= 0 && out.size < 5000) {
        val end = i + query.length
        val leftOk = !wholeWord || i == 0 || !isWordChar(text[i - 1])
        val rightOk = !wholeWord || end >= text.length || !isWordChar(text[end])
        if (leftOk && rightOk) {
            out.add(i until end)
        }
        i = text.indexOf(query, end, ignoreCase = !caseSensitive)
    }
    return out
}

/**
 * MA83 : pincement à deux doigts, variante de detectTransformGestures qui ne
 * consomme les événements qu'à partir de deux pointeurs — le défilement d'une
 * ligne et la sélection au doigt restent intacts.
 */
private fun Modifier.twoFingerZoom(onZoom: (Float) -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var previousSpan = -1f
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val pressed = event.changes.filter { it.pressed }
            if (pressed.size >= 2) {
                val span = (pressed[0].position - pressed[1].position).getDistance()
                if (previousSpan > 0f && span > 0f) {
                    val zoom = span / previousSpan
                    if (abs(zoom - 1f) > 0.01f) {
                        onZoom(zoom)
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    }
                }
                previousSpan = span
            } else {
                previousSpan = -1f
            }
            if (event.changes.none { it.pressed }) break
        }
    }
}

@Composable
internal fun CodeEditor(
    tab: EditorTab,
    vScroll: ScrollState,
    matches: List<IntRange>,
    current: Int,
    fontSize: Int,
    codeAlwaysDark: Boolean,
    onFontSize: (Int) -> Unit,
    onSave: () -> Unit
) {
    val c = Zcolors
    val density = LocalDensity.current
    // MA81 : le code reste sur fond nuit, sauf si l'utilisateur l'autorise à suivre le thème clair.
    val darkCode = c.isDark || codeAlwaysDark
    val fieldBg = if (darkCode && !c.isDark) c.codeBg else c.bg
    val gutterBg = if (darkCode && !c.isDark) c.codeBg else c.bg2
    val codeText = if (darkCode && !c.isDark) c.codePlain else c.text
    val gutterText = if (darkCode) ZText3Dark else c.text3
    val currentLineBg = if (darkCode) Color(0x14E9EEF8) else Color(0x120D1930)
    val guideColor = if (darkCode) Color(0x2494A3BA) else Color(0x240D1930)

    // MA83 : hauteur de ligne proportionnelle (13 sp → 20 sp, comme avant).
    val lineHeight = (fontSize + 7).sp
    val lineStyle = TextStyle(
        fontFamily = JetBrainsMono,
        fontSize = fontSize.sp,
        lineHeight = lineHeight,
        color = codeText
    )
    val hScroll = rememberScrollState()
    val focus = remember { FocusRequester() }
    val text = tab.value.text
    val lineCount = remember(text) { text.count { it == '\n' } + 1 }
    val gutterTextValue = remember(lineCount) { (1..lineCount).joinToString("\n") }
    val digits = lineCount.toString().length.coerceAtLeast(2)

    // Largeur exacte d'un caractère mono (gouttière + guides), mesurée avec la police du code.
    val textMeasurer = rememberTextMeasurer()
    val charWidthPx = remember(fontSize) {
        textMeasurer.measure("0".repeat(8), lineStyle).size.width / 8f
    }
    val gutterWidth = with(density) { (digits * charWidthPx + 24.dp.toPx()).toDp() }

    val transformation = remember(tab.lang, matches, current, darkCode) {
        CodeTransformation(tab.lang, matches, current, darkCode)
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(fieldBg)
            // MA83 : zoom par pincement (10 à 22 sp).
            .twoFingerZoom { zoom ->
                val target = (fontSize * zoom).roundToInt().coerceIn(10, 22)
                if (target != fontSize) onFontSize(target)
            }
    ) {
        val viewportHeight = maxHeight
        val viewportPx = with(density) { viewportHeight.toPx() }
        val fieldMinWidth = (maxWidth - gutterWidth).coerceAtLeast(120.dp)

        Row(
            Modifier
                .fillMaxSize()
                .verticalScroll(vScroll)
        ) {
            // Gouttière des numéros de ligne (MA81) : gris, alignée sur le texte.
            Text(
                gutterTextValue,
                style = lineStyle.copy(color = gutterText, textAlign = TextAlign.End),
                modifier = Modifier
                    .widthIn(min = gutterWidth)
                    .heightIn(min = viewportHeight)
                    .background(gutterBg)
                    .padding(top = 10.dp, end = 10.dp, start = 4.dp)
            )
            // Champ de saisie, sans retour à la ligne (défilement horizontal).
            // MA81 : ligne courante surlignée + guides d'indentation, dessinés
            // dans l'espace du texte (mêmes coordonnées que le TextLayoutResult).
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
                onTextLayout = { layout = it },
                modifier = Modifier
                    .horizontalScroll(hScroll)
                    .widthIn(min = fieldMinWidth)
                    .heightIn(min = viewportHeight)
                    .padding(start = 10.dp, end = 40.dp, top = 10.dp, bottom = 120.dp)
                    .drawBehind {
                        val l = layout ?: return@drawBehind
                        // ── Ligne courante légèrement surlignée (MA81) ──
                        val selOffset = tab.value.selection.min.coerceIn(0, text.length)
                        val selLine = l.getLineForOffset(selOffset)
                        if (selLine in 0 until l.lineCount) {
                            val top = l.getLineTop(selLine)
                            val bottom = l.getLineBottom(selLine)
                            if (bottom > top) {
                                drawRect(
                                    color = currentLineBg,
                                    topLeft = Offset(0f, top),
                                    size = Size(size.width, bottom - top)
                                )
                            }
                        }
                        // ── Guides d'indentation fins (MA81) ──
                        // Un guide tous les deux espaces ; seules les lignes
                        // visibles sont dessinées pour rester fluide.
                        if (l.lineCount > 1 && charWidthPx > 0f) {
                            val lineH = l.getLineTop(1) - l.getLineTop(0)
                            if (lineH > 0f) {
                                val scrollY = vScroll.value
                                val firstVisible = (scrollY / lineH).toInt() - 1
                                val lastVisible = ((scrollY + viewportPx) / lineH).toInt() + 1
                                val first = firstVisible.coerceAtLeast(0)
                                val last = lastVisible.coerceIn(0, l.lineCount - 1)
                                var offset = 0
                                for (line in 0..last) {
                                    val lineEnd = text.indexOf('\n', offset)
                                    if (line >= first) {
                                        val lineText = if (lineEnd == -1) {
                                            text.substring(offset)
                                        } else {
                                            text.substring(offset, lineEnd)
                                        }
                                        val indent = lineText.takeWhile { it == ' ' }.length / 2
                                        if (indent > 0) {
                                            val y0 = l.getLineTop(line)
                                            val y1 = y0 + lineH
                                            for (k in 1..indent) {
                                                val x = k * 2f * charWidthPx
                                                drawLine(
                                                    color = guideColor,
                                                    start = Offset(x, y0),
                                                    end = Offset(x, y1),
                                                    strokeWidth = 1.dp.toPx()
                                                )
                                            }
                                        }
                                    }
                                    if (lineEnd == -1) break
                                    offset = lineEnd + 1
                                }
                            }
                        }
                    }
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

// ─────────────────────────────────────────────────────────────────────────────
// MA80 — Barre de symboles au-dessus du clavier : touches manquantes sur mobile
// (Tab = 2 espaces, accolades, chevrons, deux-points, antislash, souligné,
// virgule…) puis annuler / refaire. Fond verre ou surface selon le style.
// ─────────────────────────────────────────────────────────────────────────────

internal val SYMBOLS = listOf(
    "Tab" to "  ", "{" to "{", "}" to "}", "(" to "(", ")" to ")", "[" to "[", "]" to "]",
    "<" to "<", ">" to ">", ";" to ";", ":" to ":", "\"" to "\"", "'" to "'", "=" to "=",
    "/" to "/", "\\" to "\\", "_" to "_", "," to ","
)

@Composable
internal fun SymbolBar(
    onInsert: (String) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean
) {
    val c = Zcolors
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .glassSurface(
                LocalHazeState.current,
                LocalGlass.current,
                c.isDark,
                RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                context
            )
            .background(if (LocalGlass.current.enabled) c.bg2.copy(alpha = 0.55f) else c.bg2)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SYMBOLS.forEach { (label, value) ->
            SymbolKey(label = label, onClick = { onInsert(value) })
        }
        Spacer(Modifier.width(2.dp))
        SymbolKey(
            icon = Icons.AutoMirrored.Filled.Undo,
            description = "Annuler",
            enabled = canUndo,
            onClick = onUndo
        )
        SymbolKey(
            icon = Icons.AutoMirrored.Filled.Redo,
            description = "Refaire",
            enabled = canRedo,
            onClick = onRedo
        )
    }
}

@Composable
private fun SymbolKey(
    label: String? = null,
    icon: ImageVector? = null,
    description: String = label ?: "",
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val c = Zcolors
    val tint = if (enabled) c.text else c.text3.copy(alpha = 0.5f)
    Box(
        Modifier
            .height(36.dp)
            .widthIn(min = if (label == "Tab") 52.dp else 38.dp)
            .clip(MaterialTheme.shapes.small)
            .background(if (enabled) c.surface2 else c.surface2.copy(alpha = 0.5f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(icon, description, tint = tint, modifier = Modifier.size(18.dp))
        } else if (label != null) {
            Text(
                label,
                style = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                color = tint
            )
        }
    }
}
