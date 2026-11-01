package com.zagent.app.terminal

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.system.Os
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.terminal.TextStyle
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.LinkOption
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * Gère le bootstrap (téléchargement + extraction) et la session shell du terminal embarqué.
 * Vit dans le MainViewModel : la session survit à la fermeture de l'écran Terminal.
 */
class TerminalController(
    private val app: Application,
    private val scope: CoroutineScope
) {
    sealed interface State {
        data object Idle : State
        data class Working(val label: String, val progress: Float?) : State
        data object Ready : State
        data class Failed(val message: String) : State
    }

    companion object {
        private const val RELEASES_API =
            "https://api.github.com/repos/termux/termux-packages/releases?per_page=30"

        // Même longueur (10 caractères) : permet de remplacer l'un par l'autre dans les binaires.
        private const val OLD_PKG = "com.termux"
        private const val NEW_PKG = "com.zagent"
    }

    // Le préfixe est /data/data/com.zagent/files/usr (bootstrap officiel adapté à l'extraction).
    private val baseDir = File("/data/data/${app.packageName}/files")
    private val prefixDir = File(baseDir, "usr")
    private val homeDir = File(baseDir, "home")
    private val stagingDir = File(baseDir, "usr-staging")

    var state by mutableStateOf<State>(if (isInstalled()) State.Ready else State.Idle)
        private set

    /** Un onglet shell : sa session et son état (terminée ou non). */
    class ShellTab(val session: TerminalSession, val number: Int) {
        var ended by mutableStateOf(false)
    }

    val tabs = mutableStateListOf<ShellTab>()
    var activeTab by mutableIntStateOf(0)
    private var tabCounter = 0

    val session: TerminalSession? get() = tabs.getOrNull(activeTab)?.session
    val sessionEnded: Boolean get() = tabs.getOrNull(activeTab)?.ended ?: false

    private val prefs = app.getSharedPreferences("terminal_prefs", Context.MODE_PRIVATE)

    /** Taille du texte du terminal (réglable par A- / A+). */
    var fontSize by mutableIntStateOf(prefs.getInt("font_size", 14))
        private set

    fun changeFontSize(delta: Int) {
        fontSize = (fontSize + delta).coerceIn(8, 28)
        prefs.edit().putInt("font_size", fontSize).apply()
    }
    var ctrlActive by mutableStateOf(false)

    /** Journal en lecture seule des commandes lancées par l'agent (onglet « Agent » du terminal). */
    var agentLog by mutableStateOf("")
        private set
    private var agentBase = ""

    /** Ajoute une ligne au journal de l'onglet Agent (actions de l'agent autres que les commandes). */
    @Synchronized
    fun logAgent(line: String) {
        agentBase = (agentBase + line + "\n").takeLast(60000)
        agentLog = agentBase
    }

    fun clearAgentLog() {
        agentBase = ""
        agentLog = ""
    }

    /** Dossier du projet actif : la prochaine session shell démarre dedans. */
    var projectDir: String? = null


    var view: TerminalView? = null

    @Volatile private var currentProc: Process? = null
    @Volatile private var userStopped = false

    /** Arrête la commande de l'agent en cours (bouton « Arrêter » du chat). */
    fun stopCurrentCommand() {
        userStopped = true
        currentProc?.destroyForcibly()
    }

    private fun isInstalled(): Boolean = File(prefixDir, "bin/bash").exists()

    // ── Installation ────────────────────────────────────────────────────────

    fun install() {
        if (state is State.Working) return
        val arch = when (Build.SUPPORTED_ABIS.firstOrNull()) {
            "arm64-v8a" -> "aarch64"
            else -> {
                state = State.Failed("Architecture non prise en charge : ${Build.SUPPORTED_ABIS.firstOrNull()}")
                return
            }
        }
        if (app.packageName != NEW_PKG) {
            state = State.Failed("L'identifiant de l'appli doit être $NEW_PKG (actuellement ${app.packageName})")
            return
        }
        state = State.Working("Recherche du système", null)
        scope.launch(Dispatchers.IO) {
            try {
                val url = resolveBootstrapUrl(arch)
                val zip = File(baseDir, "bootstrap-$arch.zip")
                download(url, zip)
                setState(State.Working("Extraction", null))
                extract(zip)
                zip.delete()
                setState(State.Working("Configuration", null))
                installDebPatcher()
                runSecondStage()
                ensureMirror()
                if (!isInstalled()) error("bin/bash introuvable après l'extraction")
                setState(State.Ready)
            } catch (e: Exception) {
                stagingDir.deleteRecursively()
                setState(State.Failed(e.message ?: e.javaClass.simpleName))
            }
        }
    }

    /** Trouve le dernier bootstrap officiel Termux (variante Android 7+) pour l'architecture donnée. */
    private fun resolveBootstrapUrl(arch: String): String {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
        val req = Request.Builder().url(RELEASES_API)
            .header("Accept", "application/vnd.github+json")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("Impossible de lister les versions du système (HTTP ${resp.code})")
            val arr = org.json.JSONArray(resp.body?.string() ?: error("Réponse vide"))
            for (i in 0 until arr.length()) {
                val rel = arr.getJSONObject(i)
                val tag = rel.optString("tag_name")
                if (!tag.startsWith("bootstrap-") || !tag.contains("android-7")) continue
                val assets = rel.optJSONArray("assets") ?: continue
                for (j in 0 until assets.length()) {
                    val a = assets.getJSONObject(j)
                    if (a.optString("name") == "bootstrap-$arch.zip") return a.getString("browser_download_url")
                }
            }
        }
        error("Aucun bootstrap Termux trouvé")
    }

    private suspend fun setState(s: State) = withContext(Dispatchers.Main) { state = s }

    private suspend fun download(url: String, dest: File) {
        baseDir.mkdirs()
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
        client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            if (!resp.isSuccessful) error("Téléchargement impossible (HTTP ${resp.code})")
            val body = resp.body ?: error("Réponse vide")
            val total = body.contentLength()
            var done = 0L
            var lastPct = -1
            body.byteStream().use { input ->
                FileOutputStream(dest).use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        if (total > 0) {
                            val pct = (done * 100 / total).toInt()
                            if (pct != lastPct) {
                                lastPct = pct
                                setState(State.Working("Téléchargement du système", done.toFloat() / total))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun extract(zipFile: File) {
        stagingDir.deleteRecursively()
        stagingDir.mkdirs()
        val symlinks = ArrayList<Pair<String, String>>()
        val stagingCanonical = stagingDir.canonicalPath + File.separator

        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                val name = entry.name
                if (name == "SYMLINKS.txt") {
                    zis.readBytes().toString(Charsets.UTF_8).replace(OLD_PKG, NEW_PKG).lineSequence().forEach { line ->
                        val parts = line.split("←")
                        if (parts.size == 2) symlinks.add(parts[0] to parts[1])
                    }
                    continue
                }
                val out = File(stagingDir, name)
                if (!out.canonicalPath.startsWith(stagingCanonical)) error("Entrée d'archive invalide : $name")
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    FileOutputStream(out).use { zis.copyTo(it) }
                    val executable = name.startsWith("bin/") || name.startsWith("libexec") ||
                        name.startsWith("lib/apt/apt-helper") || name.startsWith("lib/apt/methods") ||
                        name.contains("second-stage") && name.endsWith(".sh")
                    if (executable) Os.chmod(out.absolutePath, 0x1C0) // 0700
                }
            }
        }

        for ((target, link) in symlinks) {
            val linkFile = File(stagingDir, link)
            linkFile.parentFile?.mkdirs()
            Os.symlink(target, linkFile.absolutePath)
        }

        patchPrefix(stagingDir)

        prefixDir.deleteRecursively()
        if (!stagingDir.renameTo(prefixDir)) error("Impossible de finaliser l'installation")
        File(prefixDir, "tmp").mkdirs()
        homeDir.mkdirs()
    }

    /** Remplace com.termux par com.zagent dans tous les fichiers (binaires compris, même longueur). */
    private fun patchPrefix(root: File) {
        val old = OLD_PKG.toByteArray(Charsets.US_ASCII)
        val new = NEW_PKG.toByteArray(Charsets.US_ASCII)
        Files.walk(root.toPath()).use { stream ->
            stream.forEach { p ->
                if (!Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)) return@forEach
                val f = p.toFile()
                val len = f.length()
                if (len < old.size || len > 64L * 1024 * 1024) return@forEach
                val bytes = f.readBytes()
                if (replaceBytes(bytes, old, new)) {
                    f.setWritable(true, true)
                    f.writeBytes(bytes)
                }
            }
        }
    }

    private fun replaceBytes(data: ByteArray, old: ByteArray, new: ByteArray): Boolean {
        var changed = false
        var i = 0
        val last = data.size - old.size
        while (i <= last) {
            if (data[i] == old[0]) {
                var k = 1
                while (k < old.size && data[i + k] == old[k]) k++
                if (k == old.size) {
                    System.arraycopy(new, 0, data, i, new.size)
                    changed = true
                    i += old.size
                    continue
                }
            }
            i++
        }
        return changed
    }

    /** Installe le crochet apt qui adapte chaque paquet officiel Termux avant son installation. */
    private fun installDebPatcher() {
        val script = app.assets.open("zagent-patch-debs.sh").bufferedReader().use { it.readText() }
            .replace("@PREFIX@", prefixDir.absolutePath)
        val bin = File(prefixDir, "bin/zagent-patch-debs")
        bin.writeText(script)
        Os.chmod(bin.absolutePath, 0x1C0) // 0700
        val confDir = File(prefixDir, "etc/apt/apt.conf.d")
        confDir.mkdirs()
        File(confDir, "99-zagent").writeText("DPkg::Pre-Install-Pkgs { \"${bin.absolutePath}\"; };\n")
    }

    /**
     * Fixe le miroir apt par défaut (packages-cf.termux.dev) au lieu d'un miroir tiré au hasard.
     * Appliqué une seule fois (marqueur etc/termux/.zagent-mirror) : un changement ultérieur
     * via termux-change-repo est respecté. Sert aussi aux installations déjà existantes.
     */
    private fun ensureMirror() {
        try {
            val termuxDir = File(prefixDir, "etc/termux")
            val marker = File(termuxDir, ".zagent-mirror")
            if (marker.exists()) return
            val mirrors = File(termuxDir, "mirrors")
            mirrors.mkdirs()
            val default = File(mirrors, "default")
            if (!default.exists()) {
                default.writeText(
                    "# Default mirror\n" +
                        "WEIGHT=1\n" +
                        "MAIN=\"https://packages-cf.termux.dev/apt/termux-main\"\n" +
                        "ROOT=\"https://packages-cf.termux.dev/apt/termux-root\"\n" +
                        "X11=\"https://packages-cf.termux.dev/apt/termux-x11\"\n"
                )
            }
            val chosen = File(termuxDir, "chosen_mirrors")
            chosen.delete() // supprime le lien (ou le fichier) existant, sans suivre le lien
            chosen.deleteRecursively()
            Os.symlink("mirrors/default", chosen.absolutePath)
            File(prefixDir, "etc/apt/sources.list").writeText(
                "# The main termux repository:\n" +
                    "deb https://packages-cf.termux.dev/apt/termux-main stable main\n"
            )
            marker.writeText("1\n")
        } catch (e: Exception) {
            // Non bloquant : pkg choisira un miroir lui-même.
        }
    }

    private fun runSecondStage() {
        val script = File(prefixDir, "etc/termux/termux-bootstrap/second-stage/termux-bootstrap-second-stage.sh")
        if (!script.exists()) return
        val pb = ProcessBuilder(File(prefixDir, "bin/bash").absolutePath, script.absolutePath)
        pb.directory(homeDir)
        pb.redirectErrorStream(true)
        pb.environment().clear()
        buildEnv().forEach { e ->
            val i = e.indexOf('=')
            pb.environment()[e.substring(0, i)] = e.substring(i + 1)
        }
        val p = pb.start()
        p.inputStream.readBytes()
        if (!p.waitFor(120, TimeUnit.SECONDS)) {
            p.destroyForcibly()
            error("La configuration du système a dépassé 120 s")
        }
    }

    // ── Commandes de l'agent ────────────────────────────────────────────────

    /**
     * Exécute [command] avec bash dans [cwd] (dossier du projet), sans entrée clavier.
     * [onOutput] reçoit la sortie accumulée (au plus toutes les 150 ms) pour l'affichage en direct.
     * Retourne "Code de sortie: N" (ou "Erreur: ...") suivi de la sortie.
     */
    suspend fun runCommand(
        command: String,
        cwd: String?,
        timeoutSec: Int,
        silent: Boolean = false,
        outLimit: Int = 20000,
        onOutput: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        if (!isInstalled()) {
            return@withContext "Erreur: le terminal n'est pas installé. Ouvre l'écran Terminal pour l'installer."
        }
        File(prefixDir, "tmp").mkdirs()
        val dir = cwd?.let { File(it) }?.takeIf { it.isDirectory } ?: homeDir
        val pb = ProcessBuilder(File(prefixDir, "bin/bash").absolutePath, "-c", command)
        pb.directory(dir)
        pb.redirectErrorStream(true)
        pb.redirectInput(ProcessBuilder.Redirect.from(File("/dev/null")))
        pb.environment().clear()
        buildEnv().forEach { e ->
            val i = e.indexOf('=')
            pb.environment()[e.substring(0, i)] = e.substring(i + 1)
        }
        if (!silent) {
            agentBase = (agentLog + "\n$ " + command + "\n").takeLast(60000)
            agentLog = agentBase
        }
        val emit: (String) -> Unit = { out ->
            if (!silent) agentLog = agentBase + out
            onOutput(out)
        }
        val proc = try {
            pb.start()
        } catch (e: Exception) {
            if (!silent) {
                agentBase += "[impossible de lancer la commande]\n"
                agentLog = agentBase
            }
            return@withContext "Erreur: impossible de lancer la commande (${e.message})"
        }

        currentProc = proc
        userStopped = false
        val ctx = currentCoroutineContext()
        var timedOut = false
        var cancelled = false
        val started = System.currentTimeMillis()
        val watchdog = Thread {
            try {
                while (proc.isAlive) {
                    if (!ctx.isActive) {
                        cancelled = true
                        proc.destroyForcibly()
                        break
                    }
                    if (System.currentTimeMillis() - started > timeoutSec * 1000L) {
                        timedOut = true
                        proc.destroyForcibly()
                        break
                    }
                    Thread.sleep(250)
                }
            } catch (e: InterruptedException) {
                // fin normale
            }
        }
        watchdog.isDaemon = true
        watchdog.start()

        val sb = StringBuilder()
        var lastEmit = 0L
        try {
            proc.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val buf = CharArray(4096)
                while (true) {
                    val n = reader.read(buf)
                    if (n < 0) break
                    sb.append(buf, 0, n)
                    if (sb.length > 60000) sb.delete(0, sb.length - 40000)
                    val now = System.currentTimeMillis()
                    if (now - lastEmit > 250) {
                        lastEmit = now
                        emit(sb.toString())
                    }
                }
            }
        } catch (e: Exception) {
            // flux fermé par l'arrêt du processus
        }
        val code = try {
            proc.waitFor()
        } catch (e: InterruptedException) {
            -1
        }
        watchdog.interrupt()
        currentProc = null
        emit(sb.toString())

        val out = com.zagent.app.ui.components.cleanForModel(sb.toString()).let {
            if (it.length > outLimit) "[début de la sortie tronqué]\n" + it.takeLast(outLimit) else it
        }
        val secs = "%.1f".format(java.util.Locale.US, (System.currentTimeMillis() - started) / 1000.0)
        val head = when {
            timedOut -> "Erreur: délai de $timeoutSec s dépassé, commande arrêtée ($secs s)."
            cancelled -> "Erreur: commande interrompue ($secs s)."
            userStopped -> "Erreur: commande arrêtée par l'utilisateur ($secs s)."
            code == 0 -> "Code de sortie: 0 ($secs s)"
            else -> "Erreur: code de sortie $code ($secs s)"
        }
        if (!silent) {
            agentBase = agentBase + sb.toString() + "[" + head.lineSequence().first() + "]\n"
            agentLog = agentBase
        }
        "$head\n$out"
    }

    // ── Serveur de développement (arrière-plan) ─────────────────────────────

    /** Adresse locale détectée dans la sortie du serveur (ex : http://localhost:5173/). */
    var devUrl by mutableStateOf<String?>(null)
        private set
    var devCommand by mutableStateOf<String?>(null)
        private set
    var devRunning by mutableStateOf(false)
        private set
    var devLog by mutableStateOf("")
        private set

    /** Prévenu quand le serveur démarre (true) ou s'arrête (false). */
    var onDevServerChanged: ((Boolean) -> Unit)? = null

    @Volatile private var devProc: Process? = null

    private val devUrlRegex = Regex("""https?://(?:localhost|127\.0\.0\.1|0\.0\.0\.0|\[::1?\]):\d+[^\s'"<>)]*""")
    private val devPortRegex = Regex("""(?i)listening\s+(?:on|at)\s+(?:port\s+)?(\d{4,5})""")

    private fun detectDevUrl(text: String): String? {
        devUrlRegex.find(text)?.let { m ->
            return m.value.trimEnd('.', ',', ';')
                .replace("0.0.0.0", "localhost")
                .replace("[::1]", "localhost")
                .replace("[::]", "localhost")
        }
        devPortRegex.find(text)?.let { m -> return "http://localhost:" + m.groupValues[1] + "/" }
        return null
    }

    private fun pidOf(p: Process): Int? = try {
        val f = p.javaClass.getDeclaredField("pid")
        f.isAccessible = true
        f.getInt(p)
    } catch (e: Throwable) {
        null
    }

    /** Tue le processus et ses descendants (npm lance node en sous-processus). */
    private fun killTree(p: Process) {
        val root = pidOf(p)
        if (root != null) {
            try {
                val children = HashMap<Int, MutableList<Int>>()
                File("/proc").listFiles()?.forEach { d ->
                    val pid = d.name.toIntOrNull() ?: return@forEach
                    try {
                        val stat = File(d, "stat").readText()
                        val rest = stat.substring(stat.lastIndexOf(')') + 2).split(' ')
                        val ppid = rest.getOrNull(1)?.toIntOrNull() ?: return@forEach
                        children.getOrPut(ppid) { ArrayList() }.add(pid)
                    } catch (e: Exception) {
                        // processus disparu ou illisible
                    }
                }
                val all = ArrayList<Int>()
                val stack = ArrayList<Int>()
                stack.add(root)
                while (stack.isNotEmpty()) {
                    val x = stack.removeAt(stack.size - 1)
                    all.add(x)
                    children[x]?.let { stack.addAll(it) }
                }
                for (pid in all.reversed()) {
                    try { android.os.Process.killProcess(pid) } catch (e: Exception) { }
                }
            } catch (e: Exception) {
                // on retombe sur destroyForcibly
            }
        }
        p.destroyForcibly()
    }

    /**
     * Lance [command] en arrière-plan (sans délai) et attend jusqu'à [waitSec] secondes
     * que l'adresse locale apparaisse dans la sortie.
     */
    suspend fun startDevServer(command: String, cwd: String?, waitSec: Int = 45): String {
        if (!isInstalled()) {
            return "Erreur: le terminal n'est pas installé. Ouvre l'écran Terminal pour l'installer."
        }
        if (devProc?.isAlive == true) {
            return "Erreur: un serveur de développement tourne déjà (" + (devCommand ?: "") + ")" +
                (devUrl?.let { " sur $it" } ?: "") + ". Appelle stop_dev_server avant d'en lancer un autre."
        }
        val proc: Process = withContext(Dispatchers.IO) {
            File(prefixDir, "tmp").mkdirs()
            val dir = cwd?.let { File(it) }?.takeIf { it.isDirectory } ?: homeDir
            val pb = ProcessBuilder(File(prefixDir, "bin/bash").absolutePath, "-c", command)
            pb.directory(dir)
            pb.redirectErrorStream(true)
            pb.redirectInput(ProcessBuilder.Redirect.from(File("/dev/null")))
            pb.environment().clear()
            buildEnv().forEach { e ->
                val i = e.indexOf('=')
                pb.environment()[e.substring(0, i)] = e.substring(i + 1)
            }
            try { pb.start() } catch (e: Exception) { null }
        } ?: return "Erreur: impossible de lancer la commande."

        devProc = proc
        devUrl = null
        devCommand = command
        devLog = ""
        devRunning = true
        onDevServerChanged?.invoke(true)

        val sb = StringBuilder()
        val reader = Thread {
            try {
                proc.inputStream.bufferedReader(Charsets.UTF_8).use { r ->
                    val buf = CharArray(2048)
                    while (true) {
                        val n = r.read(buf)
                        if (n < 0) break
                        sb.append(buf, 0, n)
                        if (sb.length > 40000) sb.delete(0, sb.length - 30000)
                        val clean = com.zagent.app.ui.components.cleanForModel(sb.toString())
                        devLog = clean
                        if (devUrl == null) {
                            val found = detectDevUrl(clean)
                            if (found != null) devUrl = found
                        }
                    }
                }
            } catch (e: Exception) {
                // flux fermé par l'arrêt du processus
            }
            val code = try { proc.waitFor() } catch (e: InterruptedException) { -1 }
            if (devProc === proc) {
                devProc = null
                devRunning = false
                devUrl = null
                devLog = devLog + "\n[serveur arrêté, code " + code + "]"
                onDevServerChanged?.invoke(false)
            }
        }
        reader.isDaemon = true
        reader.start()

        val started = System.currentTimeMillis()
        while (System.currentTimeMillis() - started < waitSec * 1000L) {
            if (devUrl != null || !devRunning) break
            delay(400)
        }
        val tail = devLog.takeLast(1500).trim()
        val url = devUrl
        return when {
            url != null -> "Serveur de développement démarré en arrière-plan. Adresse locale : " + url +
                "\nL'utilisateur ouvre lui-même l'aperçu (bouton dans le chat) : ne l'ouvre pas. Pour l'arrêter : stop_dev_server.\nDernières lignes :\n" + tail
            !devRunning -> "Erreur: le serveur s'est arrêté juste après le lancement.\n" + tail
            else -> "Serveur lancé en arrière-plan, mais aucune adresse locale détectée après " + waitSec + " s. Dernières lignes :\n" + tail
        }
    }

    /** Arrête le serveur de développement. Retourne false s'il n'y en avait pas. */
    fun stopDevServer(): Boolean {
        val p = devProc ?: return false
        devProc = null
        killTree(p)
        devRunning = false
        devUrl = null
        devLog = devLog + "\n[serveur arrêté]"
        onDevServerChanged?.invoke(false)
        return true
    }

    // ── Session shell ───────────────────────────────────────────────────────

    private fun buildEnv(): Array<String> {
        val env = arrayListOf(
            "HOME=${homeDir.absolutePath}",
            "PREFIX=${prefixDir.absolutePath}",
            "TMPDIR=${File(prefixDir, "tmp").absolutePath}",
            "PATH=${File(prefixDir, "bin").absolutePath}",
            "LANG=en_US.UTF-8",
            "TERM=xterm-256color",
            "COLORTERM=truecolor",
            "ANDROID_DATA=/data",
            "ANDROID_ROOT=/system",
            "TERMUX_VERSION=0.118.0",
            "TERMUX_APP__PACKAGE_NAME=${app.packageName}",
            "TERMUX__PREFIX=${prefixDir.absolutePath}",
            "TERMUX__HOME=${homeDir.absolutePath}"
        )
        val termuxExec = listOf("lib/libtermux-exec-ld-preload.so", "lib/libtermux-exec.so")
            .map { File(prefixDir, it) }.firstOrNull { it.exists() }
        if (termuxExec != null) env.add("LD_PRELOAD=${termuxExec.absolutePath}")
        return env.toTypedArray()
    }

    /** Ouvre le premier onglet, ou relance l'onglet actif. */
    fun openSession() {
        if (!isInstalled()) return
        if (tabs.isEmpty()) {
            addTab()
            return
        }
        val i = activeTab.coerceIn(0, tabs.lastIndex)
        val old = tabs[i]
        val s = createSession() ?: return
        old.session.finishIfRunning()
        tabs[i] = ShellTab(s, old.number)
    }

    /** Nouvel onglet shell dans le dossier du projet actif. */
    fun addTab() {
        if (!isInstalled()) return
        val s = createSession() ?: return
        tabs.add(ShellTab(s, ++tabCounter))
        activeTab = tabs.lastIndex
    }

    fun closeTab(index: Int) {
        val t = tabs.getOrNull(index) ?: return
        t.session.finishIfRunning()
        val old = activeTab
        tabs.removeAt(index)
        activeTab = when {
            tabs.isEmpty() -> 0
            index < old -> old - 1
            else -> old.coerceAtMost(tabs.lastIndex)
        }
        if (tabs.isEmpty()) tabCounter = 0
    }

    private fun createSession(): TerminalSession? {
        ensureMirror()
        // Réinstalle le crochet apt à chaque ouverture : prend en compte un script mis à jour.
        try { installDebPatcher() } catch (e: Exception) { }
        homeDir.mkdirs()
        File(prefixDir, "tmp").mkdirs()
        val startDir = projectDir?.let { File(it) }?.takeIf { it.isDirectory } ?: homeDir
        return TerminalSession(
            File(prefixDir, "bin/bash").absolutePath,
            startDir.absolutePath,
            arrayOf("-bash"),
            buildEnv(),
            2000,
            client
        )
    }

    private fun shellQuote(p: String) = "'" + p.replace("'", "'\\''") + "'"

    /** Envoie « cd » vers le projet actif dans l'onglet affiché (bouton « Ouvrir dans le terminal »). */
    fun cdToProject() {
        val d = projectDir ?: return
        val t = tabs.getOrNull(activeTab) ?: return
        if (t.ended) return
        t.session.write("\u0015cd " + shellQuote(d) + "\n")
    }

    fun paste() {
        val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(app)?.toString()
        if (!text.isNullOrEmpty()) session?.emulator?.paste(text)
    }

    /** Copie tout l'historique de l'onglet affiché (les 50 000 derniers caractères). */
    fun copyAll() {
        val text = session?.emulator?.screen?.transcriptText?.trimEnd() ?: return
        val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("terminal", text.takeLast(50000)))
        android.widget.Toast.makeText(app, "Terminal copié", android.widget.Toast.LENGTH_SHORT).show()
    }

    private var colorBg = 0xFF0B0F17.toInt()
    private var colorFg = 0xFFE6EAF2.toInt()

    /** Aligne les couleurs du terminal sur le thème clair/sombre de l'appli. */
    fun applyColors(bg: Int, fg: Int) {
        colorBg = bg
        colorFg = fg
        applyToEmulator()
    }

    private fun applyToEmulator() {
        tabs.forEach { t ->
            val e = t.session.emulator ?: return@forEach
            val cols = e.mColors.mCurrentColors
            cols[TextStyle.COLOR_INDEX_FOREGROUND] = colorFg
            cols[TextStyle.COLOR_INDEX_BACKGROUND] = colorBg
            cols[TextStyle.COLOR_INDEX_CURSOR] = colorFg
        }
        view?.onScreenUpdated()
    }

    fun send(text: String) {
        session?.write(text)
    }

    fun showKeyboard() {
        val v = view ?: return
        v.requestFocus()
        (v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .showSoftInput(v, 0)
    }

    // ── Clients (session + vue) ─────────────────────────────────────────────

    private val client = object : TerminalSessionClient, TerminalViewClient {
        override fun onTextChanged(changedSession: TerminalSession?) { view?.onScreenUpdated() }
        override fun onTitleChanged(changedSession: TerminalSession?) {}
        override fun onSessionFinished(finishedSession: TerminalSession?) {
            tabs.firstOrNull { it.session === finishedSession }?.ended = true
        }
        override fun onCopyTextToClipboard(session: TerminalSession?, text: String?) {
            val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("terminal", text ?: ""))
        }
        override fun onPasteTextFromClipboard(session: TerminalSession?) {
            val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(app)?.toString()
            if (!text.isNullOrEmpty()) session?.emulator?.paste(text)
        }
        override fun onBell(session: TerminalSession?) {}
        override fun onColorsChanged(session: TerminalSession?) { view?.onScreenUpdated() }
        override fun onTerminalCursorStateChange(state: Boolean) {}
        override fun getTerminalCursorStyle(): Int? = 0

        override fun onScale(scale: Float): Float = 1.0f
        override fun onSingleTapUp(e: MotionEvent?) { showKeyboard() }
        override fun shouldBackButtonBeMappedToEscape(): Boolean = false
        override fun shouldEnforceCharBasedInput(): Boolean = true
        override fun shouldUseCtrlSpaceWorkaround(): Boolean = false
        override fun isTerminalViewSelected(): Boolean = true
        override fun copyModeChanged(copyMode: Boolean) {}
        override fun onKeyDown(keyCode: Int, e: KeyEvent?, session: TerminalSession?): Boolean = false
        override fun onKeyUp(keyCode: Int, e: KeyEvent?): Boolean = false
        override fun onLongPress(event: MotionEvent?): Boolean = false
        override fun readControlKey(): Boolean = ctrlActive
        override fun readAltKey(): Boolean = false
        override fun readShiftKey(): Boolean = false
        override fun readFnKey(): Boolean = false
        override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession?): Boolean {
            if (ctrlActive) ctrlActive = false
            return false
        }
        override fun onEmulatorSet() { applyToEmulator() }

        override fun logError(tag: String?, message: String?) {}
        override fun logWarn(tag: String?, message: String?) {}
        override fun logInfo(tag: String?, message: String?) {}
        override fun logDebug(tag: String?, message: String?) {}
        override fun logVerbose(tag: String?, message: String?) {}
        override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {}
        override fun logStackTrace(tag: String?, e: Exception?) {}
    }

    fun terminalViewClient(): TerminalViewClient = client
}
