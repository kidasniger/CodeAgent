package com.zagent.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.data.DiffUtil
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.Zcolors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Puces d'outils (list_dir · read_file · make_dir · write_file)
// ─────────────────────────────────────────────────────────────────────────────

enum class ToolState { RUNNING, OK, ERROR, REFUSED }

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
                .clip(RoundedCornerShape(14.dp))
                .background(c.surface)
                .border(1.dp, c.border, RoundedCornerShape(14.dp))
                .then(if (hasResult) Modifier.clickable { expanded = !expanded } else Modifier)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icône clé à molette sur carré violet translucide
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(c.violetBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Build, contentDescription = null,
                    tint = c.violet, modifier = Modifier.size(15.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                name,
                style = TextStyle(
                    fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                ),
                color = c.cyan
            )
            if (!arg.isNullOrBlank()) {
                Spacer(Modifier.width(6.dp))
                Text(
                    "·",
                    color = c.text3,
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    arg,
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, lineHeight = 14.sp),
                    color = c.text2,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
            } else {
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.width(8.dp))
            when (state) {
                ToolState.RUNNING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(13.dp), strokeWidth = 2.dp, color = c.cyan
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("en cours", color = c.cyan, style = TextStyle(fontFamily = Inter, fontSize = 12.sp))
                }
                ToolState.OK -> {
                    Icon(Icons.Default.Check, null, tint = c.green, modifier = Modifier.size(14.dp))
                    Text("ok", color = c.green, style = TextStyle(fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
                }
                ToolState.ERROR -> {
                    Icon(Icons.Default.Close, null, tint = c.red, modifier = Modifier.size(14.dp))
                    Text("erreur", color = c.red, style = TextStyle(fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
                }
                ToolState.REFUSED -> {
                    Text("refusé", color = c.amber, style = TextStyle(fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
                }
            }
            if (hasResult) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Détail",
                    tint = c.text3, modifier = Modifier.size(16.dp)
                )
            }
        }

        if (expanded && hasResult) {
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.surface2)
                    .border(1.dp, c.border, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                val preview = resultText!!.lines().take(8).joinToString("\n")
                Text(
                    preview,
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, lineHeight = 15.sp),
                    color = c.text2
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Texte markdown léger : `code` et **gras**
// ─────────────────────────────────────────────────────────────────────────────

private val INLINE_MD = Regex("(`[^`]+`)|(\\*\\*[^*]+\\*\\*)")

fun buildInlineMarkdown(
    text: String,
    baseColor: Color,
    codeColor: Color,
    codeBg: Color?,
    bold: Boolean = false
): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (m in INLINE_MD.findAll(text)) {
        append(text.substring(last, m.range.first))
        if (m.groupValues[1].isNotEmpty()) {
            // fragment de code
            val code = m.groupValues[1].trim('`')
            withStyle(
                SpanStyle(
                    fontFamily = JetBrainsMono,
                    fontSize = 0.85.sp,
                    color = codeColor,
                    background = codeBg ?: Color.Unspecified
                )
            ) { append(code) }
        } else {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = baseColor)) {
                append(m.groupValues[2].trim('*'))
            }
        }
        last = m.range.last + 1
    }
    append(text.substring(last))
}

// ─────────────────────────────────────────────────────────────────────────────
// Coloration syntaxique minimaliste (Kotlin / Gradle / JSON…)
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

// Palette du bloc de code (fond toujours sombre)
private val CODE_COMMENT = Color(0xFF5C6A85)
private val CODE_STRING = Color(0xFF7EE8A2)
private val CODE_KEYWORD = Color(0xFFC792EA)
private val CODE_NUMBER = Color(0xFFFBBF24)
private val CODE_ANNOTATION = Color(0xFF67E8F9)

fun highlightCode(code: String): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (m in TOKENS.findAll(code)) {
        append(code.substring(last, m.range.first))
        val style = when {
            m.value.startsWith("//") -> SpanStyle(color = CODE_COMMENT, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            m.value.startsWith("\"") || m.value.startsWith("'") -> SpanStyle(color = CODE_STRING)
            m.value.startsWith("@") -> SpanStyle(color = CODE_ANNOTATION)
            m.value.firstOrNull()?.isDigit() == true -> SpanStyle(color = CODE_NUMBER)
            else -> SpanStyle(color = CODE_KEYWORD)
        }
        withStyle(style) { append(m.value) }
        last = m.range.last + 1
    }
    append(code.substring(last))
}

// ─────────────────────────────────────────────────────────────────────────────
// Bloc de code avec en-tête, langage et copie
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CodeBlock(
    path: String?,
    language: String?,
    code: String,
    modifier: Modifier = Modifier,
    maxHeight: Int = 260
) {
    val c = Zcolors
    val clipboard = LocalClipboardManager.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.codeBg)
            .border(1.dp, c.codeBorder, RoundedCornerShape(16.dp))
    ) {
        // En-tête
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Description, contentDescription = null,
                tint = c.cyan, modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                path ?: "extrait",
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp),
                color = c.codePlain,
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = true)
            )
            if (language != null) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(c.violetBg)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        language,
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp),
                        color = c.violet
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x1A94A3BA))
                    .clickable {
                        clipboard.setText(AnnotatedString(code))
                        copied = true
                        scope.launch { delay(2000); copied = false }
                    }
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = "Copier",
                    tint = if (copied) CODE_STRING else Color(0xFFC9D4E8),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    if (copied) "Copié !" else "Copier",
                    style = TextStyle(fontFamily = Inter, fontSize = 11.sp),
                    color = Color(0xFFC9D4E8)
                )
            }
        }

        // Corps : numéros de ligne + code colorisé
        val annotated = remember(code) { highlightCode(code) }
        val lineCount = code.count { it == '\n' } + 1
        Box(
            Modifier
                .heightIn(max = maxHeight.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    for (i in 1..lineCount) {
                        Text(
                            "$i",
                            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp, lineHeight = 16.sp),
                            color = Color(0xFF43506B)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    annotated,
                    style = TextStyle(
                        fontFamily = JetBrainsMono, fontSize = 11.sp, lineHeight = 16.sp
                    ),
                    color = c.codePlain,
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Panneau de diff (bottom sheet de confirmation)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DiffPanel(
    lines: List<DiffUtil.Line>,
    modifier: Modifier = Modifier,
    maxLines: Int = 14
) {
    val c = Zcolors
    val shown = lines.take(maxLines)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.codeBg)
            .border(1.dp, c.codeBorder, RoundedCornerShape(14.dp))
            .verticalScroll(rememberScrollState())
    ) {
        shown.forEach { line ->
            val (bg, textColor, sign) = when (line.kind) {
                DiffUtil.Kind.ADD -> Triple(Color(0x1434D399), Color(0xFF7EE8A2), "+")
                DiffUtil.Kind.DEL -> Triple(Color(0x14F87171), Color(0xFFFCA5A5), "-")
                DiffUtil.Kind.CTX -> Triple(Color.Transparent, Color(0xFF8B98AD), " ")
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(bg)
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    "${line.newNum ?: line.oldNum ?: ""}",
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp, lineHeight = 16.sp),
                    color = Color(0xFF43506B),
                    modifier = Modifier.width(24.dp)
                )
                Text(
                    sign,
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, lineHeight = 16.sp),
                    color = textColor
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    line.text.ifBlank { " " },
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, lineHeight = 16.sp),
                    color = textColor,
                    maxLines = 1
                )
            }
        }
        if (lines.size > maxLines) {
            Text(
                "+ ${lines.size - maxLines} lignes…",
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp),
                color = Color(0xFF8B98AD),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}
