package com.zagent.app.update

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Télécharge l'APK dans le cache privé, dans un fichier .part (reprise possible avec Range),
 * puis vérifie taille, SHA-256, paquet et version avant de le renommer.
 */
class UpdateDownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) { execute() }

    private suspend fun execute(): Result {
        val url = inputData.getString(KEY_URL).orEmpty()
        val name = inputData.getString(KEY_NAME).orEmpty()
        val size = inputData.getLong(KEY_SIZE, -1L)
        val sha = inputData.getString(KEY_SHA)?.takeIf { it.isNotBlank() }
        if (!url.startsWith("https://") || name.isEmpty() || name.contains("/") || name.contains("..")) {
            return fail("Paramètres de téléchargement invalides.")
        }

        val dir = File(applicationContext.cacheDir, "updates")
        dir.mkdirs()
        val finalFile = File(dir, name)
        val part = File(dir, "$name.part")

        if (finalFile.exists()) {
            if (ApkVerifier.verify(applicationContext, finalFile, size, sha) == null) return ok(finalFile)
            finalFile.delete()
        }

        try {
            download(url, part, size)
        } catch (e: IOException) {
            if (runAttemptCount < 3) return Result.retry()
            return fail("Téléchargement interrompu : ${e.localizedMessage ?: "réseau indisponible"}. Réessaie.")
        }

        if (!part.renameTo(finalFile)) return fail("Impossible d'enregistrer le fichier téléchargé.")
        val error = ApkVerifier.verify(applicationContext, finalFile, size, sha)
        if (error != null) {
            finalFile.delete()
            return fail(error)
        }
        return ok(finalFile)
    }

    private suspend fun download(url: String, part: File, expected: Long) {
        var offset = if (part.exists()) part.length() else 0L
        if (expected > 0 && offset > expected) {
            part.delete()
            offset = 0L
        }
        if (expected > 0 && offset == expected) return

        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "ZAgent-Android")
        if (offset > 0) conn.setRequestProperty("Range", "bytes=$offset-")
        try {
            val code = conn.responseCode
            val append: Boolean
            if (code == HttpURLConnection.HTTP_PARTIAL) {
                append = true
            } else if (code == HttpURLConnection.HTTP_OK) {
                append = false
                offset = 0L
            } else {
                throw IOException("HTTP $code")
            }
            val remaining = conn.contentLengthLong
            val total = if (expected > 0) expected else if (remaining > 0) remaining + offset else -1L
            var downloaded = offset
            var lastReport = 0L
            val deadline = System.currentTimeMillis() + MAX_DURATION_MS
            conn.inputStream.use { input ->
                FileOutputStream(part, append).use { out ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        downloaded += n
                        val now = System.currentTimeMillis()
                        if (now > deadline) throw IOException("délai maximum dépassé")
                        if (now - lastReport > 400) {
                            lastReport = now
                            setProgress(workDataOf(KEY_DOWNLOADED to downloaded, KEY_TOTAL to total))
                        }
                    }
                }
            }
            setProgress(workDataOf(KEY_DOWNLOADED to downloaded, KEY_TOTAL to total))
        } finally {
            conn.disconnect()
        }
    }

    private fun ok(file: File): Result = Result.success(workDataOf(KEY_PATH to file.absolutePath))

    private fun fail(message: String): Result = Result.failure(workDataOf(KEY_ERROR to message))

    companion object {
        const val KEY_URL = "url"
        const val KEY_NAME = "name"
        const val KEY_SIZE = "size"
        const val KEY_SHA = "sha256"
        const val KEY_DOWNLOADED = "downloaded"
        const val KEY_TOTAL = "total"
        const val KEY_PATH = "path"
        const val KEY_ERROR = "error"
        private const val MAX_DURATION_MS = 9 * 60 * 1000L
    }
}