package com.zagent.app.data

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File

/**
 * Accès aux fichiers du projet, sur de vrais fichiers (le même dossier que le terminal).
 * [rootPath] = chemin absolu du dossier du projet, par exemple
 * /data/data/com.zagent/files/home/projects/monprojet
 */
class FileRepository(private val context: Context?, private val rootPath: String?) : FileRepositoryApi {

    private fun getRootFolder(): File? {
        val p = rootPath ?: return null
        if (!p.startsWith("/")) return null
        val f = File(p)
        return if (f.isDirectory) f else null
    }

    /**
     * corr-098 — Verrou par chemin canonique : deux écritures concurrentes sur
     * le même fichier sont sérialisées (le HashMap est lui-même protégé par
     * synchronized ; le verrou du chemin est pris pendant toute l'écriture).
     */
    private val pathLocks = HashMap<String, Any>()

    private fun lockFor(canonical: String): Any =
        synchronized(pathLocks) { pathLocks.getOrPut(canonical) { Any() } }

    /** Décode les séquences %XX (corr-026 : %2e%2e = « .. » encodé). */
    private fun decodePercent(p: String): String {
        if (!p.contains('%')) return p
        return Regex("%[0-9A-Fa-f]{2}").replace(p) { m ->
            m.value.substring(1).toInt(16).toChar().toString()
        }
    }

    /**
     * corr-026 — Validation stricte d'un chemin relatif : après décodage
     * percent-encoding, refuse toute traversée (« .. », séparateurs Windows),
     * tout caractère nul. La containment canonique est vérifiée dans
     * [resolveInside] (liens symboliques inclus, corr-025).
     */
    private fun validatePath(path: String): Boolean {
        if (path.isEmpty()) return true
        val decoded = decodePercent(path)
        if (decoded.contains("..")) return false
        if (decoded.contains('\\')) return false
        if (decoded.contains('\u0000')) return false
        return true
    }

    /**
     * corr-025 — Résout [path] dans le projet (existe ou non). Retourne null si
     * le chemin sort du projet, y compris via un lien symbolique : le chemin
     * canonique (qui suit les liens) doit rester sous la racine canonique.
     */
    private fun resolveInside(root: File, path: String): File? {
        if (!validatePath(path)) return null
        val clean = path.removePrefix("./").removePrefix("/")
        if (clean.isEmpty() || clean == ".") return root
        val f = File(root, clean)
        return try {
            val rc = root.canonicalPath
            val fc = f.canonicalPath
            if (fc == rc || fc.startsWith(rc + File.separator)) f else null
        } catch (e: Exception) {
            null
        }
    }

    private fun resolveExisting(root: File, path: String): File? =
        resolveInside(root, path)?.takeIf { it.exists() }

    override fun listDir(path: String): String {
        if (!validatePath(path)) return "Erreur: Chemin invalide (.. interdit)"
        val root = getRootFolder() ?: return "Erreur: Aucun dossier de travail sélectionné."
        val target = resolveExisting(root, path) ?: return "Erreur: Dossier introuvable: $path"
        if (!target.isDirectory) return "Erreur: Ce n'est pas un dossier: $path"

        val ignoredDirs = setOf(".git", "node_modules", ".godot", "build")
        val resultList = mutableListOf<String>()
        var count = 0

        fun recursiveList(dir: File, currentRelPath: String, depth: Int) {
            if (depth > 4 || count >= 300) return
            val files = (dir.listFiles() ?: return).sortedBy { it.name.lowercase() }
            for (file in files) {
                if (count >= 300) break
                val name = file.name
                if (file.isDirectory && ignoredDirs.contains(name)) continue
                val rel = if (currentRelPath.isEmpty()) name else "$currentRelPath/$name"
                val type = if (file.isDirectory) "d" else "f"
                resultList.add("[$type] $rel")
                count++
                if (file.isDirectory) {
                    recursiveList(file, rel, depth + 1)
                }
            }
        }

        val startRel = if (path == "." || path.isEmpty()) "" else path.removePrefix("./").removePrefix("/")
        recursiveList(target, startRel, 1)
        return if (resultList.isEmpty()) "Dossier vide." else resultList.joinToString("\n")
    }

    data class Entry(
        val name: String,
        val isDir: Boolean,
        val size: Long,
        val path: String,
        /** Dernière modification (ms) — tri par date de l'explorateur (MA85). */
        val lastModified: Long = 0L
    )

    /**
     * Liste plate et structurée d'un dossier (pour l'explorateur).
     * [path] = "." ou chemin relatif. Tri : dossiers d'abord puis alphabétique.
     */
    override fun listEntries(path: String): List<Entry> =
        listEntriesResult(path).getOrDefault(emptyList())

    /** Variante diagnostique : distingue un dossier réellement vide d'un dossier illisible/invalide. */
    override fun listEntriesResult(path: String): Result<List<Entry>> = runCatching {
        require(!path.contains("..")) { "Chemin invalide : .. n'est pas autorisé." }
        val root = getRootFolder()
            ?: throw IllegalStateException("Aucun dossier de projet valide n'est sélectionné.")
        val target = resolveExisting(root, path)
            ?: throw IllegalStateException("Dossier introuvable : $path")
        require(target.isDirectory) { "Ce chemin n'est pas un dossier : $path" }
        val base = if (path == "." || path.isEmpty() || path == "/") "" else path.removePrefix("./").removePrefix("/")
        val files = target.listFiles()
            ?: throw SecurityException("Accès refusé au dossier : $path")
        files.map { f ->
            Entry(
                name = f.name,
                isDir = f.isDirectory,
                size = f.length(),
                path = if (base.isEmpty()) f.name else "$base/${f.name}",
                lastModified = f.lastModified()
            )
        }.sortedWith(compareByDescending<Entry> { it.isDir }.thenBy { it.name.lowercase() })
    }

    /** URI de partage d'un fichier du projet (via FileProvider dédié, maj-076). */
    override fun fileUri(path: String): Uri? {
        if (!validatePath(path)) return null
        val root = getRootFolder() ?: return null
        val file = resolveExisting(root, path) ?: return null
        if (file.isDirectory) return null
        if (context == null) return null
        return try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileshareprovider", file)
        } catch (e: Exception) {
            null
        }
    }

    /** Les fichiers texte sont lus dans l'aperçu intégré ; les autres passent par Android. */
    override fun isTextFile(path: String): Boolean {
        val extension = path.substringAfterLast('.', "").lowercase()
        return extension in setOf(
            "txt", "md", "markdown", "kt", "kts", "java", "js", "jsx", "ts", "tsx",
            "json", "xml", "yaml", "yml", "toml", "gradle", "properties", "pro",
            "py", "sh", "html", "htm", "css", "scss", "sql", "csv", "log", "ini",
            "conf", "env", "gitignore", "dockerfile"
        )
    }

    override fun mimeType(path: String): String {
        if (isTextFile(path)) return "text/plain"
        val extension = path.substringAfterLast('.', "").lowercase()
        return if (extension.isEmpty()) "application/octet-stream"
        else MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: "application/octet-stream"
    }

    override fun folderName(): String {
        val root = getRootFolder() ?: return "Projet"
        return root.name.ifEmpty { "Projet" }
    }

    override fun readFile(path: String): String {
        if (!validatePath(path)) return "Erreur: Chemin invalide (.. interdit)"
        val root = getRootFolder() ?: return "Erreur: Aucun dossier de travail sélectionné."
        val target = resolveExisting(root, path) ?: return "Erreur: Fichier introuvable: $path"
        if (target.isDirectory) return "Erreur: C'est un dossier, pas un fichier: $path"

        return try {
            target.inputStream().bufferedReader(Charsets.UTF_8).use { reader ->
                // corr-073 : un BOM UTF-8 éventuel est retiré (il apparaîtrait
                // sinon comme ﻿ en tête du contenu affiché à l'agent).
                reader.mark(1)
                if (reader.read() != 0xFEFF.toChar().code) reader.reset()
                val sb = StringBuilder()
                var charCount = 0
                val buffer = CharArray(1024)
                var read: Int
                while (reader.read(buffer).also { read = it } != -1) {
                    sb.append(buffer, 0, read)
                    charCount += read
                    if (charCount >= 20000) {
                        sb.append("\n[Tronqué à 20000 caractères]")
                        break
                    }
                }
                sb.toString()
            }
        } catch (e: Exception) {
            "Erreur lors de la lecture (fichier binaire ou corrompu ?): ${e.localizedMessage}"
        }
    }

    /**
     * feat-036 — Informations d'un fichier : type, taille, dernière
     * modification, nombre de lignes si textuel (< 2 Mo).
     */
    override fun fileInfo(path: String): String {
        if (!validatePath(path)) return "Erreur: Chemin invalide (.. interdit)"
        val root = getRootFolder() ?: return "Erreur: Aucun dossier de travail sélectionné."
        val f = resolveInside(root, path) ?: return "Erreur: Chemin hors du projet: $path"
        if (!f.exists()) return "Erreur: Fichier introuvable: $path"
        val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
            .format(java.util.Date(f.lastModified()))
        val type = if (f.isDirectory) "dossier" else mimeType(path)
        val lignes = if (f.isFile && f.length() < 2_000_000L) {
            runCatching { f.readLines().size }.getOrDefault(0)
        } else 0
        return buildString {
            append("Chemin : ").append(path)
            append("\nType : ").append(type)
            append("\nTaille : ").append(f.length()).append(" octets")
            append("\nModifié : ").append(date)
            if (lignes > 0) append("\nLignes : ").append(lignes)
        }
    }

    /** Lecture binaire d'une ressource du projet (aperçu web). Limite : 20 Mo. */
    override fun readBytes(path: String): ByteArray? {
        if (!validatePath(path)) return null
        val root = getRootFolder() ?: return null
        val target = resolveExisting(root, path) ?: return null
        if (target.isDirectory || target.length() > 20_000_000L) return null
        return try {
            target.readBytes()
        } catch (e: Exception) {
            null
        }
    }

    /** Lecture complète (sans troncature) pour l'éditeur. Limite : 500 Ko. */
    override fun readFileForEdit(path: String): Result<String> {
        if (!validatePath(path)) return Result.failure(IllegalArgumentException("Chemin invalide (.. interdit)"))
        val root = getRootFolder()
            ?: return Result.failure(IllegalStateException("Aucun dossier de travail sélectionné."))
        val target = resolveExisting(root, path)
            ?: return Result.failure(IllegalStateException("Fichier introuvable: $path"))
        if (target.isDirectory) return Result.failure(IllegalStateException("C'est un dossier: $path"))
        if (target.length() > 500_000L) {
            return Result.failure(IllegalStateException("Fichier trop volumineux pour l'éditeur (> 500 Ko)."))
        }
        return runCatching {
            // corr-073 : UTF-8 strict, BOM retiré pour l'édition ; les fins de
            // ligne (CRLF/LF) sont préservées telles quelles.
            val text = target.readBytes().toString(Charsets.UTF_8)
            if (text.startsWith("\uFEFF")) text.substring(1) else text
        }
    }

    override fun createFile(path: String): String {
        if (!validatePath(path)) return "Erreur: Chemin invalide (.. interdit)"
        val root = getRootFolder() ?: return "Erreur: Aucun dossier de travail sélectionné."
        val target = resolveInside(root, path) ?: return "Erreur: Chemin invalide: $path"
        if (target.exists()) return "Erreur: Existe déjà: $path"
        return try {
            target.parentFile?.mkdirs()
            if (target.createNewFile()) "Succès: Fichier créé $path" else "Erreur: Impossible de créer $path"
        } catch (e: Exception) {
            "Erreur: Impossible de créer $path (${e.localizedMessage})"
        }
    }

    override fun deletePath(path: String): String {
        if (!validatePath(path)) return "Erreur: Chemin invalide (.. interdit)"
        if (path == "." || path.isEmpty() || path == "/") return "Erreur: Impossible de supprimer la racine."
        val root = getRootFolder() ?: return "Erreur: Aucun dossier de travail sélectionné."
        val target = resolveExisting(root, path) ?: return "Erreur: Introuvable: $path"
        if (target.canonicalPath == root.canonicalPath) return "Erreur: Impossible de supprimer la racine."
        return try {
            val ok = if (target.isDirectory) target.deleteRecursively() else target.delete()
            if (ok) "Succès: Supprimé $path" else "Erreur: Suppression impossible: $path"
        } catch (e: Exception) {
            "Erreur: ${e.localizedMessage}"
        }
    }

    override fun renamePath(path: String, newName: String): String {
        if (!validatePath(path) || newName.contains('/') || newName.contains("..")) return "Erreur: Nom invalide"
        val root = getRootFolder() ?: return "Erreur: Aucun dossier de travail sélectionné."
        val target = resolveExisting(root, path) ?: return "Erreur: Introuvable: $path"
        val parent = target.parentFile ?: return "Erreur: Dossier parent introuvable"
        val dest = File(parent, newName)
        if (dest.exists()) return "Erreur: Existe déjà: $newName"
        return try {
            if (target.renameTo(dest)) "Succès: Renommé en $newName" else "Erreur: Renommage impossible"
        } catch (e: Exception) {
            "Erreur: ${e.localizedMessage}"
        }
    }

    override fun makeDir(path: String): String {
        if (!validatePath(path)) return "Erreur: Chemin invalide (.. interdit)"
        val root = getRootFolder() ?: return "Erreur: Aucun dossier de travail sélectionné."
        val target = resolveInside(root, path) ?: return "Erreur: Chemin invalide: $path"
        if (target.isDirectory) return "Succès: Dossier créé $path"
        return if (target.mkdirs()) "Succès: Dossier créé $path" else "Erreur: Impossible de créer le dossier $path"
    }

    override fun writeFile(path: String, content: String): String {
        if (!validatePath(path)) return "Erreur: Chemin invalide (.. interdit)"
        val root = getRootFolder() ?: return "Erreur: Aucun dossier de travail sélectionné."
        val target = resolveInside(root, path) ?: return "Erreur: Chemin invalide: $path"
        if (target.isDirectory) return "Erreur: C'est un dossier: $path"
        // corr-098 : une seule écriture à la fois par chemin canonique.
        synchronized(lockFor(target.canonicalPath)) { return writeFileLocked(target, path, content) }
    }

    private fun writeFileLocked(target: File, path: String, content: String): String {
        if (target.isDirectory) return "Erreur: C'est un dossier: $path"
        val tmp = File(target.parentFile, target.name + ".zagent-tmp")
        return try {
            target.parentFile?.mkdirs()
            // Écriture atomique : le fichier n'est remplacé qu'une fois le contenu
            // intégralement enregistré (un crash ne laisse plus de fichier tronqué).
            tmp.writeBytes(content.toByteArray(Charsets.UTF_8))
            if (tmp.renameTo(target)) {
                "Succès: Fichier écrit $path"
            } else {
                // Repli rare (file system différent) : copie directe.
                target.writeBytes(tmp.readBytes())
                tmp.delete()
                "Succès: Fichier écrit $path"
            }
        } catch (e: Exception) {
            tmp.delete()
            "Erreur d'écriture: ${e.localizedMessage}"
        }
    }
}
