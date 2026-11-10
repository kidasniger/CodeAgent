package com.zagent.app.ui.editor

import android.webkit.MimeTypeMap
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private val EMPTY = ByteArray(0)

private fun mimeFor(path: String): String {
    val ext = path.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "html", "htm" -> "text/html"
        "js", "mjs" -> "application/javascript"
        "css" -> "text/css"
        "json", "map" -> "application/json"
        "webmanifest" -> "application/manifest+json"
        "svg" -> "image/svg+xml"
        "xml" -> "application/xml"
        "txt", "md" -> "text/plain"
        "wasm" -> "application/wasm"
        "woff" -> "font/woff"
        "woff2" -> "font/woff2"
        "ttf" -> "font/ttf"
        "otf" -> "font/otf"
        "ico" -> "image/x-icon"
        "avif" -> "image/avif"
        else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }
}

private fun isTextMime(mime: String): Boolean =
    mime.startsWith("text/") || mime == "application/javascript" || mime == "application/json" ||
        mime == "application/xml" || mime == "image/svg+xml" || mime == "application/manifest+json"

/**
 * Mini serveur HTTP local (127.0.0.1 uniquement) pour l'aperçu web, comme Acode.
 * Sert les fichiers du projet : chemins relatifs et absolus, fetch, Range (audio/vidéo),
 * CORS, index.html par défaut, redirection des dossiers.
 * Ports essayés dans l'ordre : 8080, 8081, 8082, puis un port libre.
 */
class PreviewServer(private val readResource: (String) -> ByteArray?) {

    private var server: ServerSocket? = null
    private var pool: ExecutorService? = null

    /**
     * corr-035 — Jeton aléatoire exigé sur chaque requête : même si le serveur
     * n'écoute que sur 127.0.0.1, une autre appli de l'appareil pourrait s'y
     * connecter. Le WebView reçoit le jeton via cookie (posé par PreviewPane)
     * ou paramètre « ?t= » ; toute autre connexion reçoit 403.
     */
    val token: String = java.security.SecureRandom().let { rnd ->
        val chars = "0123456789abcdef"
        buildString { repeat(24) { append(chars[rnd.nextInt(chars.length)]) } }
    }

    /**
     * Dossiers (du plus proche au plus lointain) où chercher une ressource introuvable à la racine.
     * Permet aux chemins absolus ("/css/style.css") de fonctionner quand la page est dans un
     * sous-dossier comme "public/" (cas Firebase Hosting), comme dans Acode.
     */
    @Volatile
    var fallbackDirs: List<String> = emptyList()

    /** MA88 : true tant que le serveur local écoute (voyant vert/rouge de l'aperçu). */
    val isRunning: Boolean
        get() = server?.isClosed == false

    private fun lookup(path: String): ByteArray? {
        readResource(path)?.let { return it }
        for (dir in fallbackDirs) {
            readResource("$dir/$path")?.let { return it }
        }
        return null
    }

    /** Démarre le serveur et renvoie le port. À appeler hors du thread principal. */
    @Synchronized
    fun start(): Int {
        server?.let { if (!it.isClosed) return it.localPort }
        val loopback = InetAddress.getByName("127.0.0.1")
        var bound: ServerSocket? = null
        for (port in intArrayOf(8080, 8081, 8082, 0)) {
            val candidate = ServerSocket()
            try {
                candidate.reuseAddress = true
                candidate.bind(InetSocketAddress(loopback, port))
                bound = candidate
                break
            } catch (e: Exception) {
                try { candidate.close() } catch (ignored: Exception) { }
            }
        }
        val socket = bound ?: throw IOException("Serveur d'aperçu : aucun port disponible")
        val executor = Executors.newCachedThreadPool()
        server = socket
        pool = executor
        val acceptor = Thread {
            while (!socket.isClosed) {
                try {
                    val client = socket.accept()
                    try {
                        executor.execute { handle(client) }
                    } catch (e: Exception) {
                        try { client.close() } catch (ignored: Exception) { }
                    }
                } catch (e: Exception) {
                    if (socket.isClosed) break
                    try { Thread.sleep(50) } catch (ie: InterruptedException) { break }
                }
            }
        }
        acceptor.name = "preview-server"
        acceptor.isDaemon = true
        acceptor.start()
        return socket.localPort
    }

    @Synchronized
    fun stop() {
        try { server?.close() } catch (ignored: Exception) { }
        server = null
        pool?.shutdownNow()
        pool = null
    }

    private fun readHeaderLine(input: InputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val b = input.read()
            if (b == -1) return if (sb.isEmpty()) null else sb.toString()
            if (b == '\n'.code) break
            if (b != '\r'.code) sb.append(b.toChar())
            if (sb.length > 8192) return null
        }
        return sb.toString()
    }

    private fun handle(socket: Socket) {
        socket.use { s ->
            try {
                s.soTimeout = 15_000
                val input = BufferedInputStream(s.getInputStream())
                val out = s.getOutputStream()
                val requestLine = readHeaderLine(input) ?: return
                val parts = requestLine.split(' ')
                if (parts.size < 2) return
                var range: String? = null
                var cookie: String? = null
                while (true) {
                    val line = readHeaderLine(input) ?: break
                    if (line.isEmpty()) break
                    val i = line.indexOf(':')
                    if (i > 0) {
                        val name = line.substring(0, i).trim()
                        val value = line.substring(i + 1).trim()
                        if (name.equals("range", ignoreCase = true)) range = value
                        else if (name.equals("cookie", ignoreCase = true)) cookie = value
                    }
                }
                respond(out, parts[0].uppercase(), parts[1], range, cookie)
            } catch (e: Exception) {
                // client déconnecté ou requête invalide : on ignore
            }
        }
    }

    private fun send(
        out: OutputStream,
        status: Int,
        reason: String,
        contentType: String?,
        body: ByteArray,
        offset: Int,
        length: Int,
        sendBody: Boolean,
        extra: List<Pair<String, String>> = emptyList()
    ) {
        val sb = StringBuilder()
        sb.append("HTTP/1.1 ").append(status).append(' ').append(reason).append("\r\n")
        if (contentType != null) sb.append("Content-Type: ").append(contentType).append("\r\n")
        sb.append("Content-Length: ").append(length).append("\r\n")
        sb.append("Accept-Ranges: bytes\r\n")
        sb.append("Cache-Control: no-store\r\n")
        // corr-037 : durcissement des réponses de l'aperçu.
        sb.append("X-Content-Type-Options: nosniff\r\n")
        sb.append("Referrer-Policy: no-referrer\r\n")
        if (contentType?.startsWith("text/html") == true) {
            // CSP modérée : casse ni les scripts inline ni les fetch externes
            // légitimes, mais bloque objets plugin, framing et base-uri.
            sb.append("Content-Security-Policy: object-src 'none'; frame-ancestors 'none'; base-uri 'none'\r\n")
        }
        sb.append("Access-Control-Allow-Origin: *\r\n")
        sb.append("Access-Control-Allow-Methods: GET, HEAD, OPTIONS\r\n")
        sb.append("Access-Control-Allow-Headers: *\r\n")
        for ((k, v) in extra) sb.append(k).append(": ").append(v).append("\r\n")
        sb.append("Connection: close\r\n\r\n")
        out.write(sb.toString().toByteArray(Charsets.ISO_8859_1))
        if (sendBody && length > 0) out.write(body, offset, length)
        out.flush()
    }

    private fun sendText(out: OutputStream, status: Int, reason: String, text: String, head: Boolean) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        send(out, status, reason, "text/plain; charset=utf-8", bytes, 0, bytes.size, !head)
    }

    private fun respond(out: OutputStream, method: String, target: String, rangeHeader: String?, cookie: String?) {
        val head = method == "HEAD"
        if (method == "OPTIONS") {
            send(out, 200, "OK", null, EMPTY, 0, 0, false)
            return
        }
        if (method != "GET" && !head) {
            sendText(out, 405, "Method Not Allowed", "405 : methode non supportee", head)
            return
        }

        // corr-035 : jeton requis — cookie « preview=… » (posé par PreviewPane)
        // ou paramètre « ?t= » sur la requête initiale.
        val query = target.substringAfter('?', "")
        val queryToken = Regex("(?:^|[&?])t=([0-9a-f]{24})").find(query)?.groupValues?.get(1)
        val cookieToken = cookie
            ?.split(';')
            ?.map { it.trim() }
            ?.firstOrNull { it.startsWith("preview=") }
            ?.substringAfter('=')
        val jetonOk = queryToken == token || cookieToken == token
        if (!jetonOk) {
            sendText(out, 403, "Forbidden", "403 : jeton d'aperçu manquant", head)
            return
        }

        val rawPath = target.substringBefore('#').substringBefore('?')
        val decoded = try {
            URLDecoder.decode(rawPath.replace("+", "%2B"), "UTF-8")
        } catch (e: Exception) {
            rawPath
        }
        val path = decoded.trimStart('/')
        if (path.contains("..") || path.contains('\\') || path.contains('\u0000')) {
            sendText(out, 403, "Forbidden", "403", head)
            return
        }

        var served = path
        val bytes: ByteArray?
        if (path.isEmpty() || path.endsWith("/")) {
            served = path + "index.html"
            bytes = lookup(served)
        } else {
            bytes = lookup(path)
            if (bytes == null && !path.substringAfterLast('/').contains('.') &&
                lookup("$path/index.html") != null
            ) {
                // Dossier sans "/" final : on redirige pour que les chemins relatifs fonctionnent.
                val location = (if (rawPath.startsWith("/")) rawPath else "/$rawPath") + "/"
                send(out, 301, "Moved Permanently", null, EMPTY, 0, 0, false, listOf("Location" to location))
                return
            }
        }
        if (bytes == null) {
            sendText(out, 404, "Not Found", "404 : $served", head)
            return
        }

        val mime = mimeFor(served)
        val ctype = if (isTextMime(mime)) "$mime; charset=utf-8" else mime
        val total = bytes.size
        var start = 0
        var end = total - 1
        var partial = false

        if (rangeHeader != null && rangeHeader.startsWith("bytes=", ignoreCase = true) && total > 0) {
            val spec = rangeHeader.substring(6).substringBefore(',').trim()
            val dash = spec.indexOf('-')
            if (dash >= 0) {
                val a = spec.substring(0, dash).trim()
                val b = spec.substring(dash + 1).trim()
                var rs = -1
                var re = -1
                if (a.isEmpty()) {
                    val n = b.toIntOrNull()
                    if (n != null && n > 0) {
                        rs = maxOf(0, total - n)
                        re = total - 1
                    }
                } else {
                    val sv = a.toIntOrNull()
                    if (sv != null) {
                        rs = sv
                        re = if (b.isEmpty()) total - 1 else (b.toIntOrNull() ?: -1)
                        if (re >= 0) re = minOf(re, total - 1)
                    }
                }
                if (rs >= 0 && re >= 0) {
                    if (rs > re || rs >= total) {
                        send(
                            out, 416, "Range Not Satisfiable", ctype, EMPTY, 0, 0, false,
                            listOf("Content-Range" to "bytes */$total")
                        )
                        return
                    }
                    start = rs
                    end = re
                    partial = true
                }
            }
        }

        // corr-035 : le cookie garantit que les sous-ressources (img, css, fetch)
        // passent la vérification sans paramètre de requête.
        val setCookie = listOf("Set-Cookie" to "preview=$token; Path=/; HttpOnly")
        if (partial) {
            send(
                out, 206, "Partial Content", ctype, bytes, start, end - start + 1, !head,
                listOf("Content-Range" to "bytes $start-$end/$total") + setCookie
            )
        } else {
            send(out, 200, "OK", ctype, bytes, 0, total, !head, setCookie)
        }
    }
}
