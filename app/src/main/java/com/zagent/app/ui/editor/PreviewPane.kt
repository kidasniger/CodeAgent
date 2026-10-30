package com.zagent.app.ui.editor

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Aperçu web : un vrai serveur HTTP local (PreviewServer) sert le projet,
 * le WebView charge http://127.0.0.1:<port>/<fichier>, comme dans Acode.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PreviewPane(
    path: String,
    reloadKey: Int,
    readResource: (String) -> ByteArray?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val latestRead by rememberUpdatedState(readResource)
    val server = remember { PreviewServer { res -> latestRead(res) } }
    var port by remember { mutableStateOf<Int?>(null) }
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            webViewClient = WebViewClient()
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
