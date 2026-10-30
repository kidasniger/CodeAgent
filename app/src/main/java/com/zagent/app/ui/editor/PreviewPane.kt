package com.zagent.app.ui.editor

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.MimeTypeMap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayInputStream

const val PREVIEW_HOST = "preview.local"

private fun mimeFor(path: String): String {
    val ext = path.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "html", "htm" -> "text/html"
        "js", "mjs" -> "application/javascript"
        "css" -> "text/css"
        "json" -> "application/json"
        "svg" -> "image/svg+xml"
        "txt", "md" -> "text/plain"
        "wasm" -> "application/wasm"
        else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }
}

private fun isTextMime(mime: String): Boolean =
    mime.startsWith("text/") || mime == "application/javascript" ||
        mime == "application/json" || mime == "image/svg+xml"

/** Sert les fichiers du projet sous https://preview.local/ : chemins relatifs et fetch fonctionnent. */
private class PreviewClient(private val readResource: (String) -> ByteArray?) : WebViewClient() {
    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        val url = request?.url ?: return null
        if (url.host != PREVIEW_HOST) return null
        var path = (url.path ?: "/").removePrefix("/")
        if (path.isEmpty() || path.endsWith("/")) path += "index.html"
        val bytes = if (path.contains("..")) null else readResource(path)
        val headers = mapOf("Access-Control-Allow-Origin" to "*", "Cache-Control" to "no-store")
        if (bytes == null) {
            return WebResourceResponse(
                "text/plain", "utf-8", 404, "Not Found", headers,
                ByteArrayInputStream("404 : $path".toByteArray())
            )
        }
        val mime = mimeFor(path)
        return WebResourceResponse(
            mime, if (isTextMime(mime)) "utf-8" else null, 200, "OK", headers,
            ByteArrayInputStream(bytes)
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PreviewPane(
    path: String,
    reloadKey: Int,
    readResource: (String) -> ByteArray?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            webViewClient = PreviewClient(readResource)
        }
    }
    DisposableEffect(webView) { onDispose { webView.destroy() } }
    AndroidView(
        factory = { webView },
        modifier = modifier,
        update = { wv ->
            val key = path + "#" + reloadKey
            if (wv.tag != key) {
                wv.tag = key
                wv.loadUrl("https://" + PREVIEW_HOST + "/" + Uri.encode(path, "/"))
            }
        }
    )
}
