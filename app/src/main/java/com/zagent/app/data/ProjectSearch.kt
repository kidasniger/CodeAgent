package com.zagent.app.data

/**
 * feat-080 — Filtre .gitignore simplifié mais fidèle aux usages courants :
 * commentaires `#`, négations `!`, `*`, `?`, `**`, ancres `/` et règles de
 * dossier (motif terminé par `/`). Les .gitignore sont lus à la racine ET dans
 * chaque sous-dossier parcouru (règle git : un .gitignore s'applique à son
 * dossier et à ses descendants).
 */
class GitignoreMatcher {
    private data class Rule(val negation: Boolean, val dirOnly: Boolean, val regex: Regex)

    /** Règles par dossier d'ancrage (chemin relatif, "" = racine). */
    private val parDossier = HashMap<String, List<Rule>>()

    /** Charge les .gitignore trouvés depuis la racine (lecture une seule fois). */
    fun charger(repo: FileRepository) {
        parDossier.clear()
        parDossier[""] = parser(repo.readFile(".gitignore"))
    }

    private fun parser(contenu: String): List<Rule> {
        val out = ArrayList<Rule>()
        for (ligne in contenu.lines()) {
            val brute = ligne.trim()
            if (brute.isEmpty() || brute.startsWith("#")) continue
            var motif = brute
            val negation = motif.startsWith("!")
            if (negation) motif = motif.substring(1)
            val dirOnly = motif.endsWith("/")
            if (dirOnly) motif = motif.trimEnd('/')
            // Une ligne sans « / » interne s'applique à tous les niveaux.
            val sansChemin = !motif.contains('/')
            var regex = builderGlob(motif)
            if (sansChemin) regex = "(?:.*/)?${regex}"
            else if (!motif.startsWith("/")) regex = "(?:.*/)?${regex}"
            else regex = regex.removePrefix("/")
            out.add(Rule(negation, dirOnly, Regex(regex)))
        }
        return out
    }

    /** Convertit un motif gitignore (déjà débarrassé de !/final) en regex. */
    private fun builderGlob(motif: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < motif.length) {
            val ch = motif[i]
            when {
                ch == '*' && i + 1 < motif.length && motif[i + 1] == '*' -> {
                    sb.append(".*"); i += 2
                    if (i < motif.length && motif[i] == '/') i++
                }
                ch == '*' -> sb.append("[^/]*")
                ch == '?' -> sb.append("[^/]")
                ch == '.' || ch == '(' || ch == ')' || ch == '+' || ch == '{' ||
                    ch == '}' || ch == '[' || ch == ']' || ch == '^' || ch == '$' ||
                    ch == '|' || ch == '\\' -> {
                    sb.append('\\').append(ch)
                }
                else -> sb.append(ch)
            }
            i++
        }
        return sb.toString()
    }

    /** Charge un .gitignore de sous-dossier (appelé par le parcours à la volée). */
    fun chargerDossier(repo: FileRepository, dossier: String) {
        if (parDossier.containsKey(dossier)) return
        val contenu = repo.readFile(if (dossier == ".") ".gitignore" else "$dossier/.gitignore")
        if (!contenu.startsWith("Erreur") && contenu.isNotBlank()) {
            parDossier[dossier] = parser(contenu)
        } else {
            parDossier[dossier] = emptyList()
        }
    }

    /**
     * Vrai si le chemin (fichier ou dossier, relatif au projet) est ignoré.
     * La dernière règle qui matche gagne (sémantique git).
     */
    fun estIgnore(chemin: String, estDossier: Boolean): Boolean {
        var ignore = false
        // Règles racine puis règles des dossiers ancêtres (le plus proche gagne par ordre d'application).
        val dossier = chemin.substringBeforeLast('/', "").ifEmpty { "" }
        val ancetres = ArrayList<String>()
        var acc = ""
        for (part in dossier.split('/').filter { it.isNotEmpty() }) {
            acc = if (acc.isEmpty()) part else "$acc/$part"
            ancetres.add(acc)
        }
        val ordre = listOf("") + ancetres
        for (ancre in ordre) {
            for (rule in parDossier[ancre].orEmpty()) {
                if (rule.dirOnly && !estDossier) continue
                val relatif = if (ancre.isEmpty()) chemin
                else chemin.removePrefix("$ancre/").ifEmpty { chemin }
                if (rule.regex.matches(relatif)) ignore = !rule.negation
            }
        }
        return ignore
    }
}

/** Recherche de texte dans les fichiers du projet (outil search_in_files) + autocomplétion « @ » (MA53). */
object ProjectSearch {

    /** Un résultat de recherche de code structuré (MA58) : fichier, ligne, extrait. */
    data class CodeHit(val path: String, val line: Int, val text: String)

    private val ignoredDirs = setOf(".git", "node_modules", ".godot", "build", ".gradle", ".idea")
    private const val MAX_RESULTS = 100
    private const val MAX_FILES = 400
    private const val MAX_FILE_BYTES = 500_000L

    /** feat-080 — Charge le .gitignore racine (retourne un matcher, jamais null). */
    fun gitignore(repo: FileRepository): GitignoreMatcher =
        GitignoreMatcher().also { it.charger(repo) }

    /**
     * corr-058 — [annule] est sondé à chaque entrée de dossier et chaque fichier :
     * une recherche lancée par l'agent peut être stoppée sans balayer tout le
     * projet. Exécution attendue sur Dispatchers.IO (côté appelant).
     */
    fun search(
        repo: FileRepository,
        query: String,
        path: String,
        caseSensitive: Boolean,
        annule: () -> Boolean = { false }
    ): String {
        if (query.isEmpty()) return "Erreur: query vide."
        val results = ArrayList<String>()
        var scanned = 0
        var truncated = false
        // feat-080 : la recherche respecte le .gitignore du projet.
        val gi = gitignore(repo)

        fun walk(dir: String, depth: Int) {
            if (annule()) { truncated = true; return }
            if (truncated || depth > 8) return
            gi.chargerDossier(repo, dir)
            for (e in repo.listEntries(dir)) {
                if (truncated) return
                if (e.isDir) {
                    if (e.name !in ignoredDirs && !gi.estIgnore(e.path, true)) walk(e.path, depth + 1)
                    continue
                }
                if (annule()) { truncated = true; return }
                if (gi.estIgnore(e.path, false)) continue
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
     * feat-080 : respecte aussi le .gitignore (racine + sous-dossiers).
     */
    fun listProjectFiles(repo: FileRepository, maxFiles: Int = MAX_FILES): List<String> {
        val out = ArrayList<String>()
        val gi = gitignore(repo)
        fun walk(dir: String, depth: Int) {
            if (out.size >= maxFiles || depth > 6) return
            gi.chargerDossier(repo, dir)
            for (e in repo.listEntries(dir)) {
                if (out.size >= maxFiles) return
                if (e.isDir) {
                    if (e.name !in ignoredDirs && !gi.estIgnore(e.path, true)) walk(e.path, depth + 1)
                } else if (!gi.estIgnore(e.path, false)) {
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

    /**
     * Recherche de code structurée pour l'écran Recherche globale (MA58) :
     * renvoie des [CodeHit] (chemin, ligne, extrait), même parcours que [search].
     */
    fun codeSearch(
        repo: FileRepository,
        query: String,
        maxHits: Int = 120,
        annule: () -> Boolean = { false }
    ): List<CodeHit> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val results = ArrayList<CodeHit>()
        var scanned = 0
        var truncated = false
        // feat-080 : l'écran Recherche respecte aussi le .gitignore.
        val gi = gitignore(repo)

        fun walk(dir: String, depth: Int) {
            if (annule()) { truncated = true; return }
            if (truncated || depth > 8) return
            gi.chargerDossier(repo, dir)
            for (e in repo.listEntries(dir)) {
                if (truncated) return
                if (e.isDir) {
                    if (e.name !in ignoredDirs && !gi.estIgnore(e.path, true)) walk(e.path, depth + 1)
                    continue
                }
                if (annule()) { truncated = true; return }
                if (gi.estIgnore(e.path, false)) continue
                if (!repo.isTextFile(e.path) || e.size > MAX_FILE_BYTES) continue
                if (scanned >= MAX_FILES) { truncated = true; return }
                scanned++
                val text = repo.readFileForEdit(e.path).getOrNull() ?: continue
                var n = 0
                for (line in text.lineSequence()) {
                    n++
                    if (line.contains(q, ignoreCase = true)) {
                        results.add(CodeHit(e.path, n, line.trim().take(200)))
                        if (results.size >= maxHits) { truncated = true; return }
                    }
                }
            }
        }

        walk(".", 0)
        return results
    }
}
