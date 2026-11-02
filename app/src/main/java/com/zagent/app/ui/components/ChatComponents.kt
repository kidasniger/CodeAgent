package com.zagent.app.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.data.DiffUtil
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.Zcolors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Puces d'outils (list_dir · read_file · make_dir · write_file) — MA34
// ─────────────────────────────────────────────────────────────────────────────

enum class ToolState { RUNNING, OK, ERROR, REFUSED }

/** Raccourcit un chemin long pour la ligne compacte des outils : « app/src/…/Fichier.kt ». */
fun shortenPath(path: String, max: Int = 34): String {
    val p = path.trim().trimStart('/')
    if (p.length <= max) return p
    val name = p.substringAfterLast('/')
    return "…/" + name
}

@Composable
fun ToolChip(
    name: String,
    arg: String?,
    state: ToolState,
    resultText: String?,
    expandKey: String,
    modifier: Modifier = Modifier
) {
    val c = Zcolors
    var expanded by rememberSaveable(expandKey) { mutableStateOf(false) }
    val hasResult = !resultText.isNullOrBlank()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable(enabled = hasResult) { expanded = !expanded }
                .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (state) {
                ToolState.RUNNING -> CircularProgressIndicator(
                    modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = c.cyan
                )
                ToolState.OK -> Icon(Icons.Default.Check, null, tint = c.green, modifier = Modifier.size(13.dp))
                ToolState.ERROR -> Icon(Icons.Default.Close, null, tint = c.red, modifier = Modifier.size(13.dp))
                ToolState.REFUSED -> Icon(Icons.Default.Block, null, tint = c.amber, modifier = Modifier.size(13.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(
                name,
                style = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 12.sp),
                color = c.text2
            )
            if (!arg.isNullOrBlank()) {
                Spacer(Modifier.width(6.dp))
                Text(
                    shortenPath(arg),
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp),
                    color = c.text3,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            if (hasResult) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Détail",
                    tint = c.text3, modifier = Modifier.size(15.dp)
                )
            }
        }

        if (expanded && hasResult) {
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(c.surface2)
                    .padding(10.dp)
            ) {
                val preview = resultText!!.lines().take(8).joinToString("\n").take(1500)
                Text(
                    preview,
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, lineHeight = 15.sp),
                    color = c.text2
                )
            }
        }
    }
}

/** Regroupement façon Claude : « 4 outils utilisés », tap pour déplier la liste (MA34). */
@Composable
fun ToolGroupSummary(count: Int, expanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val c = Zcolors
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onToggle)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$count outils utilisés",
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.5.sp),
            color = c.text2
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (expanded) "Replier" else "Déplier",
            tint = c.text3, modifier = Modifier.size(15.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Markdown : spans en ligne (gras, italique, barré, code, liens, chemins) — MA31/MA46
// ─────────────────────────────────────────────────────────────────────────────

sealed class MdSpan {
    data class Plain(val text: String) : MdSpan()
    data class Code(val text: String) : MdSpan()
    data class Bold(val spans: List<MdSpan>) : MdSpan()
    data class Italic(val spans: List<MdSpan>) : MdSpan()
    data class Strike(val spans: List<MdSpan>) : MdSpan()
    data class Link(val label: String, val url: String) : MdSpan()
    data class File(val path: String, val line: Int?) : MdSpan()
}

private val SPAN_REGEX = Regex(
    "`([^`\n]+)`"                              // 1 : code
        + "|\\*\\*([^*\n]+)\\*\\*"             // 2 : gras
        + "|~~([^~\n]+)~~"                     // 3 : barré
        + "|\\*([^*\n]+)\\*"                   // 4 : italique
        + "|\\[([^\\]\n]+)\\]\\(([^)\\s]+)\\)" // 5+6 : lien
)

/** Chemins de fichiers éventuellement suivis de « :ligne » (MA46). */
private val PATH_REGEX = Regex(
    "(?<![\\w./\\-])((?:[\\w.-]+/)*[\\w.-]+\\.(?:kt|kts|java|xml|gradle|py|js|ts|tsx|jsx|md|json|html|css|sh|txt|yaml|yml|toml))(?::(\\d+))?"
)

private fun parsePlainWithPaths(text: String): List<MdSpan> {
    if (text.isEmpty()) return emptyList()
    val out = mutableListOf<MdSpan>()
    var last = 0
    for (m in PATH_REGEX.findAll(text)) {
        if (m.range.first > last) out.add(MdSpan.Plain(text.substring(last, m.range.first)))
        out.add(MdSpan.File(m.groupValues[1], m.groupValues[2]?.toIntOrNull()))
        last = m.range.last + 1
    }
    if (last < text.length) out.add(MdSpan.Plain(text.substring(last)))
    return out
}

/** Découpe une ligne en spans enrichis (une passe de mise en forme, puis détection des chemins). */
fun parseInlineSpans(text: String): List<MdSpan> {
    val out = mutableListOf<MdSpan>()
    var last = 0
    for (m in SPAN_REGEX.findAll(text)) {
        if (m.range.first > last) out.addAll(parsePlainWithPaths(text.substring(last, m.range.first)))
        when {
            m.groupValues[1].isNotEmpty() -> out.add(MdSpan.Code(m.groupValues[1]))
            m.groupValues[2].isNotEmpty() -> out.add(MdSpan.Bold(parseInlineSpans(m.groupValues[2])))
            m.groupValues[3].isNotEmpty() -> out.add(MdSpan.Strike(parseInlineSpans(m.groupValues[3])))
            m.groupValues[4].isNotEmpty() -> out.add(MdSpan.Italic(parseInlineSpans(m.groupValues[4])))
            m.groupValues[5].isNotEmpty() -> out.add(MdSpan.Link(m.groupValues[5], m.groupValues[6]))
        }
        last = m.range.last + 1
    }
    if (last < text.length) out.addAll(parsePlainWithPaths(text.substring(last)))
    return out
}

/** Version compatible du rendu en ligne (bulles utilisateur, chips) : gras + code + liens + chemins. */
fun buildInlineMarkdown(
    text: String,
    baseColor: Color,
    codeColor: Color,
    codeBg: Color?,
    bold: Boolean = false
): AnnotatedString = annotatedFromSpans(
    parseInlineSpans(text), baseColor, codeColor, codeBg,
    bold = bold, baseFontSize = 15.sp
)

private fun AnnotatedString.Builder.appendSpans(
    spans: List<MdSpan>,
    baseColor: Color,
    codeColor: Color,
    codeBg: Color?,
    bold: Boolean,
    italic: Boolean,
    baseFontSize: TextUnit
) {
    spans.forEach { span ->
        when (span) {
            is MdSpan.Plain -> append(span.text)
            is MdSpan.Code -> {
                pushStringAnnotation("code", span.text)
                withStyle(
                    SpanStyle(
                        fontFamily = JetBrainsMono,
                        fontSize = baseFontSize * 0.86f,
                        color = codeColor,
                        background = codeBg ?: Color.Unspecified
                    )
                ) { append(span.text) }
                pop()
            }
            is MdSpan.Bold -> appendSpans(span.spans, baseColor, codeColor, codeBg, bold = true, italic = italic, baseFontSize = baseFontSize)
            is MdSpan.Italic -> appendSpans(span.spans, baseColor, codeColor, codeBg, bold = bold, italic = true, baseFontSize = baseFontSize)
            is MdSpan.Strike -> withStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)) {
                appendSpans(span.spans, baseColor, codeColor, codeBg, bold = bold, italic = italic, baseFontSize = baseFontSize)
            }
            is MdSpan.Link -> {
                pushStringAnnotation("url", span.url)
                withStyle(SpanStyle(color = baseColor, fontWeight = FontWeight.SemiBold, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) {
                    append(span.label)
                }
                pop()
            }
            is MdSpan.File -> {
                pushStringAnnotation("file", span.path + (span.line?.let { ":$it" } ?: ""))
                withStyle(
                    SpanStyle(
                        fontFamily = JetBrainsMono,
                        fontSize = baseFontSize * 0.9f,
                        color = baseColor,
                        background = codeBg ?: Color.Unspecified,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    )
                ) { append(span.path + (span.line?.let { ":$it" } ?: "")) }
                pop()
            }
        }
    }
}

private fun annotatedFromSpans(
    spans: List<MdSpan>,
    baseColor: Color,
    codeColor: Color,
    codeBg: Color?,
    bold: Boolean,
    baseFontSize: TextUnit
): AnnotatedString = buildAnnotatedString {
    appendSpans(spans, baseColor, codeColor, codeBg, bold = bold, italic = false, baseFontSize = baseFontSize)
}

// ─────────────────────────────────────────────────────────────────────────────
// Markdown : blocs (titres, listes, citations, tableaux, séparateurs) — MA31
// ─────────────────────────────────────────────────────────────────────────────

sealed class MdBlock {
    data class Heading(val level: Int, val spans: List<MdSpan>) : MdBlock()
    data class Paragraph(val spans: List<MdSpan>) : MdBlock()
    data class ListBlock(val ordered: Boolean, val items: List<Item>) : MdBlock() {
        data class Item(val marker: String, val spans: List<MdSpan>, val level: Int)
    }
    data class Quote(val spans: List<MdSpan>) : MdBlock()
    data class Table(val header: List<String>, val rows: List<List<String>>) : MdBlock()
    object Divider : MdBlock()
}

fun parseMarkdownBlocks(text: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = text.replace("\r\n", "\n").split('\n')
    var i = 0
    val paragraph = StringBuilder()
    fun flushParagraph() {
        if (paragraph.isNotBlank()) blocks.add(MdBlock.Paragraph(parseInlineSpans(paragraph.toString().trim())))
        paragraph.clear()
    }
    while (i < lines.size) {
        val raw = lines[i]
        val line = raw.trimEnd()
        val trimmed = line.trim()
        when {
            trimmed.isEmpty() -> { flushParagraph() }
            trimmed.startsWith("```") -> {
                // Clôture de bloc isolée (le contenu fenced est déjà découpé par splitContent).
                while (i < lines.size && !lines[i].trim().startsWith("```")) i++
            }
            Regex("^#{1,6}\\s+").containsMatchIn(trimmed) -> {
                flushParagraph()
                val level = trimmed.takeWhile { it == '#' }.length.coerceAtMost(3)
                blocks.add(MdBlock.Heading(level, parseInlineSpans(trimmed.dropWhile { it == '#' }.trim())))
            }
            trimmed.startsWith("---") || trimmed.startsWith("***") || trimmed.startsWith("___") -> {
                flushParagraph()
                blocks.add(MdBlock.Divider)
            }
            trimmed.startsWith(">") -> {
                flushParagraph()
                val quote = StringBuilder(trimmed.removePrefix(">").trim())
                while (i + 1 < lines.size && lines[i + 1].trim().startsWith(">")) {
                    i++
                    quote.append('\n').append(lines[i].trim().removePrefix(">").trim())
                }
                blocks.add(MdBlock.Quote(parseInlineSpans(quote.toString())))
            }
            trimmed.startsWith("|") && i + 1 < lines.size && Regex("^\\|?[\\s:-]*-[-\\s:|]*$").containsMatchIn(lines[i + 1].trim()) -> {
                flushParagraph()
                fun cells(row: String): List<String> =
                    row.trim().trim('|').split('|').map { it.trim() }
                val header = cells(trimmed)
                i += 2
                val rows = mutableListOf<List<String>>()
                while (i < lines.size && lines[i].trim().startsWith("|")) {
                    rows.add(cells(lines[i]))
                    i++
                }
                blocks.add(MdBlock.Table(header, rows))
                continue
            }
            Regex("^\\s*([-*+]|\\d+[.)])\\s+").containsMatchIn(line) -> {
                flushParagraph()
                val ordered = Regex("^\\s*\\d+[.)]\\s+").containsMatchIn(line)
                val items = mutableListOf<MdBlock.ListBlock.Item>()
                while (i < lines.size) {
                    val m = Regex("^(\\s*)([-*+]|\\d+[.)])\\s+(.*)$").find(lines[i]) ?: break
                    val level = (m.groupValues[1].length / 2).coerceAtMost(2)
                    val marker = if (ordered) m.groupValues[2].substringBefore('.') + "." else "•"
                    items.add(MdBlock.ListBlock.Item(marker, parseInlineSpans(m.groupValues[3]), level))
                    i++
                }
                blocks.add(MdBlock.ListBlock(ordered, items))
                continue
            }
            else -> {
                if (paragraph.isNotEmpty()) paragraph.append('\n')
                paragraph.append(trimmed)
            }
        }
        i++
    }
    flushParagraph()
    return blocks
}

/** Texte tapable : chemins de fichiers → [onFileTap], liens → navigateur (MA46). */
@Composable
private fun AnnotatedText(
    spans: List<MdSpan>,
    modifier: Modifier = Modifier,
    baseColor: Color = Zcolors.text,
    codeBg: Color? = Zcolors.cyanBg,
    fontSize: TextUnit = 16.sp,
    lineHeight: TextUnit = 26.sp,
    bold: Boolean = false,
    italic: Boolean = false,
    onFileTap: (String, Int?) -> Unit = { _, _ -> }
) {
    val c = Zcolors
    val annotated = remember(spans, baseColor, bold, italic) {
        buildAnnotatedString {
            appendSpans(spans, baseColor, baseColor, codeBg, bold = bold, italic = italic, baseFontSize = fontSize)
        }
    }
    val layout = remember { mutableStateOf<TextLayoutResult?>(null) }
    val uriHandler = LocalUriHandler.current
    Text(
        annotated,
        style = TextStyle(
            fontFamily = Inter,
            fontSize = fontSize,
            lineHeight = lineHeight,
            color = baseColor,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            fontStyle = if (italic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
        ),
        onTextLayout = { layout.value = it },
        modifier = modifier.pointerInput(annotated) {
            detectTapGestures { pos ->
                layout.value?.let { l ->
                    val offset = l.getOffsetForPosition(pos)
                    annotated.getStringAnnotations("file", offset, offset).firstOrNull()?.let { a ->
                        val parts = a.item.split(':')
                        onFileTap(parts.first(), parts.getOrNull(1)?.toIntOrNull())
                    }
                    annotated.getStringAnnotations("url", offset, offset).firstOrNull()?.let { a ->
                        runCatching { uriHandler.openUri(a.item) }
                    }
                }
            }
        }
    )
}

/** Rendu complet des blocs markdown d'un message (MA31). */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    baseColor: Color = Zcolors.text,
    fontSize: TextUnit = 16.sp,
    lineHeight: TextUnit = 26.sp,
    onFileTap: (String, Int?) -> Unit = { _, _ -> }
) {
    val c = Zcolors
    val blocks = remember(text) { parseMarkdownBlocks(text) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> AnnotatedText(
                    block.spans,
                    baseColor = baseColor,
                    fontSize = when (block.level) {
                        1 -> fontSize * 1.35f
                        2 -> fontSize * 1.18f
                        else -> fontSize * 1.06f
                    },
                    lineHeight = lineHeight * 1.2f,
                    bold = true,
                    onFileTap = onFileTap
                )
                is MdBlock.Paragraph -> AnnotatedText(
                    block.spans,
                    baseColor = baseColor,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    onFileTap = onFileTap
                )
                is MdBlock.ListBlock -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    block.items.forEach { item ->
                        Row {
                            Text(
                                item.marker,
                                style = TextStyle(fontFamily = Inter, fontSize = fontSize, lineHeight = lineHeight),
                                color = c.text3,
                                modifier = Modifier.width(18.dp + 14.dp * item.level)
                            )
                            AnnotatedText(
                                item.spans,
                                baseColor = baseColor,
                                fontSize = fontSize,
                                lineHeight = lineHeight,
                                onFileTap = onFileTap
                            )
                        }
                    }
                }
                is MdBlock.Quote -> Row {
                    Box(
                        Modifier
                            .width(3.dp)
                            .height(IntrinsicSize.Min)
                            .fillMaxHeight()
                            .background(c.violet.copy(alpha = 0.55f), RoundedCornerShape(2.dp))
                    )
                    Spacer(Modifier.width(10.dp))
                    AnnotatedText(
                        block.spans,
                        baseColor = c.text2,
                        fontSize = fontSize * 0.94f,
                        lineHeight = lineHeight,
                        italic = true,
                        onFileTap = onFileTap
                    )
                }
                is MdBlock.Table -> Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(c.surface2)
                        .border(1.dp, c.border, MaterialTheme.shapes.small)
                ) {
                    Row(Modifier.fillMaxWidth().background(c.surface3)) {
                        block.header.forEach { h ->
                            Text(
                                h,
                                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, lineHeight = 18.sp),
                                color = c.text,
                                modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                    }
                    block.rows.forEach { row ->
                        HorizontalDivider(color = c.border, thickness = 0.5.dp)
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { cell ->
                                Text(
                                    cell,
                                    style = TextStyle(fontFamily = Inter, fontSize = 12.5.sp, lineHeight = 18.sp),
                                    color = c.text2,
                                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            if (row.size < block.header.size) {
                                repeat(block.header.size - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                MdBlock.Divider -> HorizontalDivider(color = c.border, thickness = 0.5.dp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Coloration syntaxique — thème clair et sombre (MA32)
// ─────────────────────────────────────────────────────────────────────────────

private val TOKENS = Regex(
    "(?<!:)//[^\\n]*" +                       // 1 commentaire ligne
        "|\"(?:[^\"\\\\\\n]|\\\\.)*\"" +       // 2 chaîne "
        "|'(?:[^'\\\\\\n]|\\\\.)*'" +          // 3 chaîne '
        "|@\\w+" +                              // 4 annotation
        "|\\b\\d+(?:\\.\\d+)?[fFlL]?\\b" +      // 5 nombre
        "|\\b(?:val|var|fun|class|object|interface|if|else|when|for|while|return|import|package|" +
        "true|false|null|implementation|api|compileOnly|runtimeOnly|testImplementation|" +
        "plugins|dependencies|buildscript|repositories|id|alias|data|sealed|enum|annotation|" +
        "override|suspend|private|public|internal|protected|companion|lateinit|by|is|as|in|" +
        "throw|try|catch|finally|this|super|const|vararg|typealias|init|constructor|open|" +
        "abstract|let|apply|also|run|with|map|filter|forEach|listOf|mapOf|mutableListOf)\\b"
)

/** Palette du bloc de code selon le mode clair / sombre. */
data class CodeTheme(
    val plain: Color,
    val comment: Color,
    val string: Color,
    val keyword: Color,
    val number: Color,
    val annotation: Color,
    val gutter: Color
)

@Composable
fun codeTheme(): CodeTheme {
    val c = Zcolors
    return if (c.isDark) CodeTheme(
        plain = c.codePlain,
        comment = Color(0xFF5C6A85),
        string = Color(0xFF7EE8A2),
        keyword = Color(0xFFC792EA),
        number = Color(0xFFFBBF24),
        annotation = Color(0xFF67E8F9),
        gutter = Color(0xFF43506B)
    ) else CodeTheme(
        plain = Color(0xFF3B3A36),
        comment = Color(0xFF9A938A),
        string = Color(0xFF2E7D4F),
        keyword = Color(0xFF7C4DBC),
        number = Color(0xFFB26A00),
        annotation = Color(0xFF0E7490),
        gutter = Color(0xFFB8AFA3)
    )
}

fun highlightCode(code: String, theme: CodeTheme): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (m in TOKENS.findAll(code)) {
        append(code.substring(last, m.range.first))
        val style = when {
            m.value.startsWith("//") -> SpanStyle(color = theme.comment, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            m.value.startsWith("\"") || m.value.startsWith("'") -> SpanStyle(color = theme.string)
            m.value.startsWith("@") -> SpanStyle(color = theme.annotation)
            m.value.firstOrNull()?.isDigit() == true -> SpanStyle(color = theme.number)
            else -> SpanStyle(color = theme.keyword)
        }
        withStyle(style) { append(m.value) }
        last = m.range.last + 1
    }
    append(code.substring(last))
}

// ─────────────────────────────────────────────────────────────────────────────
// Bloc de code : en-tête fin, copie, appliquer, repli > 15 lignes — MA32
// ─────────────────────────────────────────────────────────────────────────────

private const val CODE_COLLAPSE_LINES = 15

@Composable
fun CodeBlock(
    path: String?,
    language: String?,
    code: String,
    modifier: Modifier = Modifier,
    maxHeight: Int = 260,
    showLineNumbers: Boolean = true,
    onApply: ((String) -> Unit)? = null
) {
    val c = Zcolors
    val theme = codeTheme()
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }
    var expanded by rememberSaveable(path, code.take(24), code.length) { mutableStateOf(code.count { it == '\n' } + 1 <= CODE_COLLAPSE_LINES) }

    val lineCount = code.count { it == '\n' } + 1
    val shownCode = if (expanded || lineCount <= CODE_COLLAPSE_LINES) code
    else code.lines().take(CODE_COLLAPSE_LINES).joinToString("\n")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(c.codeBg)
            .border(1.dp, c.codeBorder, MaterialTheme.shapes.medium)
    ) {
        // En-tête fin : langue en mono à gauche, Copier / Appliquer à droite
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                (language ?: "code").lowercase(),
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.5.sp),
                color = theme.comment,
                modifier = Modifier.weight(1f)
            )
            if (onApply != null) {
                HeaderAction("Appliquer") { onApply(code) }
                Spacer(Modifier.width(8.dp))
            }
            HeaderAction(if (copied) "Copié ✓" else "Copier") {
                clipboard.setText(AnnotatedString(code))
                copied = true
                scope.launch { delay(1500); copied = false }
            }
        }
        HorizontalDivider(color = c.codeBorder, thickness = 0.5.dp)

        // Corps : numéros de ligne + code colorisé, défilement horizontal
        val annotated = remember(shownCode, theme) { highlightCode(shownCode, theme) }
        val shownLines = shownCode.count { it == '\n' } + 1
        Box(
            Modifier
                .heightIn(max = maxHeight.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                if (showLineNumbers) {
                    Column(horizontalAlignment = Alignment.End) {
                        for (i in 1..shownLines) {
                            Text(
                                "$i",
                                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp, lineHeight = 16.sp),
                                color = theme.gutter
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    annotated,
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, lineHeight = 16.sp),
                    color = theme.plain,
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                )
            }
        }

        if (lineCount > CODE_COLLAPSE_LINES) {
            HorizontalDivider(color = c.codeBorder, thickness = 0.5.dp)
            Text(
                if (expanded) "Replier" else "Afficher tout ($lineCount lignes)",
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.5.sp),
                color = c.cyan,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun HeaderAction(label: String, onClick: () -> Unit) {
    val c = Zcolors
    Text(
        label,
        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp),
        color = c.text2,
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Panneau de diff : unifié ou côte à côte, surlignage mot à mot — MA42
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DiffPanel(
    lines: List<DiffUtil.Line>,
    modifier: Modifier = Modifier,
    maxLines: Int = 14,
    allowToggle: Boolean = true
) {
    val c = Zcolors
    val theme = codeTheme()
    val clipboard = LocalClipboardManager.current
    var preferSideBySide by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val wide = maxWidth >= 600.dp
        val sideBySide = allowToggle && preferSideBySide && wide
        val shown = lines.take(maxLines)

        Column(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(c.codeBg)
                .border(1.dp, c.codeBorder, MaterialTheme.shapes.medium)
        ) {
            // Barre de commande : bascule + copie du patch
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (allowToggle && wide) {
                    DiffToggle("Unifié", selected = !sideBySide) { preferSideBySide = false }
                    Spacer(Modifier.width(4.dp))
                    DiffToggle("Côte à côte", selected = sideBySide) { preferSideBySide = true }
                }
                Spacer(Modifier.weight(1f))
                HeaderAction("Copier le patch") {
                    clipboard.setText(AnnotatedString(shown.joinToString("\n") { l ->
                        (when (l.kind) {
                            DiffUtil.Kind.ADD -> "+"
                            DiffUtil.Kind.DEL -> "-"
                            DiffUtil.Kind.CTX -> " "
                        }) + l.text
                    }))
                }
            }
            HorizontalDivider(color = c.codeBorder, thickness = 0.5.dp)

            if (sideBySide) {
                SideBySideDiff(shown, theme)
            } else {
                UnifiedDiff(shown, theme)
            }

            if (lines.size > maxLines) {
                Text(
                    "+ ${lines.size - maxLines} lignes…",
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp),
                    color = theme.comment,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun DiffToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = Zcolors
    Text(
        label,
        style = TextStyle(fontFamily = Inter, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, fontSize = 11.sp),
        color = if (selected) c.text else c.text3,
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(if (selected) c.surface3 else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

private val ADD_BG_ALPHA = 0.12f
private val DEL_BG_ALPHA = 0.12f

@Composable
private fun UnifiedDiff(lines: List<DiffUtil.Line>, theme: CodeTheme) {
    val c = Zcolors
    // Surlignage mot à mot : associe chaque paire DEL/ADD consécutive (MA42).
    val wordRanges = remember(lines) {
        val map = HashMap<Int, IntRange>()
        var i = 0
        while (i < lines.size - 1) {
            if (lines[i].kind == DiffUtil.Kind.DEL && lines[i + 1].kind == DiffUtil.Kind.ADD) {
                val (oldRange, newRange) = DiffUtil.wordSpans(lines[i].text, lines[i + 1].text)
                map[i] = oldRange
                map[i + 1] = newRange
                i += 2
            } else i++
        }
        map
    }
    Column {
        lines.forEachIndexed { index, line ->
            val (bg, textColor, sign) = when (line.kind) {
                DiffUtil.Kind.ADD -> Triple(c.green.copy(alpha = ADD_BG_ALPHA), c.green, "+")
                DiffUtil.Kind.DEL -> Triple(c.red.copy(alpha = DEL_BG_ALPHA), c.red, "-")
                DiffUtil.Kind.CTX -> Triple(Color.Transparent, theme.comment, " ")
            }
            val range = wordRanges[index]
            val annotated = remember(line.text, range, textColor) {
                buildAnnotatedString {
                    if (range != null && range != IntRange.EMPTY && line.text.isNotEmpty()) {
                        append(line.text.substring(0, range.first.coerceAtMost(line.text.length)))
                        withStyle(SpanStyle(background = textColor.copy(alpha = 0.22f), fontWeight = FontWeight.SemiBold)) {
                            append(line.text.substring(range.first.coerceIn(0, line.text.length), range.last + 1))
                        }
                        append(line.text.substring((range.last + 1).coerceAtMost(line.text.length)))
                    } else append(line.text)
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(bg)
                    .padding(horizontal = 10.dp, vertical = 1.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    "${line.newNum ?: line.oldNum ?: ""}",
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp, lineHeight = 16.sp),
                    color = theme.gutter,
                    modifier = Modifier.width(26.dp)
                )
                Text(
                    sign,
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, lineHeight = 16.sp),
                    color = textColor
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    annotated,
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, lineHeight = 16.sp),
                    color = textColor,
                    maxLines = 1
                )
            }
        }
    }
}

/** Vue côte à côte : anciennes lignes à gauche, nouvelles à droite (paires alignées). */
@Composable
private fun SideBySideDiff(lines: List<DiffUtil.Line>, theme: CodeTheme) {
    val c = Zcolors
    data class PairRow(val old: DiffUtil.Line?, val new: DiffUtil.Line?)
    val pairs = remember(lines) {
        val out = mutableListOf<PairRow>()
        var i = 0
        while (i < lines.size) {
            val l = lines[i]
            when (l.kind) {
                DiffUtil.Kind.CTX -> { out.add(PairRow(l, l)); i++ }
                DiffUtil.Kind.DEL -> {
                    if (i + 1 < lines.size && lines[i + 1].kind == DiffUtil.Kind.ADD) {
                        out.add(PairRow(l, lines[i + 1])); i += 2
                    } else { out.add(PairRow(l, null)); i++ }
                }
                DiffUtil.Kind.ADD -> { out.add(PairRow(null, l)); i++ }
            }
        }
        out
    }
    Column {
        pairs.forEach { pair ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                DiffSide(pair.old, theme, isOld = true, modifier = Modifier.weight(1f))
                Box(Modifier.width(1.dp).fillMaxHeight().background(c.codeBorder))
                DiffSide(pair.new, theme, isOld = false, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DiffSide(line: DiffUtil.Line?, theme: CodeTheme, isOld: Boolean, modifier: Modifier = Modifier) {
    val c = Zcolors
    val bg = when {
        line == null -> Color.Transparent
        line.kind == DiffUtil.Kind.ADD -> c.green.copy(alpha = ADD_BG_ALPHA)
        line.kind == DiffUtil.Kind.DEL -> c.red.copy(alpha = DEL_BG_ALPHA)
        else -> Color.Transparent
    }
    val fg = when {
        line == null -> theme.comment
        line.kind == DiffUtil.Kind.ADD -> c.green
        line.kind == DiffUtil.Kind.DEL -> c.red
        else -> theme.comment
    }
    Row(modifier.fillMaxWidth().background(bg), verticalAlignment = Alignment.Top) {
        Text(
            if (line == null) "" else "${(if (isOld) line.oldNum else line.newNum) ?: ""}",
            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp, lineHeight = 16.sp),
            color = theme.gutter,
            modifier = Modifier.width(24.dp).padding(horizontal = 4.dp)
        )
        Text(
            (line?.text ?: "").ifEmpty { " " },
            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.5.sp, lineHeight = 16.sp),
            color = fg,
            maxLines = 1,
            modifier = Modifier.padding(end = 6.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bloc « Réflexion » repliable avec shimmer — MA35
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ThinkingBlock(
    reasoning: String,
    running: Boolean,
    seconds: Int,
    modifier: Modifier = Modifier
) {
    val c = Zcolors
    if (reasoning.isBlank() && !running) return
    var open by remember { mutableStateOf(false) }
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "reflexion")
    val shimmer by transition.animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(900), androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "shimmer"
    )
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable { open = !open }
                .padding(horizontal = 4.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                when {
                    running -> "Réflexion…"
                    seconds >= 0 -> "Réfléchi pendant $seconds s"
                    else -> "Réflexion"
                },
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.5.sp),
                color = c.text3.copy(alpha = if (running) shimmer else 1f)
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (open) "Replier" else "Lire",
                tint = c.text3, modifier = Modifier.size(14.dp)
            )
        }
        if (open && reasoning.isNotBlank()) {
            Text(
                reasoning,
                style = TextStyle(
                    fontFamily = Inter,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                ),
                color = c.text3,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
            )
        }
    }
}
