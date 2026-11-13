package com.zagent.app.data

/**
 * env-002 / session 16 — Logique pure de l'environnement « dépôt GitHub » :
 * extraction owner/repo d'une URL d'origine git, comptage des conflits depuis
 * `git status --porcelain`, lecture des compteurs avance/retard depuis
 * `git rev-list --count`. Tout est testé JVM sans réseau (EnvParseTest).
 */
object EnvParse {

    /**
     * Extrait « owner/repo » d'une URL d'origine git (https avec ou sans jeton,
     * ssh git@, protocole git://). Null si l'URL n'est pas un dépôt github.com.
     */
    fun depotDeOrigine(url: String): String? {
        val s = url.trim().removeSuffix("/").removeSuffix(".git")
        if (s.isBlank()) return null
        // ssh : git@github.com:owner/repo
        val ssh = Regex("^git@github\\.com:([^/]+)/([^/]+)$").find(s)
        if (ssh != null) {
            val (o, r) = ssh.destructured
            return "$o/$r"
        }
        // https://[jeton@]github.com/owner/repo[/...] — garde les deux premiers
        // segments après github.com, peu importe ce qui précède (jeton, user:pass).
        val idx = s.indexOf("github.com")
        if (idx < 0) return null
        val reste = s.substring(idx + "github.com".length).trimStart(':', '/')
        val segments = reste.split('/').filter { it.isNotBlank() }
        if (segments.size < 2) return null
        val owner = segments[0]
        val repo = segments[1].removeSuffix(".git")
        if (owner.isBlank() || repo.isBlank()) return null
        if (!Regex("^[A-Za-z0-9._-]+$").matches(owner) || !Regex("^[A-Za-z0-9._-]+$").matches(repo)) return null
        return "$owner/$repo"
    }

    /**
     * Compte les fichiers en conflit dans la sortie `git status --porcelain`.
     * Formats traités : porcelaine v1 (UU AA DD UA AU DU UD) et lignes v2
     * (« both modified: » etc. en cas de v2 verbeux).
     */
    fun compteConflits(statusPorcelain: String): Int {
        var n = 0
        for (ligne in statusPorcelain.lines()) {
            if (ligne.length < 2) continue
            when (ligne.substring(0, 2)) {
                "UU", "AA", "DD", "UA", "AU", "DU", "UD" -> n++
            }
        }
        return n
    }

    /**
     * Compte les fichiers modifiés (non commités) dans `git status --porcelain` :
     * toute ligne non vide hors conflits et hors ligne d'en-tête.
     */
    fun compteModifs(statusPorcelain: String): Int =
        statusPorcelain.lines().count { it.isNotBlank() && !it.startsWith("##") }

    /**
     * Lit un compteur « N\n » produit par `git rev-list --count …` ;
     * -1 si illisible (le code appelant dégrade le libellé au lieu de mentir).
     */
    fun compteRevList(sortie: String): Int =
        sortie.trim().toIntOrNull() ?: -1
}
