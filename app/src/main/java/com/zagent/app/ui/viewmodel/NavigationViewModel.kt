package com.zagent.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.zagent.app.ui.navigation.ZRoutes
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * corr-009 — Événement de navigation ponctuel. Remplace les anciens drapeaux
 * « navRouteRequest : String? » et « navBackRequest : Int » : un événement est
 * émis une seule fois et consommé par le collecteur de ZNavHost, sans risque
 * de re-déclenchement lors d'une recomposition ou d'un changement de
 * configuration (un LaunchedEffect sur un état persistant rejouait la route).
 */
sealed interface NavEvent {
    /** Ouvre la route demandée (ex. « terminal », « editor?path=src/Main.kt »). */
    data class Route(val route: String) : NavEvent

    /** Revient en arrière (pop du back stack). */
    data object Back : NavEvent
}

/**
 * corr-003 — La navigation quitte MainViewModel.
 *
 * Source de vérité du back stack : le NavHost Compose de ZNavHost.kt. Les
 * écrans continuent d'appeler navigateTo / navigateBack (API inchangée) ;
 * les demandes sont émises comme [NavEvent] que ZNavHost exécute.
 * [currentScreen] est resynchronisé depuis la route réellement affichée via
 * [syncScreenFromRoute], ce qui couvre aussi les deep links et les raccourcis.
 *
 * Aucune dépendance à Application : le chemin d'éditeur par défaut est fourni
 * par le lambda [editorPathProvider] (résolu au moment de la demande).
 */
class NavigationViewModel(
    private val editorPathProvider: () -> String? = { null }
) : ViewModel() {

    /** Flux d'événements de navigation ; ZNavHost est l'unique collecteur. */
    private val _events = MutableSharedFlow<NavEvent>(
        replay = 1,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<NavEvent> = _events

    /**
     * Consomme l'événement rejoué : appelé par ZNavHost après exécution, pour
     * qu'un changement de configuration ne rejoue pas une navigation déjà faite.
     */
    fun consumeReplayedEvent() {
        _events.resetReplayCache()
    }

    private val navigationHistory = mutableStateListOf<AppScreen>()

    var currentScreen by mutableStateOf(AppScreen.CHAT)
        private set

    fun navigateTo(screen: AppScreen, editorPath: String? = null) {
        if (currentScreen == screen) return
        navigationHistory.add(currentScreen)
        val route = if (screen == AppScreen.EDITOR) {
            ZRoutes.editor(editorPath ?: editorPathProvider())
        } else {
            ZRoutes.of(screen)
        }
        _events.tryEmit(NavEvent.Route(route))
    }

    /** corr-002 (raccourcis) — demande directe de route (zagent://chat, etc.). */
    fun requestRoute(route: String) {
        _events.tryEmit(NavEvent.Route(route))
    }

    fun navigationPreviousScreen(): AppScreen =
        navigationHistory.lastOrNull() ?: AppScreen.CHAT

    fun navigateBack(): Boolean {
        _events.tryEmit(NavEvent.Back)
        return true
    }

    /** Écriture directe de l'écran courant (compatibilité de l'ancien setter). */
    fun syncScreenFromRouteForce(screen: AppScreen) {
        currentScreen = screen
    }

    /**
     * Resynchronise l'état depuis la route réellement affichée par le NavHost
     * (y compris après un retour système, un deep link ou un raccourci).
     */
    fun syncScreenFromRoute(route: String?) {
        val screen = when {
            route == null -> AppScreen.CHAT
            route.startsWith("editor") -> AppScreen.EDITOR
            route.startsWith("files") -> AppScreen.FILE_BROWSER
            route.startsWith("terminal") -> AppScreen.TERMINAL
            route.startsWith("settings") -> AppScreen.SETTINGS
            else -> AppScreen.CHAT
        }
        if (currentScreen != screen) {
            currentScreen = screen
            navigationHistory.clear()
        }
    }
}
