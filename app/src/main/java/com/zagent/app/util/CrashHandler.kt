package com.zagent.app.util

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * err-077 — Gestionnaire de crash : intercepte toute exception fatale non
 * traitée, l'écrit dans un journal local (files/crash/…), puis laisse le
 * système tuer le processus comme d'habitude. Le dernier rapport est proposé
 * à l'utilisateur au redémarrage (Rapport de diagnostic, err-079).
 *
 * Installé une seule fois au démarrage de l'application ; en debug, le
 * comportement par défaut (stacktrace logcat) est conservé via [previous].
 */
object CrashHandler {

    @Volatile
    private var installed = false

    fun installer(context: Context) {
        if (installed) return
        installed = true
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, erreur ->
            runCatching { ecrire(context, erreur, Thread.currentThread().name) }
            previous?.uncaughtException(thread, erreur)
        }
    }

    /** Dossier des rapports de crash (privé). */
    fun dossier(context: Context): File = File(context.filesDir, "crash").apply { mkdirs() }

    /** err-078 — dernier rapport de crash non encore vu au redémarrage (null si aucun). */
    fun dernierRapportNonVu(context: Context): File? {
        val vu = File(context.filesDir, "crash-vu.stamp")
        val seuil = if (vu.exists()) vu.lastModified() else 0L
        return dossier(context).listFiles()
            ?.filter { it.isFile && it.name.startsWith("crash-") }
            ?.maxByOrNull { it.lastModified() }
            ?.takeIf { it.lastModified() > seuil }
    }

    /** err-078 — marque les rapports actuels comme vus (l'utilisateur a été prévenu). */
    fun marquerVu(context: Context) {
        runCatching {
            val vu = File(context.filesDir, "crash-vu.stamp")
            if (!vu.exists()) vu.createNewFile()
            vu.setLastModified(System.currentTimeMillis())
        }
    }

    private fun ecrire(context: Context, erreur: Throwable, nomFil: String) {
        val horodatage = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val fichier = File(dossier(context), "crash-$horodatage.txt")
        fichier.writeText(
            buildString {
                appendLine("Crash : " + erreur.javaClass.name)
                appendLine("Message : " + (erreur.message ?: "-"))
                appendLine("Thread : " + nomFil)
                appendLine()
                appendLine(erreur.stackTraceToString())
                erreur.cause?.let {
                    appendLine()
                    appendLine("Cause :")
                    appendLine(it.stackTraceToString())
                }
            }
        )
        // Garde les 5 derniers rapports seulement.
        dossier(context).listFiles()
            ?.sortedByDescending { it.name }
            ?.drop(5)
            ?.forEach { it.delete() }
    }

    /** Dernier rapport de crash non encore lu, s'il existe (err-079). */
    fun dernierRapport(context: Context): String? {
        val f = dossier(context).listFiles()?.maxByOrNull { it.name } ?: return null
        return runCatching { f.readText() }.getOrNull()?.take(4000)
    }
}
