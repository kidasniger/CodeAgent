package com.zagent.app.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
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
        // corr-041 : la preuve d'intégrité est OBLIGATOIRE — sans empreinte
        // attendue (release sans fichier .sha256), l'installation est refusée.
        if (expectedSha256 == null) {
            return "Empreinte SHA-256 attendue absente pour cette mise à jour : installation refusée."
        }
        if (!sha256(file).equals(expectedSha256, ignoreCase = true)) {
            return "Empreinte SHA-256 incorrecte : fichier corrompu ou modifié."
        }
        val archive = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
            ?: return "Le fichier téléchargé n'est pas un APK valide."
        if (archive.packageName != context.packageName) {
            return "Cet APK appartient à une autre application."
        }
        // MA69 : l'APK doit être signé avec la même clé que l'application installée.
        verifyCertificateMatch(context, file)?.let { return it }
        if (AppVersion.codeOf(archive) <= AppVersion.code(context)) {
            return "Cette version n'est pas plus récente que celle installée."
        }
        return null
    }

    /**
     * MA69 : compare le certificat de signature du paquet téléchargé avec celui
     * de l'application installée. Refuse l'installation d'un APK signé par une
     * autre clé (Android refuserait de toute façon, mais on explique pourquoi).
     */
    fun verifyCertificateMatch(context: Context, file: File): String? {
        val pm = context.packageManager
        @Suppress("DEPRECATION")
        val archiveSigs = pm.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNATURES)
            ?.signatures.orEmpty().map { bytesSha256(it.toByteArray()) }.toSet()
        if (archiveSigs.isEmpty()) return null // signature illisible : le système tranchera
        val installed = installedSignatureDigests(context)
        if (installed.isEmpty()) return null
        return if (archiveSigs.intersect(installed).isEmpty()) {
            // err-094 — signature différente : expliquer la cause ET la marche à suivre.
            "Cet APK n'est pas signé avec la même clé que l'application installée : Android refuse la mise à jour. " +
                "Désinstalle d'abord l'application actuelle (sauvegarde tes projets), puis installe cette version."
        } else {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun installedSignatureDigests(context: Context): Set<String> {
        val pm = context.packageManager
        return if (Build.VERSION.SDK_INT >= 28) {
            val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val signing = info.signingInfo ?: return emptySet()
            val chain = if (signing.hasMultipleSigners()) signing.apkContentsSigners
            else signing.signingCertificateHistory
            chain.map { bytesSha256(it.toByteArray()) }.toSet()
        } else {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                .signatures.orEmpty().map { bytesSha256(it.toByteArray()) }.toSet()
        }
    }

    private fun bytesSha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}