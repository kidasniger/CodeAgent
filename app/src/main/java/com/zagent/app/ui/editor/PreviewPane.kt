package com.zagent.app.ui.editor

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import android.net.Uri
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.Zcolors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val BG_FIX_JS = """
(function(){try{
var b=document.body,h=document.documentElement;
if(b){
var cb=getComputedStyle(b),ch=getComputedStyle(h);
var noH=ch.backgroundImage==='none'&&(ch.backgroundColor==='rgba(0, 0, 0, 0)'||ch.backgroundColor==='transparent');
var hasB=cb.backgroundImage!=='none'||(cb.backgroundColor!=='rgba(0, 0, 0, 0)'&&cb.backgroundColor!=='transparent');
if(noH&&hasB){h.style.backgroundImage=cb.backgroundImage;h.style.backgroundColor=cb.backgroundColor;
h.style.backgroundRepeat='no-repeat';h.style.backgroundAttachment='fixed';h.style.backgroundSize='cover';}
}
var fix=function(rules){for(var i=0;i<rules.length;i++){var r=rules[i];
if(!r.style){if(r.cssRules)fix(r.cssRules);continue;}
['min-height','height'].forEach(function(p){var v=r.style.getPropertyValue(p);
if(/^\s*100(\.0+)?vh\s*${'$'}/.test(v)){r.style.setProperty(p,'100dvh',r.style.getPropertyPriority(p));}});}};
for(var s=0;s<document.styleSheets.length;s++){try{fix(document.styleSheets[s].cssRules);}catch(e){}}
}catch(e){}})();
"""

/** Niveau d'un message de console. */
enum class ConsoleLevel { LOG, WARN, ERROR }

class ConsoleEntry(val level: ConsoleLevel, val text: String, val source: String)

/** Messages console JS de l'aperçu (console.log, erreurs, échecs de chargement). */
class ConsoleState {
    val entries = mutableStateListOf<ConsoleEntry>()

    val errorCount: Int get() = entries.count { it.level == ConsoleLevel.ERROR }

    fun add(level: ConsoleLevel, text: String, source: String = "") {
        entries.add(ConsoleEntry(level, text, source))
        while (entries.size > 300) entries.removeAt(0)
    }

    fun clear() = entries.clear()

    /** Erreurs formatées pour les envoyer à l'agent. */
    fun errorsAsText(): String = entries
        .filter { it.level == ConsoleLevel.ERROR }
        .joinToString("\n") { e ->
            "- " + e.text + (if (e.source.isNotEmpty()) " (" + e.source + ")" else "")
        }
}

/**
 * Aperçu web : un vrai serveur HTTP local (PreviewServer) sert le projet,
 * le WebView charge http://127.0.0.1:<port>/<fichier>, comme dans Acode.
 * Les messages console et les erreurs JS sont envoyés dans [console].
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PreviewPane(
    path: String,
    reloadKey: Int,
    readResource: (String) -> ByteArray?,
    console: ConsoleState,
    onTitle: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val latestRead by rememberUpdatedState(readResource)
    val latestTitle by rememberUpdatedState(onTitle)
    val server = remember { PreviewServer { res -> latestRead(res) } }
    var port by remember { mutableStateOf<Int?>(null) }
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            // Rendu proche d'un vrai navigateur (comme Acode)
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            setBackgroundColor(Color.WHITE)
            onResume()
            resumeTimers()
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    console.clear()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    // Le fond de la page (body) n'est pas toujours peint par le WebView :
                    // on le copie sur <html> pour qu'il s'affiche comme dans un navigateur.
                    view?.evaluateJavascript(BG_FIX_JS, null)
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    val url = request?.url?.toString().orEmpty()
                    console.add(
                        ConsoleLevel.ERROR,
                        "Échec de chargement : " + url + " (" + (error?.description ?: "erreur") + ")"
                    )
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onReceivedTitle(view: WebView?, title: String?) {
                    latestTitle(title.orEmpty())
                }

                override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                    val level = when (m.messageLevel()) {
                        ConsoleMessage.MessageLevel.ERROR -> ConsoleLevel.ERROR
                        ConsoleMessage.MessageLevel.WARNING -> ConsoleLevel.WARN
                        else -> ConsoleLevel.LOG
                    }
                    val src = m.sourceId().orEmpty().substringAfterLast('/')
                    val where = if (src.isNotEmpty()) src + ":" + m.lineNumber() else ""
                    console.add(level, m.message().orEmpty(), where)
                    return true
                }
            }
        }
    }
    LaunchedEffect(server) {
        port = withContext(Dispatchers.IO) { runCatching { server.start() }.getOrDefault(-1) }
    }
    DisposableEffect(server, webView) {
        onDispose {
            server.stop()
            webView.destroy()
        }
    }
    val p = port
    server.fallbackDirs = ancestorDirs(path)
    AndroidView(
        factory = { webView },
        modifier = modifier,
        update = { wv ->
            if (p != null) {
                val key = path + "#" + reloadKey + "@" + p
                if (wv.tag != key) {
                    wv.tag = key
                    if (p > 0) {
                        wv.loadUrl("http://127.0.0.1:" + p + "/" + Uri.encode(path, "/"))
                    } else {
                        wv.loadDataWithBaseURL(
                            null,
                            "<html><body style=\"font-family:sans-serif;padding:16px\">Serveur d'aperçu indisponible.</body></html>",
                            "text/html", "utf-8", null
                        )
                    }
                }
            }
        }
    )
}

/** Panneau console affiché sous l'aperçu. */
@Composable
fun ConsolePanel(state: ConsoleState, modifier: Modifier = Modifier) {
    val c = Zcolors
    val scroll = rememberScrollState()
    LaunchedEffect(state.entries.size) { scroll.scrollTo(scroll.maxValue) }
    Column(
        modifier
            .fillMaxWidth()
            .background(c.bg2)
            .verticalScroll(scroll)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        if (state.entries.isEmpty()) {
            Text(
                "Console vide",
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp),
                color = c.text3
            )
        }
        state.entries.forEach { e ->
            val color = when (e.level) {
                ConsoleLevel.ERROR -> c.red
                ConsoleLevel.WARN -> c.amber
                ConsoleLevel.LOG -> c.text
            }
            val bg = when (e.level) {
                ConsoleLevel.ERROR -> c.redBg
                ConsoleLevel.WARN -> c.amberBg
                ConsoleLevel.LOG -> c.bg2
            }
            Row(Modifier.fillMaxWidth().background(bg).padding(vertical = 2.dp, horizontal = 4.dp)) {
                Text(
                    e.text,
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, lineHeight = 15.sp),
                    color = color,
                    modifier = Modifier.weight(1f)
                )
                if (e.source.isNotEmpty()) {
                    Text(
                        "  " + e.source,
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp),
                        color = c.text3
                    )
                }
            }
        }
    }
}

/** "public/blog/post.html" -> ["public/blog", "public"] (du plus proche au plus lointain). */
private fun ancestorDirs(path: String): List<String> {
    val out = ArrayList<String>()
    var dir = path.substringBeforeLast('/', "")
    while (dir.isNotEmpty()) {
        out.add(dir)
        dir = dir.substringBeforeLast('/', "")
    }
    return out
}
