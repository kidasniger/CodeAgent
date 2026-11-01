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
class FileRepository(private val context: Context, private val rootPath: String?) {

    private fun getRootFolder(): File? {
        val p = rootPath ?: return null
        if (!p.startsWith("/")) return null
        val f = File(p)
        return if (f.isDirectory) f else null
    }

    private fun validatePath(path: String): Boolean {
        if (path.contains("..")) return false
        return true
    }

    /** Résout [path] dans le projet (existe ou non). Retourne null si le chemin sort du projet. */
    private fun resolveInside(root: File, path: String): File? {
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

    fun listDir(path: String = "."): String {
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
        val path: String
    )

    /**
     * Liste plate et structurée d'un dossier (pour l'explorateur).
     * [path] = "." ou chemin relatif. Tri : dossiers d'abord puis alphabétique.
     */
    fun listEntries(path: String = "."): List<Entry> {
        if (!validatePath(path)) return emptyList()
        val root = getRootFolder() ?: return emptyList()
        val target = resolveExisting(root, path) ?: return emptyList()
        if (!target.isDirectory) return emptyList()
        val base = if (path == "." || path.isEmpty() || path == "/") "" else path.removePrefix("./").removePrefix("/")
        return (target.listFiles() ?: emptyArray())
            .map { f ->
                Entry(
                    name = f.name,
                    isDir = f.isDirectory,
                    size = f.length(),
                    path = if (base.isEmpty()) f.name else "$base/${f.name}"
                )
            }
            .sortedWith(compareByDescending<Entry> { it.isDir }.thenBy { it.name.lowercase() })
    }

    /** URI de partage d'un fichier du projet (via FileProvider). */
    fun fileUri(path: String): Uri? {
        if (!validatePath(path)) return null
        val root = getRootFolder() ?: return null
        val file = resolveExisting(root, path) ?: return null
        if (file.isDirectory) return null
        return try {
            FileProvider.getUriForFile(context, "${context.packageName}.updateprovider", file)
        } catch (e: Exception) {
            null
        }
    }

    /** Les fichiers texte sont lus dans l'aperçu intégré ; les autres passent par Android. */
    fun isTextFile(path: String): Boolean {
        val extension = path.substringAfterLast('.', "").lowercase()
        return extension in setOf(
            "txt", "md", "markdown", "kt", "kts", "java", "js", "jsx", "ts", "tsx",
            "json", "xml", "yaml", "yml", "toml", "gradle", "properties", "pro",
            "py", "sh", "html", "htm", "css", "scss", "sql", "csv", "log", "ini",
            "conf", "env", "gitignore", "dockerfile"
        )
    }

    fun mimeType(path: String): String {
        if (isTextFile(path)) return "text/plain"
        val extension = path.substringAfterLast('.', "").lowercase()
        return if (extension.isEmpty()) "application/octet-stream"
        else MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: "application/octet-stream"
    }

    fun folderName(): String {
        val root = getRootFolder() ?: return "Projet"
        return root.name.ifEmpty { "Projet" }
    }

    fun readFile(path: String): String {
        if (!validatePath(path)) return "Erreur: Chemin invalide (.. interdit)"
        val root = getRootFolder() ?: return "Erreur: Aucun dossier de travail sélectionné."
        val target = resolveExisting(root, path) ?: return "Erreur: Fichier introuvable: $path"
        if (target.isDirectory) return "Erreur: C'est un dossier, pas un fichier: $path"

        return try {
            target.inputStream().bufferedReader(Charsets.UTF_8).use { reader ->
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

    /** Lecture binaire d'une ressource du projet (aperçu web). Limite : 20 Mo. */
    fun readBytes(path: String): ByteArray? {
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
    fun readFileForEdit(path: String): Result<String> {
        if (!validatePath(path)) return Result.failure(IllegalArgumentException("Chemin invalide (.. interdit)"))
        val root = getRootFolder()
            ?: return Result.failure(IllegalStateException("Aucun dossier de travail sélectionné."))
        val target = resolveExisting(root, path)
            ?: return Result.failure(IllegalStateException("Fichier introuvable: $path"))
        if (target.isDirectory) return Result.failure(IllegalStateException("C'est un dossier: $path"))
        if (target.length() > 500_000L) {
            return Result.failure(IllegalStateException("Fichier trop volumineux pour l'éditeur (> 500 Ko)."))
        }
        return runCatching { target.readBytes().toString(Charsets.UTF_8) }
    }

    fun createFile(path: String): String {
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

    fun deletePath(path: String): String {
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

    fun renamePath(path: String, newName: String): String {
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

    fun makeDir(path: String): String {
        if (!validatePath(path)) return "Erreur: Chemin invalide (.. interdit)"
        val root = getRootFolder() ?: return "Erreur: Aucun dossier de travail sélectionné."
        val target = resolveInside(root, path) ?: return "Erreur: Chemin invalide: $path"
        if (target.isDirectory) return "Succès: Dossier créé $path"
        return if (target.mkdirs()) "Succès: Dossier créé $path" else "Erreur: Impossible de créer le dossier $path"
    }

    fun writeFile(path: String, content: String): String {
        if (!validatePath(path)) return "Erreur: Chemin invalide (.. interdit)"
        val root = getRootFolder() ?: return "Erreur: Aucun dossier de travail sélectionné."
        val target = resolveInside(root, path) ?: return "Erreur: Chemin invalide: $path"
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
