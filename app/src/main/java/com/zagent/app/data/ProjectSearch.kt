package com.zagent.app.data

/** Recherche de texte dans les fichiers du projet (outil search_in_files). */
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
}
