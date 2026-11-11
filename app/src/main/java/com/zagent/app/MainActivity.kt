package com.zagent.app

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Alignment
import androidx.compose.runtime.CompositionLocalProvider
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.zagent.app.ui.components.common.AmbientBackground
import com.zagent.app.ui.components.common.FloatingTabBar
import com.zagent.app.ui.navigation.ZNavHost
import com.zagent.app.ui.theme.LocalGlass
import com.zagent.app.ui.theme.LocalHazeState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.Duration
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.zagent.app.agent.AgentNotifier
import com.zagent.app.ui.screens.HistoryDrawer
import com.zagent.app.ui.screens.OnboardingScreen
import com.zagent.app.ui.screens.ProjectsScreen
import com.zagent.app.ui.screens.SearchScreen
import com.zagent.app.ui.screens.ShareSheet
import com.zagent.app.ui.screens.EntreeApplication
import com.zagent.app.ui.screens.TemplatesScreen
import com.zagent.app.ui.screens.GitScreen
import com.zagent.app.ui.screens.TerminalScreen
import com.zagent.app.ui.theme.ZAgentTheme
import com.zagent.app.ui.theme.echelleAnimationsSysteme
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.viewmodel.AppScreen
import com.zagent.app.ui.viewmodel.MainViewModel
import com.zagent.app.update.UpdateHost

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onStart() {
        super.onStart()
        AgentNotifier.appInForeground = true
    }

    // err-082 — mémoire basse : prévient le ViewModel pour qu'il allège son état.
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            viewModel.noterMemoireBasse()
        }
    }

    override fun onStop() {
        AgentNotifier.appInForeground = false
        super.onStop()
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // MàJ 93 — splash système : DOIT être appelé avant super.onCreate().
        // Le thème de démarrage (Theme.ZAgent.Starting) affiche le logo sur le
        // fond du thème, sans flash blanc en mode sombre.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // MàJ 100 — langue par application : resynchronise appcompat avec le
        // réglage persistant (« Système » = liste vide). Avec le service
        // AppLocalesMetadataHolderService (autoStoreLocales), appcompat restaure
        // aussi la langue tout seul au démarrage ; cette synchronisation couvre
        // les migrations et les cas de désynchronisation.
        run {
            val langue = viewModel.settingsManager.langueUi
            val cible = if (langue.isBlank()) LocaleListCompat.getEmptyLocaleList()
            else LocaleListCompat.forLanguageTags(langue)
            if (AppCompatDelegate.getApplicationLocales() != cible) {
                AppCompatDelegate.setApplicationLocales(cible)
            }
        }
        AgentNotifier.ensureChannels(this)
        // MA77 : raccourcis d'application (appui long sur l'icône) — démarrage à froid.
        handleShortcutIntent(intent)
        // corr-097 : tap sur une notification → rouvre la conversation visée.
        restoreConversationFromIntent(intent)
        // feat-094 : « Partager vers ZAgent » (démarrage à froid).
        handleShareIntent(intent)
        setContent {
            val systemDark = isSystemInDarkTheme()
            var scheduleTick by remember { mutableIntStateOf(0) }
            LaunchedEffect(viewModel.themeMode, viewModel.scheduledDarkTime, viewModel.scheduledLightTime) {
                if (viewModel.themeMode == com.zagent.app.ui.theme.ThemeMode.SCHEDULED) {
                    while (true) {
                        delay(millisUntilNextScheduledChange(viewModel.scheduledDarkTime, viewModel.scheduledLightTime))
                        scheduleTick++
                    }
                }
            }
            val settings = viewModel.settingsManager
            val dark = when (viewModel.themeMode) {
                com.zagent.app.ui.theme.ThemeMode.SYSTEM -> systemDark
                com.zagent.app.ui.theme.ThemeMode.LIGHT -> false
                com.zagent.app.ui.theme.ThemeMode.DARK -> true
                com.zagent.app.ui.theme.ThemeMode.SCHEDULED -> {
                    scheduleTick
                    isWithinScheduledDarkPeriod(viewModel.scheduledDarkTime, viewModel.scheduledLightTime)
                }
            }
            // MA75 : classes de taille de fenêtre pour tablettes et pliables.
            val windowSizeClass = calculateWindowSizeClass(this@MainActivity)
            // MàJ 95 — réduction des animations : réglage interne OU échelle système à 0.
            val reduceMotion = viewModel.reduceAnimations ||
                echelleAnimationsSysteme(this@MainActivity) == 0f
            ZAgentTheme(
                darkTheme = dark,
                stylePreset = if (settings.stylePresetId == com.zagent.app.ui.theme.StylePresetIds.CUSTOM) settings.styleBasePresetId else settings.stylePresetId,
                oledBlack = viewModel.oledBlack,
                dynamicColors = viewModel.dynamicSystemColors,
                cornerRadius = viewModel.cornerRadius,
                fontFamilyId = viewModel.fontFamilyId,
                fontScale = viewModel.fontScale,
                densityScale = viewModel.densityScale,
                reduceMotion = reduceMotion,
                hapticLevel = viewModel.hapticLevel
            ) {
                // MàJ 93 — fondu de 400 ms du splash système vers l'application.
                EntreeApplication(reduceMotion = reduceMotion) {
                    SurfaceBackground()
                    AppRoot(viewModel, dark, windowSizeClass.widthSizeClass)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // MA77 : raccourcis pendant que l'activité est déjà au premier plan (singleTop).
        handleShortcutIntent(intent)
        // corr-097 : notification reçue pendant que l'appli tourne déjà.
        restoreConversationFromIntent(intent)
        // feat-094 : partage reçu pendant que l'appli tourne déjà.
        handleShareIntent(intent)
    }

    /**
     * feat-094 — « Partager vers ZAgent » : le texte partagé par une autre
     * application devient un brouillon dans une NOUVELLE conversation ; à
     * l'utilisateur d'appuyer sur envoyer (aucun envoi automatique).
     */
    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        if (intent.type != "text/plain") return
        val texte = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        if (texte.isBlank()) return
        viewModel.recevoirTextePartage(texte)
    }

    /** corr-097 : rouvre la conversation pointée par une notification. */
    private fun restoreConversationFromIntent(intent: Intent?) {
        val convId = intent?.getStringExtra(com.zagent.app.agent.AgentNotifier.EXTRA_CONVERSATION) ?: return
        if (convId.isNotBlank() && convId != viewModel.conversationId) {
            viewModel.switchToConversation(convId)
        }
    }

    /** MA77 : applique l'action d'un raccourci d'application, si reconnue. */
    private fun handleShortcutIntent(intent: Intent?) {
        val action = intent?.action ?: return
        if (action == MainViewModel.ACTION_SHORTCUT_NEW_CHAT ||
            action == MainViewModel.ACTION_SHORTCUT_OPEN_TERMINAL ||
            action == MainViewModel.ACTION_SHORTCUT_LAST_PROJECT ||
            action == MainViewModel.ACTION_SHORTCUT_OPEN_GIT ||
            action == MainViewModel.ACTION_SHORTCUT_OPEN_MODELS
        ) {
            viewModel.handleAppShortcut(action)
            return
        }
        // maj-040 — deep links zagent://git et zagent://models : mêmes cibles que
        // les raccourcis d'application (Git n'est pas une route de navigation mais
        // un état du ViewModel ; Modèles est une feuille accrochée aux Réglages).
        // Le manifeste déclare déjà le schéma zagent en VIEW/BROWSABLE.
        if (action == Intent.ACTION_VIEW) {
            when (intent.dataString) {
                "zagent://git" -> viewModel.handleAppShortcut(MainViewModel.ACTION_SHORTCUT_OPEN_GIT)
                "zagent://models" -> viewModel.handleAppShortcut(MainViewModel.ACTION_SHORTCUT_OPEN_MODELS)
            }
        }
    }
}

@Composable
private fun SurfaceBackground() {
    val c = Zcolors
    Box(
        Modifier
            .fillMaxSize()
            .background(c.bg)
    )
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
private fun AppRoot(
    viewModel: MainViewModel,
    darkTheme: Boolean,
    widthSizeClass: WindowWidthSizeClass
) {
    val hazeState = remember { HazeState() }
    var navVisible by remember { mutableStateOf(true) }
    // corr-087 : un changement d'écran réaffiche toujours la barre — sinon une
    // navigation depuis un fil déroulé laissait la barre masquée sur un écran
    // sans défilement (terminal, réglages), sans moyen de la faire revenir.
    LaunchedEffect(viewModel.currentScreen) { navVisible = true }
    val navScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -4f) navVisible = false
                else if (available.y > 4f) navVisible = true
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(darkTheme) { viewModel.reloadGlassSettings(darkTheme) }

    if (!viewModel.onboardingDone) {
        OnboardingScreen(
            currentModel = viewModel.modelInput,
            onModelSelected = { m -> viewModel.saveSettings(viewModel.apiKeyInput, m, viewModel.baseUrlInput, viewModel.autoModeInput) },
            onFolderPicked = { uri -> viewModel.importFolder(uri) },
            // MàJ 101 — la clé de l'onboarding alimente le bon fournisseur
            // (détection z.ai / NVIDIA), et le modèle de départ suit.
            onApiKeySaved = { key, url ->
                val id = viewModel.ajouterFournisseur("", url, key)
                val premier = com.zagent.app.data.ModelCatalog.all
                    .firstOrNull { it.fournisseurId == id && !it.specialized }
                if (premier != null) viewModel.selectGlobalModel(premier.id)
            },
            initialApiKey = viewModel.apiKeyInput,
            onFinished = { viewModel.completeOnboarding() },
            initialThemeMode = viewModel.themeMode,
            onThemeSelected = { mode -> viewModel.updateThemeMode(mode) },
            onGlassTrial = { enabled -> viewModel.setGlassTrialEnabled(enabled) },
            testerCle = { url, cle -> viewModel.testerCleOnboarding(url, cle) },
            creerExemple = { viewModel.creerProjetExemple() }
        )
        return
    }

    val context = LocalContext.current
    var lastBackPress by remember { mutableStateOf(0L) }
    val configuration = LocalConfiguration.current
    val landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isRoot = viewModel.currentScreen == AppScreen.CHAT

    // MA74 — Geste retour prédictif : actif sur l'écran racine uniquement, où la
    // double pression pour quitter est conservée. Sur les autres écrans, le back
    // stack du NavHost (MA71) est dépilé par le système, puis MainViewModel est
    // resynchronisé via syncScreenFromRoute. Sur API 26–32, PredictiveBackHandler
    // passe par le dispatcher classique : le comportement de retour est identique.
    // L'animation complète du geste nécessite Android 13+ et targetSdk ≥ 33
    // (voir la note de migration targetSdk 34 dans le CHANGELOG du LOT 6).
    PredictiveBackHandler(enabled = isRoot) { progress ->
        try {
            progress.collect { }
            // Geste terminé : logique de double pression conservée.
            val now = System.currentTimeMillis()
            if (now - lastBackPress < 2000) {
                (context as? android.app.Activity)?.finish()
            } else {
                lastBackPress = now
                com.zagent.app.ui.components.common.ZEphemeral.show("Encore un appui pour quitter")
            }
        } catch (e: CancellationException) {
            // Geste annulé par l'utilisateur : aucune action.
        }
    }

    CompositionLocalProvider(LocalGlass provides viewModel.glassSettings, LocalHazeState provides hazeState) {
        Box(
            Modifier
                .fillMaxSize()
                // ux-094 — clavier physique : Ctrl+K ouvre la recherche globale
                // depuis n'importe quel écran.
                .onPreviewKeyEvent { ev ->
                    if (ev.type == KeyEventType.KeyDown && ev.isCtrlPressed && ev.key == Key.K) {
                        viewModel.isSearchOpen = true
                        true
                    } else false
                }
                .nestedScroll(navScrollConnection)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        var totalY = 0f
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            totalY += change.positionChange().y
                            if (totalY < -24f) navVisible = false
                            else if (totalY > 24f) navVisible = true
                        } while (event.changes.any { it.pressed })
                    }
                }
                .hazeSource(state = hazeState) // MàJ 107 : API Haze 2 (ancien Modifier.haze supprimé)
        ) {
        // Fond animé réel de l'application, placé derrière les écrans et les surfaces Haze.
        AmbientBackground(Modifier.fillMaxSize())

        // MA71 : hôte de navigation unique (routes typées, back stack, deep links).
        ZNavHost(
            viewModel = viewModel,
            widthSizeClass = widthSizeClass,
            landscape = landscape,
            showExtraKeys = !navVisible
        )

        UpdateHost()

        // ui-091 — messages éphémères stylés avec le thème (une seule instance).
        com.zagent.app.ui.components.common.ZEphemeralHost(
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // ── Overlays du LOT 5 (MA56 → MA70), feuilles au-dessus du NavHost ───
        if (viewModel.isProjectPickerOpen) {
            ProjectsScreen(viewModel, onClose = { viewModel.isProjectPickerOpen = false })
        }
        if (viewModel.isHistoryOpen) {
            HistoryDrawer(viewModel, onClose = { viewModel.isHistoryOpen = false })
        }
        if (viewModel.isSearchOpen) {
            SearchScreen(viewModel, onClose = { viewModel.isSearchOpen = false })
        }
        if (viewModel.isGitScreenOpen) {
            GitScreen(viewModel, onClose = { viewModel.isGitScreenOpen = false })
        }
        if (viewModel.isTemplatesOpen) {
            TemplatesScreen(viewModel, onClose = { viewModel.isTemplatesOpen = false })
        }
        if (viewModel.isPreviewScreenOpen) {
            com.zagent.app.ui.editor.PreviewScreen(viewModel, onClose = { viewModel.isPreviewScreenOpen = false })
        }
        if (viewModel.isImagePreviewOpen) {
            // feat-070 : visionneuse d'images intégrée (png, jpg, webp, gif).
            com.zagent.app.ui.editor.ImagePreviewScreen(viewModel, onClose = { viewModel.closeImagePreview() })
        }
        if (viewModel.isShareSheetOpen) {
            ShareSheet(viewModel, onClose = { viewModel.isShareSheetOpen = false })
        }

        // MA73 : la barre d'onglets gère elle-même son glissement de masquage.
        FloatingTabBar(
            currentScreen = viewModel.currentScreen,
            onNavigate = { viewModel.navigateTo(it) },
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = navVisible && viewModel.currentScreen != AppScreen.SETTINGS && !viewModel.isLot5OverlayOpen,
            chatBadge = viewModel.pendingConfirmationCount,
            onNewChat = { viewModel.clearConversation() },
            onChangeProject = { viewModel.isProjectPickerOpen = true },
            onNewTerminal = { viewModel.terminal.addTab() }
        )
        }
    }
}


private fun millisUntilNextScheduledChange(darkStart: String, lightStart: String): Long {
    fun parse(value: String, fallback: LocalTime): LocalTime =
        runCatching { LocalTime.parse(value) }.getOrDefault(fallback)
    val dark = parse(darkStart, LocalTime.of(19, 0))
    val light = parse(lightStart, LocalTime.of(7, 0))
    if (dark == light) return Duration.ofHours(24).toMillis()

    val now = java.time.LocalDateTime.now()
    val darkNow = isWithinScheduledDarkPeriod(darkStart, lightStart)
    val target = if (darkNow) light else dark
    var next = now.toLocalDate().atTime(target)
    if (!next.isAfter(now)) next = next.plusDays(1)
    return Duration.between(now, next).toMillis().coerceAtLeast(1_000L)
}

private fun isWithinScheduledDarkPeriod(darkStart: String, lightStart: String): Boolean {
    fun parse(value: String, fallback: LocalTime): LocalTime =
        runCatching { LocalTime.parse(value) }.getOrDefault(fallback)
    val now = LocalTime.now()
    val dark = parse(darkStart, LocalTime.of(19, 0))
    val light = parse(lightStart, LocalTime.of(7, 0))
    return if (dark == light) false
    else if (dark.isAfter(light)) now >= dark || now < light
    else now >= dark && now < light
}
