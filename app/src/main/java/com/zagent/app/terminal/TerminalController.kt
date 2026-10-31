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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
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
        private const val BOOTSTRAP_URL_BASE =
            "https://github.com/kidasniger/CodeAgent/releases/download/bootstrap/"
    }

    // Les chemins doivent correspondre au préfixe compilé dans le bootstrap.
    private val baseDir = File("/data/data/${app.packageName}/files")
    private val prefixDir = File(baseDir, "usr")
    private val homeDir = File(baseDir, "home")
    private val stagingDir = File(baseDir, "usr-staging")

    var state by mutableStateOf<State>(if (isInstalled()) State.Ready else State.Idle)
        private set
    var session by mutableStateOf<TerminalSession?>(null)
        private set
    var sessionEnded by mutableStateOf(false)
        private set
    var ctrlActive by mutableStateOf(false)

    var view: TerminalView? = null

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
        state = State.Working("Téléchargement du système", 0f)
        scope.launch(Dispatchers.IO) {
            try {
                val zip = File(baseDir, "bootstrap-$arch.zip")
                download(BOOTSTRAP_URL_BASE + "bootstrap-$arch.zip", zip)
                setState(State.Working("Extraction", null))
                extract(zip)
                zip.delete()
                setState(State.Working("Configuration", null))
                runSecondStage()
                if (!isInstalled()) error("bin/bash introuvable après l'extraction")
                setState(State.Ready)
            } catch (e: Exception) {
                stagingDir.deleteRecursively()
                setState(State.Failed(e.message ?: e.javaClass.simpleName))
            }
        }
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
                    zis.readBytes().toString(Charsets.UTF_8).lineSequence().forEach { line ->
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

        prefixDir.deleteRecursively()
        if (!stagingDir.renameTo(prefixDir)) error("Impossible de finaliser l'installation")
        File(prefixDir, "tmp").mkdirs()
        homeDir.mkdirs()
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
            "ANDROID_ROOT=/system"
        )
        val termuxExec = File(prefixDir, "lib/libtermux-exec.so")
        if (termuxExec.exists()) env.add("LD_PRELOAD=${termuxExec.absolutePath}")
        return env.toTypedArray()
    }

    fun startSession() {
        if (!isInstalled()) return
        homeDir.mkdirs()
        File(prefixDir, "tmp").mkdirs()
        sessionEnded = false
        val s = TerminalSession(
            File(prefixDir, "bin/bash").absolutePath,
            homeDir.absolutePath,
            arrayOf("-bash"),
            buildEnv(),
            2000,
            client
        )
        session = s
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
            if (finishedSession === session) sessionEnded = true
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
        override fun setTerminalShellPid(session: TerminalSession?, pid: Int) {}
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
        override fun onEmulatorSet() {}

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
