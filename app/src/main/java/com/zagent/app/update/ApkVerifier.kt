package com.zagent.app.update

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import java.io.File
import java.security.MessageDigest

object AppVersion {
    fun name(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    } catch (e: Exception) {
        "?"
    }

    fun code(context: Context): Long = try {
        codeOf(context.packageManager.getPackageInfo(context.packageName, 0))
    } catch (e: Exception) {
        0L
    }

    @Suppress("DEPRECATION")
    fun codeOf(info: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
}

object ApkVerifier {
    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** Retourne null si le fichier est valide, sinon un message d'erreur lisible. */
    fun verify(context: Context, file: File, expectedSize: Long, expectedSha256: String?): String? {
        if (!file.exists() || !file.isFile) return "Fichier de mise à jour introuvable."
        if (expectedSize > 0 && file.length() != expectedSize) {
            return "Taille incorrecte : le fichier est incomplet ou corrompu."
        }
        if (expectedSha256 != null && !sha256(file).equals(expectedSha256, ignoreCase = true)) {
            return "Empreinte SHA-256 incorrecte : fichier corrompu ou modifié."
        }
        val archive = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
            ?: return "Le fichier téléchargé n'est pas un APK valide."
        if (archive.packageName != context.packageName) {
            return "Cet APK appartient à une autre application."
        }
        if (AppVersion.codeOf(archive) <= AppVersion.code(context)) {
            return "Cette version n'est pas plus récente que celle installée."
        }
        return null
    }
}