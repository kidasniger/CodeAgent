package com.zagent.app.ui.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

private val ANSI_SEQ = Regex("\u001B\\[[0-9;?]*[ -/]*[@-~]|\u001B\\][^\u0007\u001B]*(?:\u0007|\u001B\\\\)")
private val URL_RE = Regex("https?://[^\\s)>\\]\"'<]+")

/** Garde, pour chaque ligne, le texte après le dernier retour chariot (barres de progression). */
private fun normalizeCr(s: String): String =
    s.replace("\r\n", "\n").split('\n').joinToString("\n") { it.substringAfterLast('\r') }

fun stripAnsi(s: String): String = s.replace(ANSI_SEQ, "")

/** Texte propre à renvoyer à l'IA : sans codes couleur ni barres de progression. */
fun cleanForModel(s: String): String = normalizeCr(stripAnsi(s))

private val ANSI16: List<Color?> = listOf(
    Color(0xFF6B7280), Color(0xFFDC2626), Color(0xFF16A34A), Color(0xFFCA8A04),
    Color(0xFF2563EB), Color(0xFF9333EA), Color(0xFF0891B2), null,
    Color(0xFF9CA3AF), Color(0xFFEF4444), Color(0xFF22C55E), Color(0xFFEAB308),
    Color(0xFF3B82F6), Color(0xFFA855F7), Color(0xFF06B6D4), null
)

private fun color256(n: Int): Color? = when {
    n in 0..15 -> ANSI16[n]
    n in 16..231 -> {
        val v = n - 16
        fun lvl(x: Int) = if (x == 0) 0 else 55 + 40 * x
        Color(lvl(v / 36), lvl((v / 6) % 6), lvl(v % 6))
    }
    n in 232..255 -> {
        val g = 8 + 10 * (n - 232)
        Color(g, g, g)
    }
    else -> null
}

/** Convertit un texte avec codes ANSI (couleurs, gras) en texte stylé, avec les liens http(s) cliquables. */
fun ansiToAnnotated(raw: String, linkColor: Color): AnnotatedString {
    val text = normalizeCr(raw)
    val plain = StringBuilder()
    val spans = ArrayList<Triple<Int, Int, SpanStyle>>()
    var fg: Color? = null
    var bold = false

    fun appendPlain(t: String) {
        if (t.isEmpty()) return
        val start = plain.length
        plain.append(t)
        if (fg != null || bold) {
            spans.add(
                Triple(
                    start, plain.length,
                    SpanStyle(color = fg ?: Color.Unspecified, fontWeight = if (bold) FontWeight.Bold else null)
                )
            )
        }
    }

    var last = 0
    for (m in ANSI_SEQ.findAll(text)) {
        appendPlain(text.substring(last, m.range.first))
        val seq = m.value
        if (seq.startsWith("\u001B[") && seq.endsWith("m")) {
            val params = seq.substring(2, seq.length - 1).split(';').map { it.toIntOrNull() ?: 0 }
            var i = 0
            while (i < params.size) {
                val p = params[i]
                when {
                    p == 0 -> { fg = null; bold = false }
                    p == 1 -> bold = true
                    p == 22 -> bold = false
                    p in 30..37 -> fg = ANSI16[p - 30]
                    p in 90..97 -> fg = ANSI16[p - 90 + 8]
                    p == 39 -> fg = null
                    p == 38 && params.getOrNull(i + 1) == 5 -> {
                        fg = params.getOrNull(i + 2)?.let { color256(it) }
                        i += 2
                    }
                    p == 38 && params.getOrNull(i + 1) == 2 -> {
                        val r = params.getOrNull(i + 2) ?: 0
                        val g = params.getOrNull(i + 3) ?: 0
                        val b = params.getOrNull(i + 4) ?: 0
                        fg = Color(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
                        i += 4
                    }
                }
                i++
            }
        }
        last = m.range.last + 1
    }
    appendPlain(text.substring(last))

    val builder = AnnotatedString.Builder(plain.toString())
    spans.forEach { (s, e, st) -> builder.addStyle(st, s, e) }
    for (m in URL_RE.findAll(plain)) {
        val url = m.value.trimEnd('.', ',', ';', ':')
        val start = m.range.first
        val end = start + url.length
        builder.addStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline), start, end)
        builder.addStringAnnotation("URL", url, start, end)
    }
    return builder.toAnnotatedString()
}

/** Sortie de commande : couleurs ANSI rendues, liens cliquables. */
@Composable
fun AnsiText(
    text: String,
    style: TextStyle,
    baseColor: Color,
    linkColor: Color,
    modifier: Modifier = Modifier
) {
    val uri = LocalUriHandler.current
    val annotated = remember(text, linkColor) { ansiToAnnotated(text, linkColor) }
    ClickableText(
        text = annotated,
        style = style.copy(color = baseColor),
        modifier = modifier,
        onClick = { offset ->
            annotated.getStringAnnotations("URL", offset, offset).firstOrNull()?.let {
                try { uri.openUri(it.item) } catch (_: Exception) { }
            }
        }
    )
}
