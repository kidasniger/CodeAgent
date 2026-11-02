package com.zagent.app

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.align
import androidx.compose.ui.Alignment
import androidx.compose.runtime.CompositionLocalProvider
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.zagent.app.ui.components.FloatingTabBar
import com.zagent.app.ui.theme.LocalGlass
import com.zagent.app.ui.theme.LocalHazeState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.Duration
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.zagent.app.agent.AgentNotifier
import com.zagent.app.ui.screens.ChatScreen
import com.zagent.app.ui.screens.EditorScreen
import com.zagent.app.ui.screens.FileBrowserScreen
import com.zagent.app.ui.screens.OnboardingScreen
import com.zagent.app.ui.screens.SettingsScreen
import com.zagent.app.ui.screens.SplashScreen
import com.zagent.app.ui.screens.TerminalScreen
import com.zagent.app.ui.theme.ZAgentTheme
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.theme.resolveDarkTheme
import com.zagent.app.ui.viewmodel.MainViewModel
import com.zagent.app.update.UpdateHost

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onStart() {
        super.onStart()
        AgentNotifier.appInForeground = true
    }

    override fun onStop() {
        AgentNotifier.appInForeground = false
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AgentNotifier.ensureChannels(this)
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
            ZAgentTheme(
                darkTheme = dark,
                stylePreset = if (settings.stylePresetId == com.zagent.app.ui.theme.StylePresetIds.CUSTOM) settings.styleBasePresetId else settings.stylePresetId,
                oledBlack = viewModel.oledBlack,
                dynamicColors = viewModel.dynamicSystemColors,
                cornerRadius = viewModel.cornerRadius,
                fontFamilyId = viewModel.fontFamilyId,
                fontScale = viewModel.fontScale,
                densityScale = viewModel.densityScale
            ) {
                SurfaceBackground()
                AppRoot(viewModel, systemDark)
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

@Composable
private fun AppRoot(viewModel: MainViewModel, systemDark: Boolean) {
    val hazeState = remember { HazeState() }
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen(onDone = { showSplash = false })
        return
    }

    if (!viewModel.onboardingDone) {
        OnboardingScreen(
            currentModel = viewModel.modelInput,
            onModelSelected = { m -> viewModel.saveSettings(viewModel.apiKeyInput, m, viewModel.baseUrlInput, viewModel.autoModeInput) },
            onFolderPicked = { },
            onApiKeySaved = { key -> viewModel.saveSettings(key, viewModel.modelInput, viewModel.baseUrlInput, viewModel.autoModeInput) },
            initialApiKey = viewModel.apiKeyInput,
            onFinished = { viewModel.completeOnboarding() }
        )
        return
    }

    val context = LocalContext.current
    var lastBackPress by remember { mutableStateOf(0L) }
    val canGoBack = viewModel.currentScreen != com.zagent.app.ui.viewmodel.AppScreen.CHAT
    BackHandler(enabled = canGoBack) {
        viewModel.navigateBack()
    }
    BackHandler(enabled = !canGoBack) {
        val now = System.currentTimeMillis()
        if (now - lastBackPress < 2000) {
            (context as? android.app.Activity)?.finish()
        } else {
            lastBackPress = now
            android.widget.Toast.makeText(context, "Appuyez encore pour quitter", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    CompositionLocalProvider(LocalGlass provides viewModel.glassSettings, LocalHazeState provides hazeState) {
        Box(Modifier.fillMaxSize().haze(state = hazeState)) {
        val underlyingScreen = if (viewModel.currentScreen == com.zagent.app.ui.viewmodel.AppScreen.SETTINGS) {
            viewModel.navigationPreviousScreen()
        } else {
            viewModel.currentScreen
        }

        when (underlyingScreen) {
            com.zagent.app.ui.viewmodel.AppScreen.TERMINAL -> TerminalScreen(
                controller = viewModel.terminal,
                onBack = { viewModel.navigateBack() }
            )
            com.zagent.app.ui.viewmodel.AppScreen.EDITOR -> EditorScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateBack() }
            )
            com.zagent.app.ui.viewmodel.AppScreen.FILE_BROWSER -> FileBrowserScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateBack() }
            )
            else -> ChatScreen(
                viewModel = viewModel,
                onOpenSettings = { viewModel.navigateTo(com.zagent.app.ui.viewmodel.AppScreen.SETTINGS) }
            )
        }

        if (viewModel.currentScreen == com.zagent.app.ui.viewmodel.AppScreen.SETTINGS) {
            SettingsScreenCompat(viewModel)
        }

        UpdateHost()

        if (viewModel.isProjectPickerOpen) {
            com.zagent.app.ui.screens.ProjectPickerDialog(viewModel)
        }
        if (viewModel.currentScreen != com.zagent.app.ui.viewmodel.AppScreen.SETTINGS && !viewModel.isProjectPickerOpen) {
            FloatingTabBar(
                currentScreen = viewModel.currentScreen,
                onNavigate = { viewModel.navigateTo(it) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        }
    }
}

@Composable
private fun SettingsScreenCompat(viewModel: MainViewModel) {
    SettingsScreen(
        viewModel = viewModel,
        onDismiss = { viewModel.navigateBack() }
    )
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
