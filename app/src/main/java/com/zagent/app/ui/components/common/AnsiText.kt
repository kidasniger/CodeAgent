package com.zagent.app.ui.components.common

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

/** feat-056 — référence « chemin avec extension: numéro de ligne » (sorties de compilateur/tests). */
private val FILE_LINE_RE = Regex("([A-Za-z0-9._/-]+\\.[A-Za-z0-9]{1,6}):(\\d{1,5})[^0-9]")

/** Garde, pour chaque ligne, le texte après le dernier retour chariot (barres de progression). */
private fun normalizeCr(s: String): String =
    s.replace("\r\n", "\n").split('\n').joinToString("\n") { it.substringAfterLast('\r') }

fun stripAnsi(s: String): String = s.replace(ANSI_SEQ, "")

/** Texte propre à renvoyer à l'IA : sans codes couleur ni barres de progression. */
fun cleanForModel(s: String): String = normalizeCr(stripAnsi(s))

/** Palette ANSI 16 couleurs pour un fond sombre (rendu habituel). */
private val ANSI16_DARK: List<Color?> = listOf(
    Color(0xFF6B7280), Color(0xFFDC2626), Color(0xFF16A34A), Color(0xFFCA8A04),
    Color(0xFF2563EB), Color(0xFF9333EA), Color(0xFF0891B2), null,
    Color(0xFF9CA3AF), Color(0xFFEF4444), Color(0xFF22C55E), Color(0xFFEAB308),
    Color(0xFF3B82F6), Color(0xFFA855F7), Color(0xFF06B6D4), null
)

/**
 * MA84 — Palette ANSI 16 couleurs pour un fond clair : les mêmes teintes en
 * version plus profonde pour rester lisibles sur fond clair.
 */
private val ANSI16_LIGHT: List<Color?> = listOf(
    Color(0xFF24292F), Color(0xFFB91C1C), Color(0xFF116B3E), Color(0xFF8A6100),
    Color(0xFF0757C8), Color(0xFF7C3AED), Color(0xFF0E7490), Color(0xFF51607A),
    Color(0xFF6E7B91), Color(0xFFCB2B2B), Color(0xFF188A4B), Color(0xFF9A6E00),
    Color(0xFF2563EB), Color(0xFF8B5CF6), Color(0xFF0891B2), null
)

private fun ansiPalette(dark: Boolean): List<Color?> = if (dark) ANSI16_DARK else ANSI16_LIGHT

private fun color256(n: Int, dark: Boolean): Color? = when {
    n in 0..15 -> ansiPalette(dark)[n]
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
fun ansiToAnnotated(raw: String, linkColor: Color, dark: Boolean = true): AnnotatedString {
    val palette = ansiPalette(dark)
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
                    p in 30..37 -> fg = palette[p - 30]
                    p in 90..97 -> fg = palette[p - 90 + 8]
                    p == 39 -> fg = null
                    p == 38 && params.getOrNull(i + 1) == 5 -> {
                        fg = params.getOrNull(i + 2)?.let { color256(it, dark) }
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
    // feat-056 — références « chemin:ligne » de compilateur/tests (ex: src/A.kt:42)
    // hors URLs : sous-lignées et annotées « URL » sous la forme file:chemin:ligne.
    val dejaLies = ArrayList<IntRange>()
    for (m in URL_RE.findAll(plain)) {
        dejaLies.add(m.range.first..(m.range.first + m.value.trimEnd('.', ',', ';', ':').length - 1))
    }
    for (m in FILE_LINE_RE.findAll(plain)) {
        val start = m.range.first
        val end = m.range.last + 1
        if (dejaLies.any { start <= it.last && end >= it.first }) continue
        builder.addStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline), start, end)
        builder.addStringAnnotation("URL", "file:" + m.groupValues[1] + ":" + m.groupValues[2], start, end)
    }
    return builder.toAnnotatedString()
}

/**
 * Sortie de commande : couleurs ANSI rendues (palette adaptée au thème), liens
 * cliquables. [dark] sélectionne la palette sombre (défaut, sur fond de code)
 * ou claire (MA84). [onLinkClick] intercepte les annotations « URL » — dont les
 * références chemin:ligne détectées (feat-056) — au lieu d'ouvrir le navigateur.
 */
@Composable
fun AnsiText(
    text: String,
    style: TextStyle,
    baseColor: Color,
    linkColor: Color,
    modifier: Modifier = Modifier,
    dark: Boolean = true,
    onLinkClick: ((String) -> Unit)? = null
) {
    val uri = LocalUriHandler.current
    val annotated = remember(text, linkColor, dark) { ansiToAnnotated(text, linkColor, dark) }
    ClickableText(
        text = annotated,
        style = style.copy(color = baseColor),
        modifier = modifier,
        onClick = { offset ->
            annotated.getStringAnnotations("URL", offset, offset).firstOrNull()?.let {
                if (onLinkClick != null) onLinkClick(it.item)
                else try { uri.openUri(it.item) } catch (_: Exception) { }
            }
        }
    )
}

/** Garde la fin d'un long texte pour l'affichage (évite de mettre en page des dizaines de milliers de caractères). */
fun tailForDisplay(text: String, max: Int): String {
    if (text.length <= max) return text
    val cut = text.length - max
    val nl = text.indexOf('\n', cut)
    val start = if (nl in 0 until text.length - 1) nl + 1 else cut
    return "[...]\n" + text.substring(start)
}
