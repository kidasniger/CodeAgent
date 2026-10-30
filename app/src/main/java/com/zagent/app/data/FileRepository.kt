package com.zagent.app.data

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedReader
import java.io.InputStreamReader

class FileRepository(private val context: Context, private val rootUriString: String?) {

    private fun getRootFolder(): DocumentFile? {
        if (rootUriString.isNullOrEmpty()) return null
        val uri = Uri.parse(rootUriString)
        return DocumentFile.fromTreeUri(context, uri)
    }

    // txt -> text/plain, extensions connues -> leur type, sinon octet-stream.
    // Évite que le provider ajoute une extension (.txt) au nom du fichier.
    private fun mimeTypeFor(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        if (ext.isEmpty()) return "application/octet-stream"
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }

    private fun validatePath(path: String): Boolean {
        if (path.contains("..")) return false
        return true
    }

    private fun resolveDocument(root: DocumentFile, path: String): DocumentFile? {
        if (path == "." || path.isEmpty()) return root
        val cleanPath = path.removePrefix("./").removePrefix("/")
        val segments = cleanPath.split("/").filter { it.isNotEmpty() }
        var current: DocumentFile = root
        for (segment in segments) {
            if (segment == "..") return null
            val found = current.findFile(segment)
            if (found != null) {
                current = found
            } else {
                return null
            }
        }
        return current
    }

    private fun getOrCreateDocumentPath(root: DocumentFile, path: String, isDirectory: Boolean): DocumentFile? {
        val cleanPath = path.removePrefix("./").removePrefix("/")
        if (cleanPath.isEmpty() || cleanPath == ".") return root
        val segments = cleanPath.split("/").filter { it.isNotEmpty() }
        var current: DocumentFile = root
        for (i in segments.indices) {
            val segment = segments[i]
            if (segment == "..") return null
            var found = current.findFile(segment)
            if (found == null) {
                found = if (i == segments.size - 1 && !isDirectory) {
                    current.createFile(mimeTypeFor(segment), segment)
                } else {
                    current.createDirectory(segment)
                }
            }
            if (found == null) return null
            current = found
        }
        return current
    }

    fun listDir(path: String = "."): String {
        if (!validatePath(path)) return "Erreur: Chemin invalide (.. interdit)"
        val root = getRootFolder() ?: return "Erreur: Aucun dossier de travail sélectionné."
        val target = resolveDocument(root, path) ?: return "Erreur: Dossier introuvable: $path"
        if (!target.isDirectory) return "Erreur: Ce n'est pas un dossier: $path"

        val ignoredDirs = setOf(".git", "node_modules", ".godot", "build")
        val resultList = mutableListOf<String>()
        var count = 0

        fun recursiveList(dir: DocumentFile, currentRelPath: String, depth: Int) {
            if (depth > 4 || count >= 300) return
            val files = dir.listFiles()
            for (file in files) {
                if (count >= 300) break
                val name = file.name ?: continue
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

        recursiveList(target, if (path == "." || path.isEmpty()) "" else path, 1)
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
        val target = resolveDocument(root, path) ?: return emptyList()
        if (!target.isDirectory) return emptyList()
        val base = if (path == "." || path.isEmpty() || path == "/") "" else path.removePrefix("./").removePrefix("/")
        return target.listFiles()
            .mapNotNull { f ->
                val name = f.name ?: return@mapNotNull null
                Entry(
                    name = name,
                    isDir = f.isDirectory,
                    size = f.length(),
                    path = if (base.isEmpty()) name else "$base/$name"
                )
            }
            .sortedWith(compareByDescending<Entry> { it.isDir }.thenBy { it.name.lowercase() })
    }

    fun folderName(): String {
        val root = getRootFolder() ?: return "Projet"
        return root.name ?: "Projet"
    }

    fun readFile(path: String): String {
        if (!validatePath(path)) return "Erreur: Chemin invalide (.. interdit)"
        val root = getRootFolder() ?: return "Erreur: Aucun dossier de travail sélectionné."
        val target = resolveDocument(root, path) ?: return "Erreur: Fichier introuvable: $path"
        if (target.isDirectory) return "Erreur: C'est un dossier, pas un fichier: $path"

        try {
            val uri = target.uri
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
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
                    return sb.toString()
                }
            } ?: return "Erreur: Impossible d'ouvrir le flux pour $path"
        } catch (e: Exception) {
            return "Erreur lors de la lecture (fichier binaire ou corrompu ?): ${e.localizedMessage}"
        }
    }

    fun makeDir(path: String): String {
        if (!validatePath(path)) return "Erreur: Chemin invalide (.. interdit)"
        val root = getRootFolder() ?: return "Erreur: Aucun dossier de travail sélectionné."
        val created = getOrCreateDocumentPath(root, path, true)
        return if (created != null) "Succès: Dossier créé $path" else "Erreur: Impossible de créer le dossier $path"
    }

    fun writeFile(path: String, content: String): String {
        if (!validatePath(path)) return "Erreur: Chemin invalide (.. interdit)"
        val root = getRootFolder() ?: return "Erreur: Aucun dossier de travail sélectionné."
        
        val lastSlash = path.lastIndexOf('/')
        if (lastSlash != -1) {
            val parentPath = path.substring(0, lastSlash)
            getOrCreateDocumentPath(root, parentPath, true)
        }

        val target = getOrCreateDocumentPath(root, path, false) ?: return "Erreur: Impossible de créer/ouvrir le fichier $path"
        try {
            context.contentResolver.openOutputStream(target.uri, "wt")?.use { outputStream ->
                outputStream.write(content.toByteArray(Charsets.UTF_8))
                outputStream.flush()
                return "Succès: Fichier écrit $path"
            } ?: return "Erreur: Impossible d'ouvrir le flux d'écriture pour $path"
        } catch (e: Exception) {
            return "Erreur d'écriture: ${e.localizedMessage}"
        }
    }
}
