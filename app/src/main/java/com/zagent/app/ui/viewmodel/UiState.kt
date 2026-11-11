package com.zagent.app.ui.viewmodel

/**
 * corr-008 — États d'UI regroupés par écran (data class UiState).
 *
 * Objectif du prompt : remplacer les nombreux `mutableStateOf` isolés par une
 * data class immuable par écran — une seule source de vérité, des copies
 * explicites (`copy(...)`) au lieu d'écritures dispersées, et moins d'états
 * incohérents. Adoption progressive, sans changer l'API publique de
 * MainViewModel (les anciennes propriétés délèguent vers le champ `ui`).
 *
 * Les collections observables (browserChildren, expandedDirs) restent des
 * SnapshotStateMap séparées : les regrouper dans une data class immuable
 * casserait la granularité de recomposition par entrée.
 */

/** Aperçu d'un fichier lu depuis l'explorateur (lecture seule). */
data class BrowserFilePreview(val path: String, val content: String)

/** État d'UI de l'explorateur de fichiers (corr-008). */
data class FileBrowserUiState(
    /** Fichier ouvert en aperçu (lecture seule), null si aucun. */
    val preview: BrowserFilePreview? = null,
    /** Dernière erreur affichée par le navigateur (snackbar), null sinon. */
    val error: String? = null,
    /** Nombre de fichiers du projet (hors dossiers). */
    val count: Int = 0,
    /** Vrai pendant un chargement de la racine. */
    val loading: Boolean = false
)

/** Changements du dépôt Git : sortie de `git status`, diff consolidé, erreur éventuelle. */
data class GitChanges(val status: String, val diff: String, val error: String? = null)

/**
 * État d'UI de l'écran Git (corr-008) : adoption effective — MainViewModel ne
 * expose plus que [MainViewModel.gitUi] ; les anciens champs dispersés
 * (gitSheetOpen, gitChanges, gitLoading) ont été regroupés ici.
 */
data class GitUiState(
    /** Feuille / écran Git ouvert. */
    val sheetOpen: Boolean = false,
    /** Chargement en cours (lecture de status + diff). */
    val loading: Boolean = false,
    /** Dernier résultat consolidé, null tant que rien n'a été lu. */
    val changes: GitChanges? = null
)
