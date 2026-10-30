package com.zagent.app.ui.editor

import java.util.regex.Pattern

enum class Lang { HTML, CSS, JS, KOTLIN, PYTHON, JSON, MARKDOWN, PLAIN }

enum class Kind { COMMENT, STRING, NUMBER, KEYWORD, TYPE, FUNCTION, TAG, ATTR, PROP, LITERAL, HEADING, EMPH }

data class Span(val start: Int, val end: Int, val kind: Kind)

object SyntaxHighlighter {

    /** Au-delà, on désactive la coloration pour garder la saisie fluide. */
    const val MAX_CHARS = 80_000

    fun langFor(path: String): Lang = when (path.substringAfterLast('.', "").lowercase()) {
        "html", "htm", "xml", "svg" -> Lang.HTML
        "css", "scss" -> Lang.CSS
        "js", "jsx", "ts", "tsx", "mjs" -> Lang.JS
        "kt", "kts", "java", "gradle" -> Lang.KOTLIN
        "py" -> Lang.PYTHON
        "json" -> Lang.JSON
        "md", "markdown" -> Lang.MARKDOWN
        else -> Lang.PLAIN
    }

    fun label(lang: Lang): String = when (lang) {
        Lang.HTML -> "HTML"
        Lang.CSS -> "CSS"
        Lang.JS -> "JavaScript"
        Lang.KOTLIN -> "Kotlin"
        Lang.PYTHON -> "Python"
        Lang.JSON -> "JSON"
        Lang.MARKDOWN -> "Markdown"
        Lang.PLAIN -> "Texte"
    }

    private class Rule(val kind: Kind, val pattern: String)

    private class RuleSet(rules: List<Rule>) {
        val kinds: List<Kind> = rules.map { it.kind }
        val pattern: Pattern = Pattern.compile(
            rules.joinToString("|") { "(" + it.pattern + ")" },
            Pattern.MULTILINE
        )
    }

    private val TQ = "\"\"\""

    private val DQ_SQ = """"(?:\\.|[^"\\\n])*"?|'(?:\\.|[^'\\\n])*'?"""
    private const val ID = """[A-Za-z_\x24][\w\x24]*"""

    private val JS_RULES = RuleSet(
        listOf(
            Rule(Kind.COMMENT, """//[^\n]*|/\*[\s\S]*?(?:\*/|\z)"""),
            Rule(Kind.STRING, DQ_SQ + """|`(?:\\[\s\S]|[^`\\])*`?"""),
            Rule(Kind.NUMBER, """\b(?:0[xX][0-9a-fA-F]+|\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)\b"""),
            Rule(
                Kind.KEYWORD,
                """\b(?:const|let|var|function|return|if|else|for|while|do|switch|case|break|continue|new|class|extends|import|export|from|default|async|await|try|catch|finally|throw|typeof|instanceof|in|of|this|super|static|get|set|yield|delete|void|interface|type|enum|implements|public|private|protected|readonly)\b"""
            ),
            Rule(Kind.LITERAL, """\b(?:true|false|null|undefined|NaN|Infinity)\b"""),
            Rule(Kind.TYPE, """\b[A-Z][A-Za-z0-9_]*\b"""),
            Rule(Kind.FUNCTION, ID + """(?=\s*\()""")
        )
    )

    private val KOTLIN_RULES = RuleSet(
        listOf(
            Rule(Kind.COMMENT, """//[^\n]*|/\*[\s\S]*?(?:\*/|\z)"""),
            Rule(Kind.STRING, TQ + """[\s\S]*?(?:""" + TQ + """|\z)|""" + DQ_SQ),
            Rule(Kind.ATTR, """@[A-Za-z_]\w*"""),
            Rule(Kind.NUMBER, """\b0[xX][0-9a-fA-F_]+\b|\b\d[\d_]*(?:\.\d+)?(?:[fFLuU]|[eE][+-]?\d+)?\b"""),
            Rule(
                Kind.KEYWORD,
                """\b(?:package|import|class|interface|object|fun|val|var|if|else|when|for|while|do|return|break|continue|in|is|as|try|catch|finally|throw|override|open|abstract|sealed|data|enum|companion|private|public|protected|internal|suspend|inline|lateinit|by|init|constructor|this|super|const|vararg|static|final|void|new|extends|implements|instanceof|synchronized|throws|int|long|double|float|boolean|char|byte|short)\b"""
            ),
            Rule(Kind.LITERAL, """\b(?:true|false|null)\b"""),
            Rule(Kind.TYPE, """\b[A-Z][A-Za-z0-9_]*\b"""),
            Rule(Kind.FUNCTION, ID + """(?=\s*\()""")
        )
    )

    private val PY_RULES = RuleSet(
        listOf(
            Rule(Kind.COMMENT, """#[^\n]*"""),
            Rule(
                Kind.STRING,
                "\"\"\"" + """[\s\S]*?(?:""" + "\"\"\"" + """|\z)|'''[\s\S]*?(?:'''|\z)|""" + DQ_SQ
            ),
            Rule(Kind.ATTR, """@[A-Za-z_][\w.]*"""),
            Rule(Kind.NUMBER, """\b\d+(?:\.\d+)?(?:[eE][+-]?\d+)?\b"""),
            Rule(
                Kind.KEYWORD,
                """\b(?:def|class|return|if|elif|else|for|while|break|continue|import|from|as|try|except|finally|raise|with|lambda|yield|pass|global|nonlocal|in|is|not|and|or|assert|del|async|await|match|case|self)\b"""
            ),
            Rule(Kind.LITERAL, """\b(?:True|False|None)\b"""),
            Rule(Kind.TYPE, """\b[A-Z][A-Za-z0-9_]*\b"""),
            Rule(Kind.FUNCTION, """\b[A-Za-z_]\w*(?=\s*\()""")
        )
    )

    private val CSS_RULES = RuleSet(
        listOf(
            Rule(Kind.COMMENT, """/\*[\s\S]*?(?:\*/|\z)"""),
            Rule(Kind.STRING, DQ_SQ),
            Rule(Kind.KEYWORD, """@[A-Za-z-]+"""),
            Rule(Kind.NUMBER, """#[0-9a-fA-F]{3,8}\b|(?<![\w.-])\d*\.?\d+(?:%|[a-zA-Z]+)?"""),
            Rule(Kind.LITERAL, """!important"""),
            Rule(Kind.FUNCTION, """[A-Za-z-]+(?=\()"""),
            Rule(Kind.PROP, """[A-Za-z-]+(?=\s*:\s*[^\s{])"""),
            Rule(Kind.TYPE, """[#.][A-Za-z_-][\w-]*""")
        )
    )

    private val JSON_RULES = RuleSet(
        listOf(
            Rule(Kind.PROP, """"(?:\\.|[^"\\\n])*"(?=\s*:)"""),
            Rule(Kind.STRING, """"(?:\\.|[^"\\\n])*"?"""),
            Rule(Kind.NUMBER, """-?\b\d+(?:\.\d+)?(?:[eE][+-]?\d+)?\b"""),
            Rule(Kind.LITERAL, """\b(?:true|false|null)\b""")
        )
    )

    private val MD_RULES = RuleSet(
        listOf(
            Rule(Kind.STRING, """```[\s\S]*?(?:```|\z)"""),
            Rule(Kind.HEADING, """^#{1,6}[ \t].*$"""),
            Rule(Kind.STRING, """`[^`\n]+`"""),
            Rule(Kind.EMPH, """\*\*[^*\n]+\*\*|__[^_\n]+__"""),
            Rule(Kind.FUNCTION, """!?\[[^\]\n]*\]\([^)\n]*\)"""),
            Rule(Kind.KEYWORD, """^[ \t]*(?:[-*+]|\d+\.)(?=[ \t])"""),
            Rule(Kind.COMMENT, """^>.*$""")
        )
    )

    // HTML : balises entières, puis sous-analyse des attributs.
    private val HTML_RULES = RuleSet(
        listOf(
            Rule(Kind.COMMENT, """<!--[\s\S]*?(?:-->|\z)"""),
            Rule(Kind.KEYWORD, """<![A-Za-z][^>]*>"""),
            Rule(Kind.TAG, """</?[A-Za-z][\w:.-]*(?:\s[^<>]*)?/?>""")
        )
    )

    private val TAG_INNER = RuleSet(
        listOf(
            Rule(Kind.TAG, """</?[A-Za-z][\w:.-]*"""),
            Rule(Kind.STRING, """"[^"]*"|'[^']*'"""),
            Rule(Kind.ATTR, """[\w:@.-]+(?==)"""),
            Rule(Kind.TAG, """/?>""")
        )
    )

    private val HTML_EMBED = Pattern.compile("""(<(script|style)\b[^>]*>)(.*?)(</\2\s*>|\z)""", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)

    private fun scan(text: CharSequence, set: RuleSet, from: Int, to: Int, out: MutableList<Span>, html: Boolean = false) {
        if (from >= to) return
        val m = set.pattern.matcher(text)
        m.region(from, to)
        while (m.find()) {
            if (m.end() <= m.start()) continue
            for (i in 1..set.kinds.size) {
                if (m.start(i) >= 0) {
                    val kind = set.kinds[i - 1]
                    if (html && kind == Kind.TAG) {
                        scan(text, TAG_INNER, m.start(i), m.end(i), out)
                    } else {
                        out.add(Span(m.start(i), m.end(i), kind))
                    }
                    break
                }
            }
        }
    }

    private fun scanHtml(text: String, out: MutableList<Span>) {
        var pos = 0
        val m = HTML_EMBED.matcher(text)
        while (m.find()) {
            val bodyStart = m.start(3)
            val bodyEnd = m.end(3)
            scan(text, HTML_RULES, pos, bodyStart, out, html = true)
            val isScript = m.group(2).equals("script", ignoreCase = true)
            scan(text, if (isScript) JS_RULES else CSS_RULES, bodyStart, bodyEnd, out)
            pos = bodyEnd
        }
        scan(text, HTML_RULES, pos, text.length, out, html = true)
    }

    // Cache du dernier calcul : la recherche ou la sélection ne relancent pas l'analyse.
    private var cacheText: String? = null
    private var cacheLang: Lang? = null
    private var cacheSpans: List<Span> = emptyList()

    @Synchronized
    fun spans(text: String, lang: Lang): List<Span> {
        if (lang == Lang.PLAIN || text.length > MAX_CHARS) return emptyList()
        if (cacheLang == lang && cacheText == text) return cacheSpans
        val out = ArrayList<Span>()
        try {
            when (lang) {
                Lang.HTML -> scanHtml(text, out)
                Lang.JS -> scan(text, JS_RULES, 0, text.length, out)
                Lang.KOTLIN -> scan(text, KOTLIN_RULES, 0, text.length, out)
                Lang.PYTHON -> scan(text, PY_RULES, 0, text.length, out)
                Lang.CSS -> scan(text, CSS_RULES, 0, text.length, out)
                Lang.JSON -> scan(text, JSON_RULES, 0, text.length, out)
                Lang.MARKDOWN -> scan(text, MD_RULES, 0, text.length, out)
                Lang.PLAIN -> Unit
            }
        } catch (e: Exception) {
            out.clear()
        } catch (e: StackOverflowError) {
            out.clear()
        }
        cacheText = text
        cacheLang = lang
        cacheSpans = out
        return out
    }
}
