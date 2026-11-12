package com.zagent.app.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.zagent.app.ui.layout.AdaptiveChatPane
import com.zagent.app.ui.layout.AdaptiveEditorPane
import com.zagent.app.ui.screens.FileBrowserScreen
import com.zagent.app.ui.screens.SettingsScreen
import com.zagent.app.ui.screens.TerminalScreen
import com.zagent.app.ui.theme.LocalReduceMotion
import com.zagent.app.ui.viewmodel.MainViewModel
import com.zagent.app.ui.viewmodel.NavEvent

/**
 * Hôte de navigation unique de l'application (MA71 + MA72).
 *
 * MA71 — Le NavHost remplace les booléens isSettingsOpen / isTerminalOpen /
 * isEditorOpen / isFileBrowserOpen : chaque écran de premier niveau possède une
 * route typée et le back stack est géré par Navigation Compose. Le MainViewModel
 * reste la façade d'API (navigateTo / navigateBack / currentScreen) ; les
 * requêtes sont exécutées ici puis l'état est resynchronisé depuis la route
 * réelle, ce qui couvre aussi les deep links (zagent://…) et la restauration
 * d'état au changement de configuration.
 *
 * MA72 — Transitions : fondu + léger glissement horizontal de 24 dp en 250 ms
 * (easing « emphasizedDecelerate ») pour les écrans de premier niveau,
 * glissement du bas pour les réglages. Toutes les animations sont désactivées
 * quand « Réduire les animations » est actif (Réglages › Apparence).
 *
 * Les feuilles du LOT 5 (historique, recherche, git, partage, modèles…) restent
 * des superpositions gérées par MainActivity, avec leur propre glissement du bas.
 */
@Composable
fun ZNavHost(
    viewModel: MainViewModel,
    widthSizeClass: WindowWidthSizeClass,
    landscape: Boolean,
    showExtraKeys: Boolean
) {
    val navController = rememberNavController()
    val density = LocalDensity.current
    val px24 = with(density) { 24.dp.roundToPx() }
    // MàJ 95 — « Réduire les animations » : réglage interne OU échelle système
    // à 0 (Settings.Global.ANIMATOR_DURATION_SCALE), exposé via LocalReduceMotion.
    val reduceAnimations = LocalReduceMotion.current || viewModel.reduceAnimations
    val duration = 250
    // Easing « emphasizedDecelerate » du Material Motion (Android).
    val emphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    // ── Pont ViewModel → NavHost : corr-009, collecte des événements de navigation
    // émis par NavigationViewModel (SharedFlow) — remplace l'ancien LaunchedEffect
    // sur navRouteRequest/navBackRequest qui rejouait l'événement à la
    // recomposition. Chaque événement est exécuté une seule fois.
    LaunchedEffect(viewModel.navigation) {
        viewModel.navigation.events.collect { event ->
            when (event) {
                is NavEvent.Route -> {
                    viewModel.navigation.consumeReplayedEvent()
                    // Déjà sur cette destination (les routes à argument sont
                    // comparées sans leur partie « ?… ») : ne relance pas la
                    // transition.
                    val pattern = event.route.substringBefore('?')
                    val current = navController.currentDestination?.route?.substringBefore('?')
                    if (pattern == current) return@collect
                    navController.navigate(event.route) {
                        launchSingleTop = true
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        restoreState = true
                    }
                }
                is NavEvent.Back -> {
                    viewModel.navigation.consumeReplayedEvent()
                    navController.popBackStack()
                }
            }
        }
    }

    // ── Pont NavHost → ViewModel : resynchronise l'écran courant ────────────
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    LaunchedEffect(currentRoute, backStackEntry?.id) {
        viewModel.syncScreenFromRoute(currentRoute)
    }

    // Les réglages glissent du bas comme une feuille ; le reste glisse latéralement.
    fun isSheet(route: String?): Boolean = route == null || route.startsWith("settings")

    NavHost(
        navController = navController,
        startDestination = ZRoutes.CHAT,
        modifier = Modifier.fillMaxSize(),
        enterTransition = {
            when {
                reduceAnimations -> EnterTransition.None
                isSheet(targetState.destination.route) ->
                    slideInVertically(tween(duration, easing = emphasizedDecelerate)) { it } +
                        fadeIn(tween(duration))
                else ->
                    slideInHorizontally(tween(duration, easing = emphasizedDecelerate)) { px24 } +
                        fadeIn(tween(duration))
            }
        },
        exitTransition = {
            when {
                reduceAnimations -> ExitTransition.None
                isSheet(initialState.destination.route) ->
                    slideOutVertically(tween(duration, easing = emphasizedDecelerate)) { it } +
                        fadeOut(tween(duration))
                else ->
                    slideOutHorizontally(tween(duration, easing = emphasizedDecelerate)) { -px24 } +
                        fadeOut(tween(duration))
            }
        },
        popEnterTransition = {
            when {
                reduceAnimations -> EnterTransition.None
                isSheet(targetState.destination.route) ->
                    slideInVertically(tween(duration, easing = emphasizedDecelerate)) { it } +
                        fadeIn(tween(duration))
                else ->
                    slideInHorizontally(tween(duration, easing = emphasizedDecelerate)) { -px24 } +
                        fadeIn(tween(duration))
            }
        },
        popExitTransition = {
            when {
                reduceAnimations -> ExitTransition.None
                isSheet(initialState.destination.route) ->
                    slideOutVertically(tween(duration, easing = emphasizedDecelerate)) { it } +
                        fadeOut(tween(duration))
                else ->
                    slideOutHorizontally(tween(duration, easing = emphasizedDecelerate)) { px24 } +
                        fadeOut(tween(duration))
            }
        }
    ) {
        composable(
            route = ZRoutes.CHAT,
            deepLinks = listOf(navDeepLink { uriPattern = "${ZRoutes.DEEP_LINK_SCHEME}://chat" })
        ) {
            AdaptiveChatPane(viewModel = viewModel, widthSizeClass = widthSizeClass)
        }

        composable(
            route = ZRoutes.FILES,
            deepLinks = listOf(navDeepLink { uriPattern = "${ZRoutes.DEEP_LINK_SCHEME}://files" })
        ) {
            FileBrowserScreen(viewModel = viewModel, onBack = { viewModel.navigateBack() })
        }

        composable(
            route = ZRoutes.EDITOR,
            arguments = listOf(
                navArgument("path") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            deepLinks = listOf(navDeepLink { uriPattern = "${ZRoutes.DEEP_LINK_SCHEME}://editor?path={path}" })
        ) { entry ->
            val path = entry.arguments?.getString("path")
            // Deep link ou restauration : ouvre le fichier demandé si l'éditeur
            // n'a encore aucun onglet (sinon on conserve l'état existant).
            // corr-039 : le chemin d'un deep link doit rester un chemin RELATIF
            // du projet — toute traversée (.., séparateur absolu, encodage) est
            // ignorée ; la lecture reste de toute façon confinée par
            // FileRepository.
            LaunchedEffect(path) {
                val valide = !path.isNullOrBlank() &&
                    !path.contains("..") &&
                    !path.startsWith("/") &&
                    !path.contains('\\') &&
                    !path.contains('\u0000')
                if (valide && viewModel.editor.tabs.isEmpty()) {
                    viewModel.openFilePath(path)
                }
            }
            AdaptiveEditorPane(
                viewModel = viewModel,
                widthSizeClass = widthSizeClass,
                landscape = landscape
            )
        }

        composable(
            route = ZRoutes.TERMINAL,
            deepLinks = listOf(navDeepLink { uriPattern = "${ZRoutes.DEEP_LINK_SCHEME}://terminal" })
        ) {
            TerminalScreen(
                controller = viewModel.terminal,
                showExtraKeys = showExtraKeys,
                onBack = { viewModel.navigateBack() },
                onOpenPath = { path, line -> viewModel.openFilePath(path, line) } // feat-056
            )
        }

        composable(
            route = ZRoutes.SETTINGS,
            deepLinks = listOf(navDeepLink { uriPattern = "${ZRoutes.DEEP_LINK_SCHEME}://settings" })
        ) {
            SettingsScreen(viewModel = viewModel, onDismiss = { viewModel.navigateBack() })
        }

        // MA71 : route dédiée à la sous-vue Apparence (deep link zagent://settings/appearance).
        composable(
            route = ZRoutes.SETTINGS_APPEARANCE,
            deepLinks = listOf(navDeepLink { uriPattern = "${ZRoutes.DEEP_LINK_SCHEME}://settings/appearance" })
        ) {
            SettingsScreen(
                viewModel = viewModel,
                onDismiss = { viewModel.navigateBack() },
                openAppearance = true
            )
        }
    }
}
