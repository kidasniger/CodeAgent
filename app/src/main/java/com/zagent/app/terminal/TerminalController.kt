package com.zagent.app.terminal

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.system.Os
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import kotlin.math.roundToInt
import com.zagent.app.data.Constants

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

        /** MA83 : bornes de la taille de police du terminal (sp). */
        const val MIN_FONT_SIZE = 10
        const val MAX_FONT_SIZE = 22
    }

    // Le préfixe est /data/data/com.zagent/files/usr (bootstrap officiel adapté à l'extraction).
    private val baseDir = File("/data/data/${app.packageName}/files")
    private val prefixDir = File(baseDir, "usr")
    private val homeDir = File(baseDir, "home")
    private val stagingDir = File(baseDir, "usr-staging")

    var state by mutableStateOf<State>(if (isInstalled()) State.Ready else State.Idle)
        private set

    // corr-005 : l'état et la logique d'onglets vivent dans TerminalViewModel ;
    // le contrôleur reste responsable du bootstrap et de la création de session,
    // injectée ici comme fabrique (null = bootstrap incomplet, pas d'onglet).
    private val tabState = TerminalViewModel {
        if (!isInstalled()) null else createSession()
    }

    /** Onglets shell (corr-005 : délégation vers [TerminalViewModel.tabs]). */
    val tabs: List<TerminalViewModel.ShellTab> get() = tabState.tabs

    /** Onglet affiché ; TerminalScreen le modifie directement (API conservée). */
    var activeTab: Int
        get() = tabState.activeTab
        set(value) { tabState.activeTab = value }

    val session: TerminalSession? get() = tabState.session
    val sessionEnded: Boolean get() = tabState.sessionEnded

    private val prefs = app.getSharedPreferences("terminal_prefs", Context.MODE_PRIVATE)

    /** Taille du texte du terminal (réglable au pincement — MA83 — ou par A− / A+). */
    var fontSize by mutableIntStateOf(prefs.getInt("font_size", 14))
        private set

    fun changeFontSize(delta: Int) {
        updateFontSize(fontSize + delta)
    }

    /** MA83 : fixe la taille (10 à 22 sp), la mémorise et l'annonce.
     *  Nom « updateFontSize » : un « setFontSize » entrerait en conflit de
     *  signature JVM avec le setter généré de la propriété [fontSize]. */
    fun updateFontSize(value: Int) {
        val target = value.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)
        if (target == fontSize) return
        fontSize = target
        prefs.edit().putInt("font_size", target).apply()
        toastSize(target)
    }

    private var lastToastAt = 0L

    /** Toast « 14 sp » (MA83), au plus un toutes les 300 ms pendant le pincement. */
    private fun toastSize(size: Int) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastToastAt < 300) return
        lastToastAt = now
        com.zagent.app.ui.components.common.ZEphemeral.show("$size sp")
    }

    var ctrlActive by mutableStateOf(false)

    // ── feat-051 : thèmes ANSI du terminal ──────────────────────────────
    /** Palettes ANSI proposées pour le fond sombre (le clair garde la palette MA84). */
    val themesAnsi = listOf("zagent", "nord", "monokai", "gruvbox")

    var themeAnsi by mutableStateOf(prefs.getString("ansi_theme", "zagent") ?: "zagent")
        private set

    fun reglerThemeAnsi(id: String) {
        if (id !in themesAnsi) return
        themeAnsi = id
        prefs.edit().putString("ansi_theme", id).apply()
        applyToEmulator()
    }

    // ── feat-047 : historique persistant des commandes ───────────────────
    /** Commandes exécutées (agent et exécutions de fichier), plafonnées à 100, persistées. */
    var historiqueCommandes by mutableStateOf(lireHistoriqueCommandes())
        private set

    private fun lireHistoriqueCommandes(): List<String> = runCatching {
        val arr = org.json.JSONArray(prefs.getString("cmd_history", "[]") ?: "[]")
        (0 until arr.length()).map { arr.getString(it) }
    }.getOrDefault(emptyList())

    fun ajouterHistorique(commande: String) {
        val c = commande.trim().take(300)
        if (c.isEmpty()) return
        historiqueCommandes = ((historiqueCommandes - c) + c).takeLast(100)
        prefs.edit().putString("cmd_history", org.json.JSONArray(historiqueCommandes).toString()).apply()
    }

    fun viderHistorique() {
        historiqueCommandes = emptyList()
        prefs.edit().putString("cmd_history", "[]").apply()
    }

    // ── feat-048 : onglets nommés ───────────────────────────────────────
    /** Renomme (ou efface, nom vide) l'onglet [index] et persiste par numéro. */
    fun renommerOnglet(index: Int, nom: String) {
        val t = tabState.tabs.getOrNull(index) ?: return
        val n = nom.trim().take(24)
        t.nom = n.ifEmpty { null }
        val obj = runCatching { org.json.JSONObject(prefs.getString("tab_names", "{}") ?: "{}") }.getOrDefault(org.json.JSONObject())
        if (t.nom == null) obj.remove(t.number.toString()) else obj.put(t.number.toString(), t.nom)
        prefs.edit().putString("tab_names", obj.toString()).apply()
    }

    private fun restaurerNomOnglet(tab: TerminalViewModel.ShellTab) {
        val obj = runCatching { org.json.JSONObject(prefs.getString("tab_names", "{}") ?: "{}") }.getOrNull() ?: return
        tab.nom = obj.optString(tab.number.toString(), "").ifEmpty { null }
    }

    /** feat-049 — Partage la sortie (agent si présente, sinon onglet actif). */
    fun partagerSortie() {
        val texte = agentLog.trim().takeIf { it.isNotEmpty() }
            ?: session?.emulator?.screen?.transcriptText?.trimEnd().orEmpty()
        if (texte.isEmpty()) return
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, texte.takeLast(Constants.TERMINAL_COPIE_MAX))
        }
        app.startActivity(android.content.Intent.createChooser(intent, "Partager la sortie du terminal"))
    }

    // ── feat-054 : .bashrc modifiable avec modèles d'alias ──────────────
    /** Lit ~/.bashrc (vide s'il n'existe pas encore). */
    fun lireBashrc(): String =
        runCatching { File(homeDir, ".bashrc").takeIf { it.exists() }?.readText().orEmpty() }
            .getOrDefault("")

    /** Écrit ~/.bashrc ; il sera sourcé par les prochains shells interactifs. */
    fun ecrireBashrc(contenu: String): Boolean = runCatching {
        val f = File(homeDir, ".bashrc")
        f.parentFile?.mkdirs()
        f.writeText(contenu)
    }.isSuccess

    /** Journal en lecture seule des commandes lancées par l'agent (onglet « Agent » du terminal). */
    var agentLog by mutableStateOf("")
        private set
    private var agentBase = ""

    /** Ajoute une ligne au journal de l'onglet Agent (actions de l'agent autres que les commandes). */
    @Synchronized
    fun logAgent(line: String) {
        agentBase = (agentBase + line + "\n").takeLast(Constants.JOURNAL_AGENT_MAX)
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
        // err-067 — tue aussi les processus enfants (npm → node…) : pas d'orphelins.
        currentProc?.let { killTree(it) }
        currentProc = null
    }

    private fun isInstalled(): Boolean = File(prefixDir, "bin/bash").exists()

    // ── Installation ────────────────────────────────────────────────────────

    /** err-018 : (re)pose le bit d'exécution de bash s'il manque après une restauration. */
    private fun ensureBashExecutable() {
        val bash = File(prefixDir, "bin/bash")
        if (bash.exists() && !bash.canExecute()) {
            runCatching { Os.chmod(bash.absolutePath, 0x1C0) } // 0700
        }
    }

    /**
     * err-018 : traduit un échec de lancement du système en message clair.
     * error=13 (Permission denied) vient de la restriction W^X d'Android (exec
     * interdit dans le dossier privé quand targetSdk ≥ 29) ou d'un bit +x absent.
     */
    private fun messageExec(e: Exception): String {
        val m = e.message ?: return e.javaClass.simpleName
        return if ("error=13" in m || "Permission denied" in m) {
            "bash n'a pas pu être exécuté (permission refusée, error=13). " +
                "Installe la dernière version de l'application (correctif du terminal), " +
                "puis réessaie ; si l'échec persiste, vide les données de l'appli."
        } else m
    }

    /** err-065/075/076 — traduit les échecs courants (commande absente, git, ssh) en consignes actionnables. */
    private fun astuceSortie(sortie: String): String? {
        val manquante = Regex("bash:\\s*(\\S+):\\s*command not found").find(sortie)?.groupValues?.get(1)
        if (manquante != null) {
            return "Astuce: « $manquante » n'est pas installé — lance pkg install $manquante puis réessaie."
        }
        return when {
            "not a git repository" in sortie ->
                "Astuce: ce dossier n'est pas un dépôt git — initialise-le avec git init avant les commandes git."
            "Authentication failed" in sortie ->
                "Astuce: authentification refusée — vérifie le jeton (Réglages › GitHub) ou les identifiants du dépôt distant."
            "CONFLICT" in sortie ->
                "Astuce: conflit git — résous les fichiers marqués « <<<<<<< », puis git add et commit."
            "could not read Username" in sortie ->
                "Astuce: le dépôt distant demande des identifiants — configure le jeton dans l'URL (https://user:token@host/…)."
            "Host key verification failed" in sortie ->
                "Astuce: clé d'hôte SSH inconnue — accepte-la une première fois (ssh -o StrictHostKeyChecking=accept-new) ou ajoute-la à ~/.ssh/known_hosts."
            "Permissions" in sortie && "too open" in sortie ->
                "Astuce: ta clé privée SSH est trop accessible — lance chmod 600 ~/.ssh/id_rsa."
            "Permission denied (publickey" in sortie ->
                "Astuce: le serveur refuse ta clé publique — ajoute-la au authorized_keys du serveur."
            else -> null
        }
    }

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
        // err-018 : diagnostic précoce — Android 10+ refuse exec() des binaires du
        // dossier privé quand l'application cible SDK ≥ 29 (restriction W^X).
        // Un APK ancien échouerait toujours en error=13 : on l'annonce clairement.
        if (Build.VERSION.SDK_INT >= 29 && app.applicationInfo.targetSdkVersion >= 29) {
            state = State.Failed(
                "Cet APK cible un SDK ≥ 29 : Android bloque l'exécution du système (error=13). " +
                    "Installe la version corrigée de l'application (cible SDK 28)."
            )
            return
        }
        state = State.Working("Recherche du système", null)
        scope.launch(Dispatchers.IO) {
            try {
                // err-018 : diagnostic d'espace disque — échec tôt et explicite
                // (l'archive décompressée pèse plusieurs centaines de Mo).
                try {
                    val st = android.os.StatFs(baseDir.absolutePath)
                    val free = st.availableBytes
                    if (free < 500L * 1024 * 1024) {
                        throw IllegalStateException(
                            "Espace insuffisant : ${free / (1024L * 1024L)} Mo libres, ~500 Mo nécessaires pour le système"
                        )
                    }
                } catch (e: IllegalStateException) {
                    throw e
                } catch (e: Exception) {
                    // StatFs indisponible : on tente quand même.
                }
                val (url, expectedSha) = resolveBootstrapUrl(arch)
                val zip = File(baseDir, "bootstrap-$arch.zip")
                // err-018 : une tentative de reprise sur le téléchargement (réseau capricieux).
                try {
                    download(url, zip, expectedSha)
                } catch (e: Exception) {
                    setState(State.Working("Nouvelle tentative de téléchargement", null))
                    download(url, zip, expectedSha)
                }
                setState(State.Working("Extraction", null))
                extract(zip)
                zip.delete()
                ensureBashExecutable()
                setState(State.Working("Configuration", null))
                installDebPatcher()
                runSecondStage()
                ensureMirror()
                if (!isInstalled()) error("bin/bash introuvable après l'extraction")
                setState(State.Ready)
            } catch (e: Exception) {
                stagingDir.deleteRecursively()
                setState(State.Failed(messageExec(e)))
            }
        }
    }

    /**
     * err-074 — « Réparer le terminal » : repose les bits d'exécution, réinstalle
     * le crochet apt et relance la configuration, sans retélécharger le système.
     * Replis sur install() si le préfixe est absent.
     */
    fun reparer() {
        if (state is State.Working) return
        if (!isInstalled()) { install(); return }
        state = State.Working("Réparation du système", null)
        scope.launch(Dispatchers.IO) {
            try {
                java.nio.file.Files.walk(prefixDir.toPath()).use { stream ->
                    stream.forEach { p ->
                        val f = p.toFile()
                        if (f.isFile) {
                            val parent = f.parentFile?.absolutePath ?: ""
                            val executable = parent.endsWith("/bin") || "/libexec/" in parent ||
                                "/apt/methods/" in parent || f.name.endsWith(".sh")
                            if (executable) runCatching { Os.chmod(f.absolutePath, 0x1C0) }
                        }
                    }
                }
                installDebPatcher()
                runSecondStage()
                ensureMirror()
                setState(if (isInstalled()) State.Ready else State.Failed("Réparation incomplète — réinstalle avec Réessayer."))
            } catch (e: Exception) {
                setState(State.Failed("Réparation impossible : " + (e.message ?: e.javaClass.simpleName)))
            }
        }
    }

    /** Trouve le dernier bootstrap officiel Termux (variante Android 7+) : URL + empreinte SHA-256. */
    private fun resolveBootstrapUrl(arch: String): Pair<String, String?> {
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
                    if (a.optString("name") == "bootstrap-$arch.zip") {
                        val digest = a.optString("digest")
                        val sha = if (digest.startsWith("sha256:", true)) digest.substring(7).lowercase() else null
                        return a.getString("browser_download_url") to sha?.takeIf { it.length == 64 }
                    }
                }
            }
        }
        error("Aucun bootstrap Termux trouvé")
    }

    private suspend fun setState(s: State) = withContext(Dispatchers.Main) { state = s }

    private suspend fun download(url: String, dest: File, expectedSha256: String?) {
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
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            body.byteStream().use { input ->
                FileOutputStream(dest).use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        digest.update(buf, 0, n)
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
            // Intégrité : empreinte publiée par l'API GitHub, sinon signature ZIP.
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (expectedSha256 != null && actual != expectedSha256) {
                dest.delete()
                error("Empreinte du système invalide (téléchargement corrompu ou altéré)")
            }
            dest.inputStream().use { ins ->
                val magic = ByteArray(2)
                if (ins.read(magic) != 2 || !magic.contentEquals(byteArrayOf(0x50, 0x4B))) {
                    dest.delete()
                    error("Le fichier téléchargé n'est pas une archive ZIP valide")
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
        val p = try {
            pb.start()
        } catch (e: Exception) {
            error(messageExec(e))
        }
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
        ajouterHistorique(command) // feat-047
        File(prefixDir, "tmp").mkdirs()
        ensureBashExecutable()
        // err-070 — environnement cohérent avant chaque commande (PATH, HOME, PREFIX).
        if (!File(prefixDir, "bin").isDirectory) {
            return@withContext "Erreur: le système du terminal est incomplet (usr/bin absent) — ouvre l'écran Terminal et réinstalle."
        }
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
            agentBase = (agentLog + "\n$ " + command + "\n").takeLast(Constants.JOURNAL_AGENT_MAX)
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
            return@withContext "Erreur: impossible de lancer la commande (${messageExec(e)})"
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
                        // err-067 — arrêt complet de l'arbre de processus.
                        killTree(proc)
                        break
                    }
                    if (System.currentTimeMillis() - started > timeoutSec * 1000L) {
                        timedOut = true
                        killTree(proc)
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

        val out = com.zagent.app.ui.components.common.cleanForModel(sb.toString()).let {
            if (it.length > outLimit) "[début de la sortie tronqué]\n" + it.takeLast(outLimit) else it
        }
        var outFinal = out
        // err-069 — caractères invalides dans la sortie : signalés clairement.
        if ('\uFFFD' in outFinal) {
            outFinal += "\n[sortie : caractères invalides remplacés (encodage non UTF-8)]"
        }
        // err-065/075/076 — consigne actionable quand la cause est reconnaissable.
        astuceSortie(sb.toString())?.let { outFinal += "\n$it" }
        val secs = "%.1f".format(java.util.Locale.US, (System.currentTimeMillis() - started) / 1000.0)
        val head = when {
            timedOut -> "Erreur: délai de $timeoutSec s dépassé, commande arrêtée ($secs s). " +
                "Si la commande a besoin de plus de temps, relance-la avec un délai plus grand."
            cancelled -> "Erreur: commande interrompue ($secs s)."
            userStopped -> "Erreur: commande arrêtée par l'utilisateur ($secs s)."
            code == 0 -> "Code de sortie: 0 ($secs s)"
            else -> "Erreur: code de sortie $code ($secs s)"
        }
        if (!silent) {
            agentBase = agentBase + sb.toString() + "[" + head.lineSequence().first() + "]\n"
            agentLog = agentBase
        }
        "$head\n$outFinal"
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
        var startError: String? = null
        val proc: Process = withContext(Dispatchers.IO) {
            File(prefixDir, "tmp").mkdirs()
            ensureBashExecutable()
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
            try { pb.start() } catch (e: Exception) { startError = messageExec(e); null }
        } ?: return "Erreur: impossible de lancer la commande." + (startError?.let { " ($it)" } ?: "")

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
                        val clean = com.zagent.app.ui.components.common.cleanForModel(sb.toString())
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

    /** Ouvre le premier onglet, ou relance l'onglet actif (corr-005 : délégué). */
    fun openSession() {
        tabState.openSession()
        // feat-048 — un onglet relancé retrouve son nom.
        tabState.tabs.getOrNull(tabState.activeTab)?.let { restaurerNomOnglet(it) }
    }

    /** Nouvel onglet shell dans le dossier du projet actif (corr-005 : délégué). */
    fun addTab() {
        tabState.addTab()
        // feat-048 — restaure le nom persisté du nouvel onglet.
        tabState.tabs.lastOrNull()?.let { restaurerNomOnglet(it) }
    }

    fun closeTab(index: Int) {
        tabState.closeTab(index)
    }

    private fun createSession(): TerminalSession? {
        ensureMirror()
        // Réinstalle le crochet apt à chaque ouverture : prend en compte un script mis à jour.
        try { installDebPatcher() } catch (e: Exception) { }
        ensureBashExecutable()
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
        cm.setPrimaryClip(ClipData.newPlainText("terminal", text.takeLast(Constants.TERMINAL_COPIE_MAX)))
        com.zagent.app.ui.components.common.ZEphemeral.show("Terminal copié")
    }

    private var colorBg = 0xFF0B0F17.toInt()
    private var colorFg = 0xFFE6EAF2.toInt()
    private var colorDark = true

    /** Palette ANSI 16 couleurs du shell, harmonisée avec le thème (MA84) — fond sombre. */
    /** Palette ANSI 16 couleurs du shell — Nord (feat-051). */
    private val SHELL_ANSI_NORD = intArrayOf(
        0xFF3B4252.toInt(), 0xFFBF616A.toInt(), 0xFFA3BE8C.toInt(), 0xFFEBCB8B.toInt(),
        0xFF81A1C1.toInt(), 0xFFB48EAD.toInt(), 0xFF88C0D0.toInt(), 0xFFE5E9F0.toInt(),
        0xFF4C566A.toInt(), 0xFFBF616A.toInt(), 0xFFA3BE8C.toInt(), 0xFFEBCB8B.toInt(),
        0xFF81A1C1.toInt(), 0xFFB48EAD.toInt(), 0xFF8FBCBB.toInt(), 0xFFECEFF4.toInt()
    )

    /** Palette ANSI 16 couleurs du shell — Monokai (feat-051). */
    private val SHELL_ANSI_MONOKAI = intArrayOf(
        0xFF272822.toInt(), 0xFFF92672.toInt(), 0xFFA6E22E.toInt(), 0xFFF4BF75.toInt(),
        0xFF66D9EF.toInt(), 0xFFAE81FF.toInt(), 0xFFA1EFE4.toInt(), 0xFFF8F8F2.toInt(),
        0xFF75715E.toInt(), 0xFFF92672.toInt(), 0xFFA6E22E.toInt(), 0xFFF4BF75.toInt(),
        0xFF66D9EF.toInt(), 0xFFAE81FF.toInt(), 0xFFA1EFE4.toInt(), 0xFFF9F8F5.toInt()
    )

    /** Palette ANSI 16 couleurs du shell — Gruvbox (feat-051). */
    private val SHELL_ANSI_GRUVBOX = intArrayOf(
        0xFF3C3836.toInt(), 0xFFCC241D.toInt(), 0xFF98971A.toInt(), 0xFFD79921.toInt(),
        0xFF458588.toInt(), 0xFFB16286.toInt(), 0xFF689D6A.toInt(), 0xFFA89984.toInt(),
        0xFF7C6F64.toInt(), 0xFFFB4934.toInt(), 0xFFB8BB26.toInt(), 0xFFFABD2F.toInt(),
        0xFF83A598.toInt(), 0xFFD3869B.toInt(), 0xFF8EC07C.toInt(), 0xFFEBDBB2.toInt()
    )

    /** Palette ANSI 16 couleurs du shell, harmonisée avec le thème (MA84) — fond sombre. */
    private val SHELL_ANSI_DARK = intArrayOf(
        0xFF2A2F3A.toInt(), 0xFFDC2626.toInt(), 0xFF16A34A.toInt(), 0xFFCA8A04.toInt(),
        0xFF2563EB.toInt(), 0xFF9333EA.toInt(), 0xFF0891B2.toInt(), 0xFF9CA3AF.toInt(),
        0xFF6B7280.toInt(), 0xFFEF4444.toInt(), 0xFF22C55E.toInt(), 0xFFEAB308.toInt(),
        0xFF3B82F6.toInt(), 0xFFA855F7.toInt(), 0xFF06B6D4.toInt(), 0xFFE5E7EB.toInt()
    )

    /** Palette ANSI 16 couleurs du shell — fond clair (MA84). */
    private val SHELL_ANSI_LIGHT = intArrayOf(
        0xFF24292F.toInt(), 0xFFB91C1C.toInt(), 0xFF0F7B3E.toInt(), 0xFF8A6100.toInt(),
        0xFF0757C8.toInt(), 0xFF7C3AED.toInt(), 0xFF0E7490.toInt(), 0xFF51607A.toInt(),
        0xFF6E7B91.toInt(), 0xFFCB2B2B.toInt(), 0xFF188A4B.toInt(), 0xFF9A6E00.toInt(),
        0xFF2563EB.toInt(), 0xFF8B5CF6.toInt(), 0xFF0891B2.toInt(), 0xFF1F2937.toInt()
    )

    /** Aligne les couleurs du terminal (fond, texte, curseur et palette ANSI) sur le thème (MA84). */
    fun applyColors(bg: Int, fg: Int, dark: Boolean) {
        colorBg = bg
        colorFg = fg
        colorDark = dark
        applyToEmulator()
    }

    private fun applyToEmulator() {
        // feat-051 — palette choisie pour le fond sombre ; le clair garde MA84.
        val palette = when {
            !colorDark -> SHELL_ANSI_LIGHT
            themeAnsi == "nord" -> SHELL_ANSI_NORD
            themeAnsi == "monokai" -> SHELL_ANSI_MONOKAI
            themeAnsi == "gruvbox" -> SHELL_ANSI_GRUVBOX
            else -> SHELL_ANSI_DARK
        }
        tabs.forEach { t ->
            val e = t.session.emulator ?: return@forEach
            val cols = e.mColors.mCurrentColors
            for (i in 0..15) cols[i] = palette[i]
            cols[TextStyle.COLOR_INDEX_FOREGROUND] = colorFg
            cols[TextStyle.COLOR_INDEX_BACKGROUND] = colorBg
            cols[TextStyle.COLOR_INDEX_CURSOR] = colorFg
        }
        view?.onScreenUpdated()
    }

    /** MA84 : efface l'écran (et l'historique) de l'onglet actif. */
    fun clearActiveScreen() {
        val t = tabs.getOrNull(activeTab) ?: return
        t.session.emulator?.reset()
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

        /**
         * MA83 : pincement sur la vue du terminal. La vue accumule un facteur
         * d'échelle et nous le transmet ; on applique la taille (10 à 22 sp)
         * et on renvoie le facteur restant, non appliqué.
         */
        override fun onScale(scale: Float): Float {
            val current = fontSize.toFloat()
            val target = (current * scale).roundToInt().coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)
            if (target == fontSize) {
                // En butée : consomme le facteur pour rester réactif au pincement inverse.
                return if ((scale > 1f && fontSize >= MAX_FONT_SIZE) ||
                    (scale < 1f && fontSize <= MIN_FONT_SIZE)
                ) 1f else scale
            }
            val applied = target / current
            fontSize = target
            prefs.edit().putInt("font_size", target).apply()
            toastSize(target)
            return scale / applied
        }
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
