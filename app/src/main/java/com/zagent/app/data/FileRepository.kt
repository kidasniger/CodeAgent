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

    /** feat-070 — Images affichables dans la visionneuse intégrée (svg : non, via appli externe). */
    fun isImagePath(path: String): Boolean {
        val extension = path.substringAfterLast('.', "").lowercase()
        return extension in setOf("png", "jpg", "jpeg", "webp", "gif", "bmp")
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

    // ── feat-071/072/073 : déplacer, zipper, dézipper ─────────────────────────

    /**
     * feat-071 — Déplace un fichier ou un dossier vers [destDir] (dossier du
     * projet). Refuse de déplacer un dossier dans lui-même ou un de ses enfants.
     */
    fun movePath(path: String, destDir: String): String {
        if (!validatePath(path) || !validatePath(destDir)) return "Erreur: Chemin invalide (.. interdit)"
        val root = getRootFolder() ?: return "Erreur: Aucun dossier de travail sélectionné."
        val source = resolveExisting(root, path) ?: return "Erreur: Introuvable: $path"
        val dest = resolveInside(root, destDir) ?: return "Erreur: Dossier de destination invalide: $destDir"
        if (!dest.isDirectory) return "Erreur: La destination n'est pas un dossier: $destDir"
        if (source.canonicalPath == dest.canonicalPath) return "Erreur: Source et destination identiques."
        if (dest.canonicalPath.startsWith(source.canonicalPath + "/")) {
            return "Erreur: Impossible de déplacer un dossier dans lui-même."
        }
        val cible = File(dest, source.name)
        if (cible.exists()) return "Erreur: Existe déjà: $destDir/${source.name}"
        return try {
            if (source.renameTo(cible)) "Succès: Déplacé vers $destDir/${source.name}"
            else {
                // Repli multi-volumes : copie récursive puis suppression.
                if (source.isDirectory) {
                    source.copyRecursively(cible) && source.deleteRecursively()
                } else {
                    source.copyTo(cible).delete()
                }.let { ok -> if (ok) "Succès: Déplacé vers $destDir/${source.name}" else "Erreur: Déplacement impossible" }
            }
        } catch (e: Exception) {
            "Erreur: ${e.localizedMessage}"
        }
    }

    /**
     * feat-071/073 — Compresse les chemins donnés (fichiers ou dossiers) dans
     * une archive .zip à la racine du projet. Retour « Succès: … » / « Erreur: … ».
     */
    fun zipPaths(paths: List<String>, zipName: String): String {
        if (paths.isEmpty()) return "Erreur: Aucun fichier sélectionné."
        val root = getRootFolder() ?: return "Erreur: Aucun dossier de travail sélectionné."
        val nom = if (zipName.endsWith(".zip")) zipName else "$zipName.zip"
        if (!nom.matches(Regex("[A-Za-z0-9._ ()-]+")) || nom.contains("..")) return "Erreur: Nom d'archive invalide."
        val dest = resolveInside(root, nom) ?: return "Erreur: Chemin d'archive invalide."
        if (dest.exists()) return "Erreur: Existe déjà: $nom"
        return try {
            java.util.zip.ZipOutputStream(dest.outputStream().buffered()).use { zip ->
                for (p in paths) {
                    val f = resolveExisting(root, p) ?: continue
                    val base = f.parentFile?.canonicalPath ?: root.canonicalPath
                    f.walkTopDown().forEach { item ->
                        val rel = item.canonicalPath.removePrefix(base).trimStart('/')
                            .ifEmpty { item.name }
                        if (item.isDirectory) {
                            zip.putNextEntry(java.util.zip.ZipEntry("$rel/"))
                            zip.closeEntry()
                        } else {
                            zip.putNextEntry(java.util.zip.ZipEntry(rel))
                            item.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }
            }
            "Succès: Archive créée $nom"
        } catch (e: Exception) {
            dest.delete()
            "Erreur: Compression impossible (${e.localizedMessage})"
        }
    }

    /**
     * feat-073 — Extrait une archive .zip du projet dans [destDir] (défaut : racine).
     * Protection Zip Slip : chaque entrée doit rester à l'intérieur de la destination.
     */
    fun unzipFile(zipPath: String, destDir: String = "."): String {
        if (!validatePath(zipPath) || !validatePath(destDir)) return "Erreur: Chemin invalide (.. interdit)"
        val root = getRootFolder() ?: return "Erreur: Aucun dossier de travail sélectionné."
        val zip = resolveExisting(root, zipPath) ?: return "Erreur: Archive introuvable: $zipPath"
        val dest = resolveInside(root, destDir) ?: return "Erreur: Destination invalide: $destDir"
        return try {
            var count = 0
            java.util.zip.ZipInputStream(zip.inputStream().buffered()).use { stream ->
                while (true) {
                    val entry = stream.nextEntry ?: break
                    val out = File(dest, entry.name)
                    // Zip Slip : le chemin canonique doit rester dans la destination.
                    if (!out.canonicalPath.startsWith(dest.canonicalPath + File.separator) &&
                        out.canonicalPath != dest.canonicalPath
                    ) {
                        return "Erreur: Archive suspecte (Zip Slip) : ${entry.name}"
                    }
                    if (entry.isDirectory) {
                        out.mkdirs()
                    } else {
                        out.parentFile?.mkdirs()
                        out.outputStream().use { stream.copyTo(it) }
                        count++
                    }
                    stream.closeEntry()
                }
            }
            "Succès: $count fichier(s) extraits vers $destDir"
        } catch (e: java.util.zip.ZipException) {
            "Erreur: Archive corrompue (${e.localizedMessage})"
        } catch (e: Exception) {
            "Erreur: Extraction impossible (${e.localizedMessage})"
        }
    }

    // ── feat-075 : export du projet en zip ────────────────────────────────────

    /**
     * feat-075 — Exporte le projet entier dans un zip du cache de l'application
     * (partageable via FileProvider). Renvoie null si aucun projet ou erreur.
     */
    fun exportProjectZip(): File? {
        val root = getRootFolder() ?: return null
        if (context == null) return null
        return try {
            val out = File(context.cacheDir, "exports/${root.name.ifEmpty { "projet" }}.zip")
            out.parentFile?.mkdirs()
            if (out.exists()) out.delete()
            java.util.zip.ZipOutputStream(out.outputStream().buffered()).use { zip ->
                root.walkTopDown().forEach { item ->
                    if (item.isFile) {
                        val rel = item.relativeTo(root).invariantSeparatorsPath
                        zip.putNextEntry(java.util.zip.ZipEntry(rel))
                        item.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
            out
        } catch (e: Exception) {
            null
        }
    }

    // ── feat-079 : corbeille (7 jours, suppression récupérable) ──────────────

    /** Dossier corbeille, dans les fichiers privés de l'application. */
    private fun trashDir(): File {
        val d = File(context.filesDir, "trash").apply { mkdirs() }
        // Purge automatique : les entrées de plus de 7 jours sont supprimées.
        val seuil = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        d.listFiles()?.forEach { f ->
            val ts = f.name.substringBefore("__").toLongOrNull() ?: 0L
            if (ts in 1 until seuil) f.deleteRecursively()
        }
        return d
    }

    /**
     * feat-079 — Déplace le chemin vers la corbeille au lieu de le supprimer.
     * L'entrée est préfixée par un horodatage pour restaurer le nom d'origine.
     */
    fun trashPath(path: String): String {
        if (!validatePath(path)) return "Erreur: Chemin invalide"
        if (path == "." || path.isEmpty() || path == "/") return "Erreur: Impossible de mettre la racine à la corbeille."
        val root = getRootFolder() ?: return "Erreur: Aucun dossier de travail sélectionné."
        val target = resolveExisting(root, path) ?: return "Erreur: Introuvable: $path"
        return try {
            val stamp = System.currentTimeMillis()
            val entry = File(trashDir(), "${stamp}__${target.name}")
            if (target.renameTo(entry)) "Succès: Mis à la corbeille (${target.name})"
            else deletePath(path) // repli : suppression directe si le FS refuse
        } catch (e: Exception) {
            "Erreur: ${e.localizedMessage}"
        }
    }

    /** feat-079 — Entrées de la corbeille : (id horodaté, nom d'origine, taille, date). */
    data class TrashEntry(val id: String, val name: String, val bytes: Long, val trashedAt: Long)

    fun trashEntries(): List<TrashEntry> {
        val d = trashDir()
        return d.listFiles()?.mapNotNull { f ->
            val stamp = f.name.substringBefore("__").toLongOrNull() ?: return@mapNotNull null
            val name = f.name.substringAfter("__")
            TrashEntry(f.name, name, f.walkTopDown().filter { it.isFile }.sumOf { it.length() }, stamp)
        }?.sortedByDescending { it.trashedAt } ?: emptyList()
    }

    /** feat-079 — Restaure une entrée de corbeille à la racine du projet. */
    fun restoreFromTrash(entryId: String): String {
        val root = getRootFolder() ?: return "Erreur: Aucun dossier de travail sélectionné."
        val entry = File(trashDir(), entryId)
        if (!entry.exists()) return "Erreur: Entrée de corbeille introuvable."
        val name = entryId.substringAfter("__")
        val dest = resolveInside(root, name) ?: return "Erreur: Nom de restauration invalide."
        if (dest.exists()) return "Erreur: Un fichier nommé $name existe déjà à la racine."
        return try {
            if (entry.renameTo(dest)) "Succès: Restauré $name" else "Erreur: Restauration impossible"
        } catch (e: Exception) {
            "Erreur: ${e.localizedMessage}"
        }
    }

    /** feat-079 — Vide la corbeille définitivement. */
    fun emptyTrash(): String {
        val d = trashDir()
        var n = 0
        d.listFiles()?.forEach { f -> if (f.deleteRecursively()) n++ }
        return "Succès: Corbeille vidée ($n entrée(s))."
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

    /** feat-074 — Écriture binaire (import d'archives, images…) avec verrou identique à writeFile. */
    fun writeBytes(path: String, content: ByteArray): String {
        if (!validatePath(path)) return "Erreur: Chemin invalide (.. interdit)"
        val root = getRootFolder() ?: return "Erreur: Aucun dossier de travail sélectionné."
        val target = resolveInside(root, path) ?: return "Erreur: Chemin invalide: $path"
        if (target.isDirectory) return "Erreur: C'est un dossier: $path"
        return synchronized(lockFor(target.canonicalPath)) {
            try {
                target.parentFile?.mkdirs()
                target.writeBytes(content)
                "Succès: Fichier écrit $path"
            } catch (e: Exception) {
                "Erreur d'écriture: ${e.localizedMessage}"
            }
        }
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
