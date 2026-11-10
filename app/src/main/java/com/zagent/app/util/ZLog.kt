package com.zagent.app.util

import android.util.Log
import com.zagent.app.BuildConfig
import com.zagent.app.data.Fournisseurs

/**
 * corr-019 — Wrapper de journalisation : masque les clés API et coupe les logs
 * verbeux en release.
 *
 * Règles :
 * - En release (`BuildConfig.DEBUG == false`), [d] et [v] n'émettent rien ;
 *   seul [e] (erreurs) passe, sans jamais inclure de secret.
 * - Toute chaîne susceptible de contenir une clé API, un jeton ou une URL
 *   signée passe d'abord dans [masquer] : chaque clé connue des réglages est
 *   remplacée par `«••••»`, et toute séquence `Bearer <jeton>` est neutralisée.
 * - Les clés sont reçues de [Fournisseurs] via [rafraichirSecrets] : le
 *   wrapper ne lit jamais le stockage lui-même (pas de dépendance à
 *   SettingsManager, utilisable partout).
 *
 * Usage : `ZLog.d(TAG, "réponse ${ZLog.masquer(données)}")`.
 */
object ZLog {

    @Volatile
    private var secrets: Set<String> = emptySet()

    /** Alimente la liste des secrets à masquer (clés API des fournisseurs). */
    fun rafraichirSecrets(cles: Collection<String>) {
        secrets = cles.filter { it.isNotBlank() }.toSet()
    }

    /**
     * corr-021 — Remplace tout secret connu par «••••» et neutralise les
     * en-têtes d'authentification éventuels dans le texte donné.
     */
    fun masquer(texte: String?): String {
        if (texte == null) return "null"
        var out = texte
        for (s in secrets) {
            if (s.length >= 8) out = out.replace(s, "••••")
        }
        // Authorization: Bearer xyz → Authorization: Bearer ••••
        out = out.replace(Regex("(?i)(authorization\\s*[:=]\\s*bearer\\s*)\\S+"), "$1••••")
        // jetons style sk-… / pat_… / github_pat_… éventuellement imprimés
        out = out.replace(Regex("(?i)\\b(sk-[A-Za-z0-9_-]{8,}|pat_[A-Za-z0-9_-]{8,})\\b"), "••••")
        return out
    }

    /** Journal de debug : ignoré en release. */
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(tag, masquer(message))
    }

    /** Journal verbeux : ignoré en release. */
    fun v(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.v(tag, masquer(message))
    }

    /** Information : émise en debug uniquement (pas de bruit en production). */
    fun i(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.i(tag, masquer(message))
    }

    /** Erreur : émise partout, mais toujours masquée. */
    fun e(tag: String, message: String, erreur: Throwable? = null) {
        Log.e(tag, masquer(message), erreur)
    }
}
