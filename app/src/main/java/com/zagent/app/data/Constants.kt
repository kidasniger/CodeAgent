package com.zagent.app.data

/**
 * corr-015 — Constantes transverses de l'application : délais réseau, tailles
 * de tampons, limites et plages de réglage, jusqu'ici dispersées en valeurs
 * littérales dans les ViewModel et les dépôts.
 *
 * Règle d'usage : toute nouvelle limite se déclare ici avec son commentaire ;
 * les valeurs existantes sont remplacées progressivement par les fichiers
 * touchés (règle corr-013 : pas de renommage en masse sans prompt dédié).
 */
object Constants {

    // ── Réseau (AiRepository) ──────────────────────────────────────────────

    /** Délai (s) de connexion/lecture/écriture des appels chat/completions. */
    const val RESEAU_TIMEOUT_S = 180

    /** corr-032 — Plafond absolu (s) du timeout d'une commande, quelle que soit la valeur demandée par le modèle. */
    const val TEMPS_MAX_COMMANDE_S = 600

    // ── Limites de contenu envoyé au modèle ────────────────────────────────

    /** Longueur maximale d'une sortie de commande tronquée avant envoi (MA33…). */
    const val SORTIE_COMMANDE_MAX = 1500

    /** Filet final d'une sortie tronquée (fin conservée : l'essentiel y est). */
    const val SORTIE_COMMANDE_QUEUE = 1000

    /** Longueur maximale d'un message archivé dans une conversation (MA56). */
    const val MESSAGE_ARCHIVE_MAX = 200_000

    // ── Tampons journal ────────────────────────────────────────────────────

    /** Journal de l'onglet Agent du terminal : 60 000 derniers caractères. */
    const val JOURNAL_AGENT_MAX = 60_000

    /** Copie « tout sélectionner » du terminal : 50 000 derniers caractères. */
    const val TERMINAL_COPIE_MAX = 50_000

    // ── Usage et activité ──────────────────────────────────────────────────

    /** Nombre d'entrées conservées par UsageRepository (MA60). */
    const val USAGE_ENTREES_MAX = 2000

    // ── Plages des réglages ────────────────────────────────────────────────

    /** Échelle de texte de l'interface (MàJ 97) : 85 % à 130 %. */
    const val FONT_SCALE_MIN = 0.85f
    const val FONT_SCALE_MAX = 1.30f

    /** Échelle de densité : 88 % à 112 %. */
    const val DENSITE_MIN = 0.88f
    const val DENSITE_MAX = 1.12f

    /** Taille de police éditeur/terminal (MA83) : 10 à 22 sp. */
    const val POLICE_EDITOR_MIN = 10
    const val POLICE_EDITOR_MAX = 22
    const val POLICE_TERMINAL_MIN = 10
    const val POLICE_TERMINAL_MAX = 22

    /** Budget d'étapes de l'agent : 5 à 100 (défaut dans SettingsManager). */
    const val ETAPES_MIN = 5
    const val ETAPES_MAX = 100
}
