package com.zagent.app.data

/** Recherche de texte dans les fichiers du projet (outil search_in_files) + autocomplétion « @ » (MA53). */
object ProjectSearch {
    private val ignoredDirs = setOf(".git", "node_modules", ".godot", "build", ".gradle", ".idea")
    private const val MAX_RESULTS = 100
    private const val MAX_FILES = 400
    private const val MAX_FILE_BYTES = 500_000L

    fun search(repo: FileRepository, query: String, path: String, caseSensitive: Boolean): String {
        if (query.isEmpty()) return "Erreur: query vide."
        val results = ArrayList<String>()
        var scanned = 0
        var truncated = false

        fun walk(dir: String, depth: Int) {
            if (truncated || depth > 8) return
            for (e in repo.listEntries(dir)) {
                if (truncated) return
                if (e.isDir) {
                    if (e.name !in ignoredDirs) walk(e.path, depth + 1)
                    continue
                }
                if (!repo.isTextFile(e.path) || e.size > MAX_FILE_BYTES) continue
                if (scanned >= MAX_FILES) { truncated = true; return }
                scanned++
                val text = repo.readFileForEdit(e.path).getOrNull() ?: continue
                var n = 0
                for (line in text.lineSequence()) {
                    n++
                    if (line.contains(query, ignoreCase = !caseSensitive)) {
                        results.add(e.path + ":" + n + ": " + line.trim().take(200))
                        if (results.size >= MAX_RESULTS) { truncated = true; return }
                    }
                }
            }
        }

        walk(if (path.isBlank()) "." else path, 0)
        if (results.isEmpty()) return "Aucun résultat pour \"$query\" ($scanned fichiers lus)."
        val tail = if (truncated) "\n[Résultats tronqués : affine la recherche]" else ""
        return results.joinToString("\n") + tail
    }

    /**
     * Liste les fichiers du projet (chemins relatifs) pour l'autocomplétion des mentions « @ » (MA53).
     * Dossiers ignorés identiques à la recherche de texte ; profondeur limitée à 6 niveaux.
     */
    fun listProjectFiles(repo: FileRepository, maxFiles: Int = MAX_FILES): List<String> {
        val out = ArrayList<String>()
        fun walk(dir: String, depth: Int) {
            if (out.size >= maxFiles || depth > 6) return
            for (e in repo.listEntries(dir)) {
                if (out.size >= maxFiles) return
                if (e.isDir) {
                    if (e.name !in ignoredDirs) walk(e.path, depth + 1)
                } else {
                    out.add(e.path)
                }
            }
        }
        walk(".", 0)
        return out.sorted()
    }

    /**
     * Recherche floue simple dans une liste de chemins (MA53) :
     * 1) sous-chaîne directe (insensible à la casse), 2) sinon sous-séquence de caractères,
     * triée par chemin le plus court (le plus proche de la requête).
     */
    fun fuzzyFileMatches(files: List<String>, query: String, limit: Int = 8): List<String> {
        if (query.isBlank()) return files.take(limit)
        val q = query.lowercase().trim()
        val direct = files.filter { it.lowercase().contains(q) }
        if (direct.isNotEmpty()) return direct.take(limit)
        val scored = ArrayList<Pair<String, Int>>()
        for (f in files) {
            val lower = f.lowercase()
            var cursor = 0
            var ok = true
            for (ch in q) {
                cursor = lower.indexOf(ch, cursor)
                if (cursor < 0) { ok = false; break }
                cursor++
            }
            if (ok) scored.add(f to lower.length)
        }
        return scored.sortedBy { it.second }.map { it.first }.take(limit)
    }
}
