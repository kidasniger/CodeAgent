package com.zagent.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Terminal
import com.zagent.app.ui.editor.ConsolePanel
import com.zagent.app.ui.editor.PreviewPane
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.platform.LocalContext
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
import com.zagent.app.ui.components.GlassAlertDialog
import com.zagent.app.ui.components.GlassDropdownMenu
import com.zagent.app.ui.components.CircleIconButton
import com.zagent.app.ui.editor.EditorTab
import com.zagent.app.ui.editor.Kind
import com.zagent.app.ui.editor.Lang
import com.zagent.app.ui.editor.SyntaxHighlighter
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.theme.LocalGlass
import com.zagent.app.ui.theme.LocalHazeState
import com.zagent.app.ui.theme.glassSurface
import com.zagent.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Vue de code : coloration, éditeur de texte et barre de symboles (découpé de EditorScreen.kt)

private val MatchBg = Color(0x55FBBF24)
private val CurrentMatchBg = Color(0xAAFBBF24)

internal fun styleFor(kind: Kind): SpanStyle = when (kind) {
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

internal class CodeTransformation(
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

internal fun findAll(text: String, query: String, caseSensitive: Boolean): List<IntRange> {
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
internal fun CodeEditor(
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
        color = c.text
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
            .background(c.bg)
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
                style = lineStyle.copy(color = c.text3, textAlign = TextAlign.End),
                modifier = Modifier
                    .width(gutterWidth)
                    .heightIn(min = viewportHeight)
                    .background(c.bg2)
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

internal val SYMBOLS = listOf(
    "Tab" to "  ", "{" to "{", "}" to "}", "(" to "(", ")" to ")", "[" to "[", "]" to "]",
    ";" to ";", "<" to "<", ">" to ">", "\"" to "\"", "'" to "'", "=" to "=", "/" to "/"
)

@Composable
internal fun SymbolBar(onInsert: (String) -> Unit) {
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
                    .clip(MaterialTheme.shapes.small)
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
