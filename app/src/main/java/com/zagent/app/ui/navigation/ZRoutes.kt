package com.zagent.app.ui.navigation

import android.net.Uri
import com.zagent.app.ui.viewmodel.AppScreen

/**
 * Routes typées du NavHost (MA71).
 *
 * Les écrans de premier niveau deviennent des destinations réelles de Navigation
 * Compose : chat, files, editor?path={path}, terminal, settings et
 * settings/appearance. Les feuilles du LOT 5 (historique, recherche, git,
 * modèles, partage…) restent des superpositions modales au-dessus du NavHost.
 *
 * Les chemins d'ouverture (éditeur) sont encodés avec Uri.encode pour supporter
 * les « / », espaces et accents, puis décodés automatiquement par le NavHost.
 */
object ZRoutes {
    const val CHAT = "chat"
    const val FILES = "files"
    const val EDITOR = "editor?path={path}"
    const val TERMINAL = "terminal"
    const val SETTINGS = "settings"
    const val SETTINGS_APPEARANCE = "settings/appearance"

    /** Schéma des deep links internes : zagent://chat, zagent://terminal… */
    const val DEEP_LINK_SCHEME = "zagent"

    /** Route d'un écran de premier niveau à partir de l'énumération existante. */
    fun of(screen: AppScreen): String = when (screen) {
        AppScreen.CHAT -> CHAT
        AppScreen.FILE_BROWSER -> FILES
        AppScreen.TERMINAL -> TERMINAL
        AppScreen.SETTINGS -> SETTINGS
        AppScreen.EDITOR -> editor(null)
    }

    /** Route de l'éditeur avec le chemin du fichier encodé (peut être vide). */
    fun editor(path: String?): String = "editor?path=" + Uri.encode(path ?: "")
}
