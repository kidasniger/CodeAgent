package com.zagent.app.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Vérifie la dernière release GitHub, télécharge l'APK avec WorkManager,
 * contrôle son intégrité puis ouvre l'installateur système (avec le consentement de l'utilisateur).
 */
object UpdateManager {
    private const val WORK_NAME = "zagent-update-download"
    private const val PREFS = "zagent_update"
    private const val KEY_DISMISSED = "dismissed_tag"
    private const val STALE_PART_MS = 24L * 60L * 60L * 1000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val checking = AtomicBoolean(false)
    private lateinit var appContext: Context
    private var initialized = false
    private var downloadJob: Job? = null

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _dismissedTag = MutableStateFlow<String?>(null)
    val dismissedTag: StateFlow<String?> = _dismissedTag.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        appContext = context.applicationContext
        initialized = true
        _dismissedTag.value = prefs().getString(KEY_DISMISSED, null)
        cleanupStale()
    }

    fun currentVersionName(): String = if (initialized) AppVersion.name(appContext) else "?"

    // ---------- Vérification ----------

    /** auto = vérification au démarrage (une seule fois par session) ; sinon vérification manuelle. */
    fun check(auto: Boolean) {
        if (!initialized) return
        scope.launch { runCheck(auto) }
    }

    private suspend fun runCheck(auto: Boolean) {
        val current = _state.value
        if (current is UpdateState.Checking || current is UpdateState.Downloading) return
        if (auto && current !is UpdateState.Idle) return
        if (!checking.compareAndSet(false, true)) return
        try {
            if (!auto) setDismissed(null)
            _state.value = UpdateState.Checking
            _state.value = withContext(Dispatchers.IO) { resolveState() }
        } catch (e: Exception) {
            _state.value = UpdateState.Error("Vérification impossible : ${e.localizedMessage ?: "erreur inconnue"}")
        } finally {
            checking.set(false)
        }
    }

    private fun resolveState(): UpdateState {
        val parsed = fetchLatest()
        if (parsed is ParseResult.Failure) return UpdateState.Error(parsed.message)
        val release = (parsed as ParseResult.Success).info

        val name = AppVersion.name(appContext)
        if (!VersionComparator.isNewer(release.tag, name, AppVersion.code(appContext))) {
            deleteAllApks()
            return UpdateState.UpToDate(name)
        }

        val resolved = resolveSha(release)
        if (resolved is ParseResult.Failure) return UpdateState.Error(resolved.message)
        val info = (resolved as ParseResult.Success).info

        val cached = findCachedApk(info)
        return if (cached != null) {
            UpdateState.Ready(info, cached.absolutePath)
        } else {
            UpdateState.Available(info, name)
        }
    }

    private fun fetchLatest(): ParseResult {
        val url = "https://api.github.com/repos/${UpdateConfig.GITHUB_OWNER}/${UpdateConfig.GITHUB_REPO}/releases/latest"
        var conn: HttpURLConnection? = null
        return try {
            conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "ZAgent-Android")
            conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            when (val code = conn.responseCode) {
                200 -> ReleaseParser.parse(conn.inputStream.bufferedReader().use { it.readText() })
                404 -> ParseResult.Failure("Aucune release publiée pour le moment.")
                403, 429 -> ParseResult.Failure("Limite GitHub atteinte, réessaie plus tard.")
                else -> ParseResult.Failure("Erreur GitHub (HTTP $code).")
            }
        } catch (e: java.io.IOException) {
            ParseResult.Failure("Pas de connexion, ou GitHub est injoignable.")
        } catch (e: Exception) {
            ParseResult.Failure("Vérification impossible : ${e.localizedMessage ?: "erreur inconnue"}")
        } finally {
            conn?.disconnect()
        }
    }

    private fun httpGetText(url: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "ZAgent-Android")
            if (conn.responseCode == 200) conn.inputStream.bufferedReader().use { it.readText() } else null
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    /** Si l'empreinte n'est pas fournie par l'API, lit le fichier .sha256 publié avec la release. */
    private fun resolveSha(info: ReleaseInfo): ParseResult {
        val shaUrl = info.sha256Url
        if (info.apk.sha256 != null || shaUrl == null) return ParseResult.Success(info)
        val text = httpGetText(shaUrl)
            ?: return ParseResult.Failure("Impossible de récupérer l'empreinte SHA-256 de la mise à jour.")
        val token = text.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
        val sha = ReleaseParser.normalizeSha(token)
            ?: return ParseResult.Failure("Le fichier SHA-256 de la release est invalide.")
        return ParseResult.Success(info.copy(apk = info.apk.copy(sha256 = sha)))
    }

    // ---------- Fichiers ----------

    private fun prefs() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun updatesDir(): File {
        val dir = File(appContext.cacheDir, "updates")
        dir.mkdirs()
        return dir
    }

    private fun findCachedApk(info: ReleaseInfo): File? {
        val dir = updatesDir()
        dir.listFiles()?.forEach { f ->
            if (f.name != info.apk.name && f.name != info.apk.name + ".part") f.delete()
        }
        val apk = File(dir, info.apk.name)
        if (apk.exists()) {
            if (ApkVerifier.verify(appContext, apk, info.apk.size, info.apk.sha256) == null) return apk
            apk.delete()
        }
        return null
    }

    private fun deleteAllApks() {
        updatesDir().listFiles()?.forEach { it.delete() }
    }

    private fun cleanupStale() {
        try {
            val now = System.currentTimeMillis()
            updatesDir().listFiles()?.forEach { f ->
                val stalePart = f.name.endsWith(".part") && now - f.lastModified() > STALE_PART_MS
                if (f.length() == 0L || stalePart) f.delete()
            }
        } catch (e: Exception) {
            // Nettoyage best-effort : on ignore.
        }
    }

    private fun setDismissed(tag: String?) {
        _dismissedTag.value = tag
        if (!initialized) return
        val editor = prefs().edit()
        if (tag == null) editor.remove(KEY_DISMISSED) else editor.putString(KEY_DISMISSED, tag)
        editor.apply()
    }

    // ---------- Téléchargement ----------

    fun startDownload() {
        if (!initialized) return
        val info = when (val s = _state.value) {
            is UpdateState.Available -> s.info
            is UpdateState.Error -> s.info
            else -> null
        } ?: return

        val data = Data.Builder()
            .putString(UpdateDownloadWorker.KEY_URL, info.apk.url)
            .putString(UpdateDownloadWorker.KEY_NAME, info.apk.name)
            .putLong(UpdateDownloadWorker.KEY_SIZE, info.apk.size)
            .putString(UpdateDownloadWorker.KEY_SHA, info.apk.sha256 ?: "")
            .build()
        val request = OneTimeWorkRequestBuilder<UpdateDownloadWorker>()
            .setInputData(data)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        _state.value = UpdateState.Downloading(info, 0, 0L, info.apk.size)
        observe(request.id, info)
    }

    fun cancelDownload() {
        if (!initialized) return
        WorkManager.getInstance(appContext).cancelUniqueWork(WORK_NAME)
    }

    private fun observe(id: UUID, info: ReleaseInfo) {
        downloadJob?.cancel()
        downloadJob = scope.launch {
            WorkManager.getInstance(appContext).getWorkInfoByIdFlow(id).collect { workInfo ->
                if (workInfo == null) return@collect
                when (workInfo.state) {
                    WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED -> {
                        val downloaded = workInfo.progress.getLong(UpdateDownloadWorker.KEY_DOWNLOADED, 0L)
                        val total = workInfo.progress.getLong(UpdateDownloadWorker.KEY_TOTAL, info.apk.size)
                        val percent = if (total > 0) ((downloaded * 100) / total).toInt().coerceIn(0, 100) else 0
                        _state.value = UpdateState.Downloading(info, percent, downloaded, total)
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val path = workInfo.outputData.getString(UpdateDownloadWorker.KEY_PATH)
                        setDismissed(null)
                        if (path != null) {
                            _state.value = UpdateState.Ready(info, path)
                        } else {
                            _state.value = UpdateState.Error("Téléchargement terminé mais fichier introuvable.", info)
                        }
                    }
                    WorkInfo.State.FAILED -> {
                        setDismissed(null)
                        val message = workInfo.outputData.getString(UpdateDownloadWorker.KEY_ERROR)
                            ?: "Le téléchargement a échoué."
                        _state.value = UpdateState.Error(message, info)
                    }
                    WorkInfo.State.CANCELLED -> {
                        _state.value = UpdateState.Available(info, AppVersion.name(appContext))
                    }
                }
            }
        }
    }

    // ---------- Installation ----------

    fun install() {
        if (!initialized) return
        val (info, path) = when (val s = _state.value) {
            is UpdateState.Ready -> Pair(s.info, s.apkPath)
            is UpdateState.NeedsPermission -> Pair(s.info, s.apkPath)
            else -> return
        }
        val file = File(path)
        if (!file.exists()) {
            _state.value = UpdateState.Error("Le fichier de mise à jour a disparu. Télécharge-le de nouveau.", info)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !appContext.packageManager.canRequestPackageInstalls()) {
            _state.value = UpdateState.NeedsPermission(info, path)
            val settings = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${appContext.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                appContext.startActivity(settings)
            } catch (e: ActivityNotFoundException) {
                _state.value = UpdateState.Error(
                    "Autorise l'installation d'applications inconnues pour ZAgent dans les paramètres Android.", info
                )
            }
            return
        }

        try {
            val uri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.updateprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
        } catch (e: Exception) {
            _state.value = UpdateState.Error("Impossible de lancer l'installation : ${e.localizedMessage}", info)
        }
    }

    /** Appelé à chaque retour dans l'app : reprend l'installation une fois l'autorisation accordée. */
    fun onResume() {
        if (!initialized) return
        val s = _state.value
        if (s is UpdateState.NeedsPermission &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            appContext.packageManager.canRequestPackageInstalls()
        ) {
            _state.value = UpdateState.Ready(s.info, s.apkPath)
            install()
        }
    }

    fun dismiss(tag: String) {
        setDismissed(tag)
    }
}